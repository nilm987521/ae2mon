package cc.nilm.mcmod.ae2mon.client.screen;

import cc.nilm.mcmod.ae2mon.common.menu.PokemonTerminalMenu;
import cc.nilm.mcmod.ae2mon.common.network.DepositPokemonPayload;
import cc.nilm.mcmod.ae2mon.common.network.SyncPokemonListPayload;
import cc.nilm.mcmod.ae2mon.common.network.WithdrawPokemonPayload;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.client.gui.PokemonGuiUtilsKt;
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState;
import com.cobblemon.mod.common.entity.PoseType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Quaternionf;

import java.util.*;

public class PokemonTerminalScreen extends AbstractContainerScreen<PokemonTerminalMenu> {

    // Layout constants (1.3x scaled from original 430x242)
    private static final int SCREEN_WIDTH     = 559;
    private static final int SCREEN_HEIGHT    = 315;

    // Three equal panels: left=7, gap=7, each 177×288, top=23
    private static final int PANEL_W          = 177;
    private static final int PANEL_H          = 288;
    private static final int PANEL_Y          = 23;

    // Network list panel
    private static final int LIST_X           = 10;
    private static final int LIST_Y           = 29;
    private static final int LIST_WIDTH       = 170;
    private static final int ENTRY_HEIGHT     = 23;
    private static final int VISIBLE_ENTRIES  = 12;

    // Detail panel (center)
    private static final int DETAIL_X         = 191;  // 7 + 177 + 7
    private static final int DETAIL_Y         = PANEL_Y;
    private static final int DETAIL_W         = PANEL_W;
    private static final int DETAIL_H         = PANEL_H;

    // Party panel (right)
    private static final int PARTY_X          = 375;  // 7 + 177 + 7 + 177 + 7
    private static final int PARTY_Y          = PANEL_Y;
    private static final int PARTY_ENTRY_H    = 44;

    // Colors
    private static final int COL_BG           = 0xFF1A1A2A;
    private static final int COL_PANEL        = 0xFF252535;
    private static final int COL_PANEL_DARK   = 0xFF1E1E2E;
    private static final int COL_BORDER       = 0xFF4A4A6E;
    private static final int COL_SELECTED     = 0x66A0A0FF;
    private static final int COL_HOVER        = 0x33FFFFFF;
    private static final int COL_TEXT         = 0xFFDDDDEE;
    private static final int COL_MUTED        = 0xFF7788AA;
    private static final int COL_GOLD         = 0xFFFFCC44;
    private static final int COL_TITLE        = 0xFFAADDFF;
    private static final int COL_LABEL        = 0xFF8899CC;
    private static final int COL_MALE         = 0xFF66AAFF;
    private static final int COL_FEMALE       = 0xFFFF88AA;
    private static final int COL_GENDERLESS   = 0xFF999999;
    private static final int COL_IV_MAX       = 0xFF55FF44;
    private static final int COL_IV_HIGH      = 0xFFAAFF88;
    private static final int COL_IV_MID       = 0xFFDDDDDD;
    private static final int COL_IV_ZERO      = 0xFFFF5544;
    private static final int COL_EV_MAX       = 0xFFFFCC44;
    private static final int COL_EV_MID       = 0xFFDDCC88;
    private static final int COL_EV_ZERO      = 0xFF666677;

    // NOTE: drawProfilePokemon calls rotation.conjugate() in-place, so we must
    //       create a fresh Quaternionf on every call — never share a static instance.
    private static Quaternionf freshProfileRotation() {
        return new Quaternionf().rotationXYZ((float) Math.toRadians(13), (float) Math.toRadians(35), 0F);
    }

    private List<SyncPokemonListPayload.PokemonEntry> pokemonList = new ArrayList<>();
    private List<SyncPokemonListPayload.PartyEntry> partyList = new ArrayList<>();
    private int selectedNetworkIndex = -1;
    private int scrollOffset = 0;
    private boolean powered = true;

    // Held-item icon position for hover tooltip
    private ItemStack detailHeldItemStack = ItemStack.EMPTY;
    private int detailHeldItemScreenX = -1;
    private int detailHeldItemScreenY = -1;

    // FloatingState cache: per-UUID for network Pokemon, per-slot for party
    private final Map<UUID, FloatingState> networkStates = new LinkedHashMap<>();
    private final FloatingState[] partyStates = new FloatingState[6];

    public PokemonTerminalScreen(PokemonTerminalMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = SCREEN_WIDTH;
        this.imageHeight = SCREEN_HEIGHT;
        for (int i = 0; i < 6; i++) {
            partyStates[i] = new FloatingState();
        }
    }

    public void updatePokemonList(List<SyncPokemonListPayload.PokemonEntry> entries,
                                  List<SyncPokemonListPayload.PartyEntry> party, boolean powered) {
        this.pokemonList = new ArrayList<>(entries);
        this.partyList = new ArrayList<>(party);
        this.powered = powered;
        if (selectedNetworkIndex >= pokemonList.size()) {
            selectedNetworkIndex = -1;
        }
        // Remove states for Pokemon that are no longer in the network
        Set<UUID> activeUUIDs = new HashSet<>();
        for (var e : entries) activeUUIDs.add(e.uuid());
        networkStates.keySet().retainAll(activeUUIDs);
    }

    @Override
    protected void init() {
        super.init();

        addRenderableWidget(Button.builder(
                Component.literal("Withdraw"),
                btn -> onWithdraw()
        ).bounds(leftPos + DETAIL_X + 5, topPos + DETAIL_Y + DETAIL_H - 26, 104, 21).build());

        for (int i = 0; i < 6; i++) {
            final int slot = i;
            addRenderableWidget(Button.builder(
                    Component.literal("Deposit"),
                    btn -> onDeposit(slot)
            ).bounds(leftPos + PARTY_X + 28, topPos + PARTY_Y + 21 + i * PARTY_ENTRY_H + 27, 78, 16).build());
        }

        addRenderableWidget(Button.builder(Component.literal("▲"), btn -> scroll(-1))
                .bounds(leftPos + LIST_X + LIST_WIDTH - 20, topPos + LIST_Y, 18, 18).build());
        addRenderableWidget(Button.builder(Component.literal("▼"), btn -> scroll(1))
                .bounds(leftPos + LIST_X + LIST_WIDTH - 20, topPos + LIST_Y + VISIBLE_ENTRIES * ENTRY_HEIGHT - 18, 18, 18).build());
    }

    private void scroll(int delta) {
        int maxScroll = Math.max(0, pokemonList.size() - VISIBLE_ENTRIES);
        scrollOffset = Math.max(0, Math.min(scrollOffset + delta, maxScroll));
    }

    private void onWithdraw() {
        if (selectedNetworkIndex < 0 || selectedNetworkIndex >= pokemonList.size()) return;
        UUID uuid = pokemonList.get(selectedNetworkIndex).uuid();
        PacketDistributor.sendToServer(new WithdrawPokemonPayload(menu.containerId, uuid));
    }

    private void onDeposit(int slot) {
        PacketDistributor.sendToServer(new DepositPokemonPayload(menu.containerId, slot));
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // suppress default labels
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        graphics.fill(x, y, x + SCREEN_WIDTH, y + SCREEN_HEIGHT, COL_BG);
        drawBorderRect(graphics, x, y, SCREEN_WIDTH, SCREEN_HEIGHT, COL_BORDER);
        graphics.fill(x + 1, y + 22, x + SCREEN_WIDTH - 1, y + 23, COL_BORDER);

        drawPanel(graphics, x + 7,         y + PANEL_Y, PANEL_W, PANEL_H);
        drawPanel(graphics, x + DETAIL_X, y + PANEL_Y, PANEL_W, PANEL_H);
        drawPanel(graphics, x + PARTY_X,  y + PANEL_Y, PANEL_W, PANEL_H);

    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);

        int x = leftPos, y = topPos;

        // ── Step 1: Update FloatingStates (ONCE per state per frame) ──────────
        Set<UUID> updatedUUIDs = new HashSet<>();
        int visEnd = Math.min(scrollOffset + VISIBLE_ENTRIES, pokemonList.size());
        for (int i = scrollOffset; i < visEnd; i++) {
            UUID uuid = pokemonList.get(i).uuid();
            if (updatedUUIDs.add(uuid)) {
                networkStates.computeIfAbsent(uuid, k -> new FloatingState())
                        .updatePartialTicks(partialTick);
            }
        }
        // Also update selected Pokemon state if it's not in the visible range
        if (selectedNetworkIndex >= 0 && selectedNetworkIndex < pokemonList.size()) {
            UUID sel = pokemonList.get(selectedNetworkIndex).uuid();
            if (updatedUUIDs.add(sel)) {
                networkStates.computeIfAbsent(sel, k -> new FloatingState())
                        .updatePartialTicks(partialTick);
            }
        }
        for (FloatingState s : partyStates) s.updatePartialTicks(partialTick);

        // ── Title + power indicator ───────────────────────────────────────────
        graphics.drawString(font, "Pokemon ME Terminal", x + 10, y + 8, COL_TITLE, false);
        int dotColor = powered ? 0xFF44DD44 : 0xFFDD4444;
        graphics.fill(x + SCREEN_WIDTH - 17, y + 7, x + SCREEN_WIDTH - 8, y + 16, dotColor);
        graphics.drawString(font, powered ? "ON" : "OFF", x + SCREEN_WIDTH - 39, y + 8,
                powered ? 0xFF44DD44 : 0xFFDD4444, false);

        if (!powered) {
            int warnX = x + SCREEN_WIDTH / 2 - 85;
            int warnY = y + SCREEN_HEIGHT / 2 - 13;
            graphics.fill(warnX - 5, warnY - 5, warnX + 174, warnY + 29, 0xCC000000);
            graphics.fill(warnX - 4, warnY - 4, warnX + 173, warnY + 27, 0xCC8B0000);
            graphics.drawString(font, "! No Power / Not Connected", warnX, warnY, 0xFFFF4444, false);
            graphics.drawString(font, "Connect to ME network", warnX + 18, warnY + 14, 0xFFAAAAAA, false);
        }

        // ── List panel ────────────────────────────────────────────────────────
        graphics.drawString(font, "Network (" + pokemonList.size() + ")", x + LIST_X, y + 16, COL_LABEL, false);

        for (int i = scrollOffset; i < visEnd; i++) {
            int displayIdx = i - scrollOffset;
            int ey = y + LIST_Y + displayIdx * ENTRY_HEIGHT;
            SyncPokemonListPayload.PokemonEntry entry = pokemonList.get(i);

            boolean selected = (i == selectedNetworkIndex);
            boolean hovered = mouseX > x + LIST_X - 1 && mouseX < x + LIST_X + LIST_WIDTH - 1
                    && mouseY >= ey && mouseY < ey + ENTRY_HEIGHT;
            if (selected) {
                graphics.fill(x + LIST_X - 1, ey, x + LIST_X + LIST_WIDTH - 1, ey + ENTRY_HEIGHT - 1, COL_SELECTED);
            } else if (hovered) {
                graphics.fill(x + LIST_X - 1, ey, x + LIST_X + LIST_WIDTH - 1, ey + ENTRY_HEIGHT - 1, COL_HOVER);
            }

            // Sprite (left side of entry)
            FloatingState state = networkStates.computeIfAbsent(entry.uuid(), k -> new FloatingState());
            renderPokemonSprite(graphics, partialTick, entry.species(), entry.gender(),
                    state, x + LIST_X + 10, ey + ENTRY_HEIGHT / 2 - 5, 5.85F);

            // Text (shifted right of sprite)
            int textY = ey + (ENTRY_HEIGHT - 9) / 2;
            String label = capitalize(entry.species()) + " Lv." + entry.level();
            graphics.drawString(font, label, x + LIST_X + 23, textY, COL_TEXT, false);

            // Gender — shifted left to avoid the scroll buttons
            String gSym = genderSymbol(entry.gender());
            graphics.drawString(font, gSym,
                    x + LIST_X + LIST_WIDTH - 26 - font.width(gSym), textY, genderColor(entry.gender()), false);
        }
        if (pokemonList.isEmpty()) {
            graphics.drawString(font, "(no pokemon)", x + LIST_X + 4, y + LIST_Y + 5, COL_MUTED, false);
        }

        // ── Detail panel ──────────────────────────────────────────────────────
        renderDetailPanel(graphics, x, y, partialTick);

        // ── Party panel ───────────────────────────────────────────────────────
        graphics.drawString(font, "Party", x + PARTY_X + 5, y + PARTY_Y + 7, COL_LABEL, false);
        for (int i = 0; i < 6; i++) {
            int ey = y + PARTY_Y + 21 + i * PARTY_ENTRY_H;
            final int slot = i;
            var entryOpt = partyList.stream().filter(e -> e.slot() == slot).findFirst();
            if (entryOpt.isPresent()) {
                var pe = entryOpt.get();
                // Animated sprite on the left of the entry
                renderPokemonSprite(graphics, partialTick, pe.species(), pe.gender(),
                        partyStates[i], x + PARTY_X + 13, ey + 16, 8.5F);
                // Name + gender (shifted right of sprite)
                String nameLabel = capitalize(pe.species()) + " Lv." + pe.level();
                graphics.drawString(font, nameLabel, x + PARTY_X + 28, ey + 1, COL_TEXT, false);
                int nw = font.width(nameLabel);
                graphics.drawString(font, " " + genderSymbol(pe.gender()),
                        x + PARTY_X + 28 + nw, ey + 1, genderColor(pe.gender()), false);
                graphics.drawString(font, formatNature(pe.nature()),
                        x + PARTY_X + 28, ey + 14, COL_MUTED, false);
            } else {
                graphics.drawString(font, "(empty)", x + PARTY_X + 28, ey + 1, COL_MUTED, false);
            }
        }

        renderTooltip(graphics, mouseX, mouseY);

        // Held item hover tooltip
        if (!detailHeldItemStack.isEmpty()
                && mouseX >= detailHeldItemScreenX && mouseX < detailHeldItemScreenX + 16
                && mouseY >= detailHeldItemScreenY && mouseY < detailHeldItemScreenY + 16) {
            Minecraft mc = Minecraft.getInstance();
            var lines = detailHeldItemStack.getTooltipLines(
                    Item.TooltipContext.of(mc.level), mc.player, TooltipFlag.Default.NORMAL);
            graphics.renderTooltip(font, lines, java.util.Optional.empty(), mouseX, mouseY);
        }
    }

    private void renderDetailPanel(GuiGraphics graphics, int x, int y, float partialTick) {
        if (selectedNetworkIndex < 0 || selectedNetworkIndex >= pokemonList.size()) {
            int midY = y + DETAIL_Y + DETAIL_H / 2 - 9;
            graphics.drawString(font, "Select a Pokemon", x + DETAIL_X + 29, midY, COL_MUTED, false);
            graphics.drawString(font, "from the list", x + DETAIL_X + 39, midY + 14, COL_MUTED, false);
            return;
        }

        SyncPokemonListPayload.PokemonEntry entry = pokemonList.get(selectedNetworkIndex);
        FloatingState state = networkStates.computeIfAbsent(entry.uuid(), k -> new FloatingState());

        // Reset held item hover state each frame
        detailHeldItemStack = ItemStack.EMPTY;
        detailHeldItemScreenX = -1;
        detailHeldItemScreenY = -1;

        // ── Sprite (left column, 0-84px) ──
        renderPokemonSprite(graphics, partialTick, entry.species(), entry.gender(),
                state, x + DETAIL_X + 42, y + DETAIL_Y + 7, 32.5F);

        // ── Text (right column, 88px onwards) ──
        int tx = x + DETAIL_X + 88;
        int ty = y + DETAIL_Y + 8;

        graphics.drawString(font, capitalize(entry.species()), tx, ty, COL_GOLD, false);
        ty += 16;

        // Level + gender
        String levelStr = "Lv." + entry.level() + "  ";
        String genderStr = genderSymbol(entry.gender());
        graphics.drawString(font, levelStr, tx, ty, COL_TEXT, false);
        graphics.drawString(font, genderStr, tx + font.width(levelStr), ty, genderColor(entry.gender()), false);

        // Held item icon — right after gender symbol
        ItemStack heldStack = buildItemStack(entry.heldItem());
        if (!heldStack.isEmpty()) {
            int itemX = tx + font.width(levelStr) + font.width(genderStr) + 4;
            int itemY = ty - 4;
            graphics.renderItem(heldStack, itemX, itemY);
            detailHeldItemStack = heldStack;
            detailHeldItemScreenX = itemX;
            detailHeldItemScreenY = itemY;
        }
        ty += 14;

        graphics.drawString(font, formatNature(entry.nature()), tx, ty, COL_TEXT, false);
        ty += 14;

        String abilityLabel = "Abil: ";
        graphics.drawString(font, abilityLabel, tx, ty, COL_LABEL, false);
        graphics.drawString(font, formatAbility(entry.ability()),
                tx + font.width(abilityLabel), ty, COL_TEXT, false);
        ty += 14;

        // ── Types ─────────────────────────────────────────────────────────────
        int badgeX = tx;
        if (!entry.type1().isEmpty()) {
            badgeX = drawTypeBadge(graphics, entry.type1(), badgeX, ty);
            badgeX += 4;
        }
        if (!entry.type2().isEmpty()) {
            drawTypeBadge(graphics, entry.type2(), badgeX, ty);
        }

        // ── Divider ───────────────────────────────────────────────────────────
        int divY = y + DETAIL_Y + 87;
        graphics.fill(x + DETAIL_X + 5, divY, x + DETAIL_X + DETAIL_W - 5, divY + 1, COL_BORDER);

        // ── IVs ───────────────────────────────────────────────────────────────
        int ivBaseY = divY + 8;
        int ivTotal = entry.ivHp() + entry.ivAtk() + entry.ivDef()
                + entry.ivSpA() + entry.ivSpD() + entry.ivSpe();
        graphics.drawString(font, "IVs  (" + ivTotal + " / 186)",
                x + DETAIL_X + 5, ivBaseY, COL_LABEL, false);

        String[] statNames  = {"HP", "Atk", "Def", "SpA", "SpD", "Spe"};
        int[]    ivValues   = {entry.ivHp(), entry.ivAtk(), entry.ivDef(),
                               entry.ivSpA(), entry.ivSpD(), entry.ivSpe()};
        int[]    colOffsets = {0, 28, 56, 84, 112, 140};

        int labelsY = ivBaseY + 16;
        int valsY   = labelsY + 13;
        for (int col = 0; col < 6; col++) {
            int cx = x + DETAIL_X + 5 + colOffsets[col];
            graphics.drawString(font, statNames[col], cx, labelsY, COL_MUTED, false);
            String ivStr = String.valueOf(ivValues[col]);
            int lw = font.width(statNames[col]);
            int vw = font.width(ivStr);
            graphics.drawString(font, ivStr, cx + Math.max(0, (lw - vw) / 2), valsY, ivColor(ivValues[col]), false);
        }

        // ── EVs ───────────────────────────────────────────────────────────────
        int evBaseY = valsY + 14;
        int evTotal = entry.evHp() + entry.evAtk() + entry.evDef()
                + entry.evSpA() + entry.evSpD() + entry.evSpe();
        graphics.drawString(font, "EVs  (" + evTotal + " / 510)",
                x + DETAIL_X + 5, evBaseY, COL_LABEL, false);

        int[]  evValues  = {entry.evHp(), entry.evAtk(), entry.evDef(),
                            entry.evSpA(), entry.evSpD(), entry.evSpe()};

        int evLabelsY = evBaseY + 16;
        int evValsY   = evLabelsY + 13;
        for (int col = 0; col < 6; col++) {
            int cx = x + DETAIL_X + 5 + colOffsets[col];
            graphics.drawString(font, statNames[col], cx, evLabelsY, COL_MUTED, false);
            String evStr = String.valueOf(evValues[col]);
            int lw = font.width(statNames[col]);
            int vw = font.width(evStr);
            graphics.drawString(font, evStr, cx + Math.max(0, (lw - vw) / 2), evValsY, evColor(evValues[col]), false);
        }
    }

    // ── Sprite rendering ─────────────────────────────────────────────────────

    /**
     * Render an animated Pokemon profile sprite at the given GUI screen center position.
     * @param state  FloatingState — must have updatePartialTicks() called exactly once this frame already.
     */
    private void renderPokemonSprite(GuiGraphics graphics, float partialTick,
                                     String species, String gender,
                                     FloatingState state,
                                     int centerX, int centerY, float scale) {
        ResourceLocation speciesId = ResourceLocation.fromNamespaceAndPath("cobblemon", species.toLowerCase());
        if (PokemonSpecies.INSTANCE.getByIdentifier(speciesId) == null) return;

        Set<String> aspects = new HashSet<>();
        if ("FEMALE".equalsIgnoreCase(gender)) aspects.add("female");
        state.setCurrentAspects(aspects);

        var poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(centerX, centerY, 200.0);

        PokemonGuiUtilsKt.drawProfilePokemon(
                speciesId,
                poseStack,
                freshProfileRotation(),
                PoseType.PROFILE,
                state,
                partialTick,
                scale,
                /*applyProfileTransform=*/ true,
                /*applyBaseScale=*/        false,
                /*doQuirks=*/              false,
                1f, 1f, 1f, 1f,
                0f, 0f
        );

        poseStack.popPose();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static void drawPanel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, COL_PANEL);
        drawBorderRect(g, x, y, w, h, COL_BORDER);
    }

    private static void drawBorderRect(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x,         y,         x + w,     y + 1,     color);
        g.fill(x,         y + h - 1, x + w,     y + h,     color);
        g.fill(x,         y,         x + 1,     y + h,     color);
        g.fill(x + w - 1, y,         x + w,     y + h,     color);
    }

    private static int ivColor(int iv) {
        if (iv == 31) return COL_IV_MAX;
        if (iv == 0)  return COL_IV_ZERO;
        if (iv >= 26) return COL_IV_HIGH;
        return COL_IV_MID;
    }

    private static int evColor(int ev) {
        if (ev == 252) return COL_EV_MAX;
        if (ev == 0)   return COL_EV_ZERO;
        return COL_EV_MID;
    }

    private static String genderSymbol(String gender) {
        if (gender == null) return "-";
        return switch (gender.toUpperCase()) {
            case "MALE"   -> "♂";
            case "FEMALE" -> "♀";
            default       -> "-";
        };
    }

    private static int genderColor(String gender) {
        if (gender == null) return COL_GENDERLESS;
        return switch (gender.toUpperCase()) {
            case "MALE"   -> COL_MALE;
            case "FEMALE" -> COL_FEMALE;
            default       -> COL_GENDERLESS;
        };
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    private static String formatNature(String nature) {
        if (nature == null || nature.isEmpty()) return "???";
        String path = nature.contains(":") ? nature.substring(nature.indexOf(':') + 1) : nature;
        return capitalize(path);
    }

    /** Draws a type badge and returns the X position after the badge. */
    private int drawTypeBadge(GuiGraphics graphics, String type, int x, int y) {
        String label = capitalize(type);
        int w = font.width(label) + 6;
        int color = typeColor(type);
        graphics.fill(x, y - 1, x + w, y + 9, color);
        graphics.drawString(font, label, x + 3, y, 0xFFFFFFFF, false);
        return x + w;
    }

    private static int typeColor(String type) {
        return switch (type.toLowerCase()) {
            case "normal"   -> 0xFF9099A1;
            case "fire"     -> 0xFFFF9741;
            case "water"    -> 0xFF3892DC;
            case "grass"    -> 0xFF38BF4F;
            case "electric" -> 0xFFFBD100;
            case "ice"      -> 0xFF70CEC0;
            case "fighting" -> 0xFFCE4069;
            case "poison"   -> 0xFFB567CE;
            case "ground"   -> 0xFFD97845;
            case "flying"   -> 0xFF89AAE3;
            case "psychic"  -> 0xFFFF6675;
            case "bug"      -> 0xFF91A119;
            case "rock"     -> 0xFFC5B78C;
            case "ghost"    -> 0xFF5269AC;
            case "dark"     -> 0xFF4F4747;
            case "dragon"   -> 0xFF0A6DC4;
            case "steel"    -> 0xFF5A8EA2;
            case "fairy"    -> 0xFFFB89EB;
            default         -> 0xFF666688;
        };
    }

    /** Build an ItemStack from a full registry ID like "cobblemon:oran_berry". */
    private static ItemStack buildItemStack(String itemId) {
        if (itemId == null || itemId.isEmpty()) return ItemStack.EMPTY;
        ResourceLocation loc = ResourceLocation.tryParse(itemId);
        if (loc == null) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.get(loc);
        if (item == net.minecraft.world.item.Items.AIR) return ItemStack.EMPTY;
        return new ItemStack(item);
    }

    private static String formatItemName(String itemPath) {
        if (itemPath == null || itemPath.isEmpty()) return "None";
        String[] parts = itemPath.split("[_\\-]");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (sb.length() > 0) sb.append(" ");
            if (!p.isEmpty()) sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.length() > 0 ? sb.toString() : itemPath;
    }

    private static String formatAbility(String ability) {
        if (ability == null || ability.isEmpty()) return "???";
        String path = ability.contains(":") ? ability.substring(ability.indexOf(':') + 1) : ability;
        String[] parts = path.split("[_\\-]");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) sb.append(" ");
            if (!part.isEmpty()) sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.length() > 0 ? sb.toString() : capitalize(path);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int relX = (int)(mouseX - leftPos - LIST_X);
        int relY = (int)(mouseY - topPos - LIST_Y);
        if (relX >= 0 && relX < LIST_WIDTH && relY >= 0 && relY < VISIBLE_ENTRIES * ENTRY_HEIGHT) {
            int idx = relY / ENTRY_HEIGHT + scrollOffset;
            if (idx >= 0 && idx < pokemonList.size()) {
                selectedNetworkIndex = idx;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll(scrollY < 0 ? 1 : -1);
        return true;
    }
}
