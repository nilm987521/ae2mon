package cc.nilm.mcmod.ae2mon.client.screen;

import cc.nilm.mcmod.ae2mon.common.menu.PokemonTerminalMenu;
import cc.nilm.mcmod.ae2mon.common.network.DepositPokemonPayload;
import cc.nilm.mcmod.ae2mon.common.network.SetHeldItemPayload;
import cc.nilm.mcmod.ae2mon.common.network.SwapPokemonPayload;
import cc.nilm.mcmod.ae2mon.common.network.SyncPokemonListPayload;
import cc.nilm.mcmod.ae2mon.common.network.WithdrawPokemonPayload;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.client.gui.PokemonGuiUtilsKt;
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState;
import com.cobblemon.mod.common.entity.PoseType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
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
import java.util.function.IntPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PokemonTerminalScreen extends AbstractContainerScreen<PokemonTerminalMenu> {

    // Layout constants
    private static final int SCREEN_WIDTH    = 559;
    private static final int SCREEN_HEIGHT   = 320;

    // Three equal panels: left=7, gap=7, each 177×205, top=23
    private static final int PANEL_W         = 177;
    private static final int PANEL_H         = 205;
    private static final int PANEL_Y         = 23;

    // Network list panel
    private static final int LIST_X          = 10;
    private static final int SEARCH_ROW_Y    = 26;  // search widgets top Y
    private static final int LIST_Y          = 46;  // entries start below search row
    private static final int LIST_WIDTH      = 170;
    private static final int ENTRY_HEIGHT    = 23;
    private static final int VISIBLE_ENTRIES = 7;

    // Detail panel (center): 7 + 177 + 7 = 191
    private static final int DETAIL_X        = 191;
    private static final int DETAIL_Y        = PANEL_Y;
    private static final int DETAIL_W        = PANEL_W;
    private static final int DETAIL_H        = PANEL_H;

    // Party panel (right): 7 + 177 + 7 + 177 + 7 = 375
    private static final int PARTY_X         = 375;
    private static final int PARTY_Y         = PANEL_Y;
    private static final int PARTY_ENTRY_H   = 23;

    // Colors
    private static final int COL_BG          = 0xFF1A1A2A;
    private static final int COL_PANEL       = 0xFF252535;
    private static final int COL_BORDER      = 0xFF4A4A6E;
    private static final int COL_SELECTED    = 0x66A0A0FF;
    private static final int COL_HOVER       = 0x33FFFFFF;
    private static final int COL_TEXT        = 0xFFDDDDEE;
    private static final int COL_MUTED       = 0xFF7788AA;
    private static final int COL_GOLD        = 0xFFFFCC44;
    private static final int COL_TITLE       = 0xFFAADDFF;
    private static final int COL_LABEL       = 0xFF8899CC;
    private static final int COL_MALE        = 0xFF66AAFF;
    private static final int COL_FEMALE      = 0xFFFF88AA;
    private static final int COL_GENDERLESS  = 0xFF999999;
    private static final int COL_IV_MAX      = 0xFF55FF44;
    private static final int COL_IV_HIGH     = 0xFFAAFF88;
    private static final int COL_IV_MID      = 0xFFDDDDDD;
    private static final int COL_IV_ZERO     = 0xFFFF5544;
    private static final int COL_EV_MAX      = 0xFFFFCC44;
    private static final int COL_EV_MID      = 0xFFDDCC88;
    private static final int COL_EV_ZERO     = 0xFF666677;
    private static final int COL_DROP_ZONE   = 0x3300FF00;
    private static final int COL_SWAP_HOVER  = 0x55FF8800;

    // IV syntax: "atk=31", "hp=0", "spe>=25", "def<=10", etc.
    private static final Pattern IV_FILTER_PATTERN = Pattern.compile(
            "^(hp|atk|def|spa|spd|spe)(>=|<=|>|<|=)(\\d{1,2})$",
            Pattern.CASE_INSENSITIVE);

    // NOTE: drawProfilePokemon calls rotation.conjugate() in-place, so we must
    //       create a fresh Quaternionf on every call — never share a static instance.
    private static Quaternionf freshProfileRotation() {
        return new Quaternionf().rotationXYZ((float) Math.toRadians(13), (float) Math.toRadians(35), 0F);
    }

    // Data
    private List<SyncPokemonListPayload.PokemonEntry> pokemonList = new ArrayList<>();
    private List<SyncPokemonListPayload.PokemonEntry> displayList = new ArrayList<>();
    private List<SyncPokemonListPayload.PartyEntry>   partyList   = new ArrayList<>();
    private boolean powered = true;

    // Selection (by UUID for network, by slot for party — mutually exclusive)
    private UUID selectedUUID       = null;
    private int  selectedPartySlot  = -1;
    private int  scrollOffset       = 0;

    // Search / filter state (survives screen re-init on resize)
    private String  searchText    = "";
    private boolean filterShiny   = false;
    private int     minPerfectIVs = 0; // 0=any, 1-6=minimum count of 31 IVs
    private EditBox searchBox     = null;

    // Held-item widget state (updated each frame in renderDetailPanel)
    private ItemStack detailHeldItemStack  = ItemStack.EMPTY;
    private int       detailHeldItemScreenX = -1;  // top-left of 16x16 icon
    private int       detailHeldItemScreenY = -1;
    private boolean   heldItemWidgetVisible = false;

    // FloatingState cache: per-UUID for network Pokemon, per-slot for party
    private final Map<UUID, FloatingState> networkStates = new LinkedHashMap<>();
    private final FloatingState[]          partyStates   = new FloatingState[6];

    // Drag-and-drop state
    private enum DragSource { NONE, NETWORK, PARTY }
    private DragSource  dragSource  = DragSource.NONE;
    private boolean     isDragging  = false;
    private int         dragIndex   = -1;
    private double      dragX, dragY;
    private double      dragStartX, dragStartY;
    private String      dragSpecies = null;
    private String      dragGender  = null;
    private final FloatingState dragState = new FloatingState();

    public PokemonTerminalScreen(PokemonTerminalMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth  = SCREEN_WIDTH;
        this.imageHeight = SCREEN_HEIGHT;
        for (int i = 0; i < 6; i++) partyStates[i] = new FloatingState();
    }

    public void updatePokemonList(List<SyncPokemonListPayload.PokemonEntry> entries,
                                  List<SyncPokemonListPayload.PartyEntry> party, boolean powered) {
        this.pokemonList = new ArrayList<>(entries);
        this.partyList   = new ArrayList<>(party);
        this.powered     = powered;
        // Clear selection if the selected Pokemon is no longer in the network
        if (selectedUUID != null && pokemonList.stream().noneMatch(e -> e.uuid().equals(selectedUUID))) {
            selectedUUID = null;
        }
        Set<UUID> activeUUIDs = new HashSet<>();
        for (var e : entries) activeUUIDs.add(e.uuid());
        networkStates.keySet().retainAll(activeUUIDs);
        rebuildDisplayList();
    }

    // ── Filter logic ─────────────────────────────────────────────────────────

    private String ivFilterLabel() {
        return minPerfectIVs == 0 ? "Any" : "≥" + minPerfectIVs + "×31";
    }

    private void rebuildDisplayList() {
        // Split searchText into IV-filter tokens and plain text tokens
        Map<String, IntPredicate> ivFilters = new LinkedHashMap<>();
        List<String> textParts = new ArrayList<>();
        for (String token : searchText.trim().split("\\s+")) {
            if (token.isEmpty()) continue;
            Matcher m = IV_FILTER_PATTERN.matcher(token);
            if (m.matches()) {
                String stat = m.group(1).toLowerCase();
                String op   = m.group(2);
                int    val  = Math.min(31, Integer.parseInt(m.group(3)));
                IntPredicate pred = switch (op) {
                    case "="  -> iv -> iv == val;
                    case ">=" -> iv -> iv >= val;
                    case "<=" -> iv -> iv <= val;
                    case ">"  -> iv -> iv >  val;
                    case "<"  -> iv -> iv <  val;
                    default   -> iv -> true;
                };
                ivFilters.put(stat, pred);
            } else {
                textParts.add(token.toLowerCase());
            }
        }
        String nameQuery = String.join(" ", textParts);

        displayList = pokemonList.stream().filter(e -> {
            // Name / nature text filter
            if (!nameQuery.isEmpty()
                    && !e.species().toLowerCase().contains(nameQuery)
                    && !formatNature(e.nature()).toLowerCase().contains(nameQuery)) return false;
            // Shiny filter
            if (filterShiny && !e.shiny()) return false;
            // Perfect-IV count filter
            if (minPerfectIVs > 0) {
                int c = (e.ivHp() == 31 ? 1 : 0) + (e.ivAtk() == 31 ? 1 : 0) + (e.ivDef() == 31 ? 1 : 0)
                      + (e.ivSpA() == 31 ? 1 : 0) + (e.ivSpD() == 31 ? 1 : 0) + (e.ivSpe() == 31 ? 1 : 0);
                if (c < minPerfectIVs) return false;
            }
            // Per-stat IV filters
            for (var kv : ivFilters.entrySet()) {
                int iv = switch (kv.getKey()) {
                    case "hp"  -> e.ivHp();
                    case "atk" -> e.ivAtk();
                    case "def" -> e.ivDef();
                    case "spa" -> e.ivSpA();
                    case "spd" -> e.ivSpD();
                    case "spe" -> e.ivSpe();
                    default    -> -1;
                };
                if (iv >= 0 && !kv.getValue().test(iv)) return false;
            }
            return true;
        }).toList();

        int maxScroll = Math.max(0, displayList.size() - VISIBLE_ENTRIES);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
    }

    // ── Init ─────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        super.init();

        // Scroll buttons
        addRenderableWidget(Button.builder(Component.literal("▲"), btn -> scroll(-1))
                .bounds(leftPos + LIST_X + LIST_WIDTH - 20, topPos + LIST_Y, 18, 18).build());
        addRenderableWidget(Button.builder(Component.literal("▼"), btn -> scroll(1))
                .bounds(leftPos + LIST_X + LIST_WIDTH - 20, topPos + LIST_Y + VISIBLE_ENTRIES * ENTRY_HEIGHT - 18, 18, 18).build());

        // Search box  (x+10, width=105)
        searchBox = new EditBox(font,
                leftPos + LIST_X, topPos + SEARCH_ROW_Y, 105, 16,
                Component.literal("Search"));
        searchBox.setValue(searchText);
        searchBox.setMaxLength(32);
        searchBox.setHint(Component.literal("Name / Nature / atk=31 hp=0...").withStyle(s -> s.withColor(0x555566)));
        searchBox.setResponder(s -> { searchText = s; scrollOffset = 0; rebuildDisplayList(); });
        addRenderableWidget(searchBox);

        // Shiny toggle  (x+117, width=20)
        addRenderableWidget(Button.builder(
                Component.literal(filterShiny ? "★" : "☆"),
                btn -> {
                    filterShiny = !filterShiny;
                    btn.setMessage(Component.literal(filterShiny ? "★" : "☆"));
                    scrollOffset = 0;
                    rebuildDisplayList();
                }).bounds(leftPos + LIST_X + 107, topPos + SEARCH_ROW_Y, 20, 16).build());

        // IV filter cycle button  (x+129, width=38)
        addRenderableWidget(Button.builder(
                Component.literal(ivFilterLabel()),
                btn -> {
                    minPerfectIVs = (minPerfectIVs + 1) % 7;
                    btn.setMessage(Component.literal(ivFilterLabel()));
                    scrollOffset = 0;
                    rebuildDisplayList();
                }).bounds(leftPos + LIST_X + 129, topPos + SEARCH_ROW_Y, 38, 16).build());
    }

    // ── Scroll ───────────────────────────────────────────────────────────────

    private void scroll(int delta) {
        int maxScroll = Math.max(0, displayList.size() - VISIBLE_ENTRIES);
        scrollOffset = Math.max(0, Math.min(scrollOffset + delta, maxScroll));
    }

    // ── Rendering ────────────────────────────────────────────────────────────

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
        drawPanel(graphics, x + DETAIL_X,  y + PANEL_Y, PANEL_W, PANEL_H);
        drawPanel(graphics, x + PARTY_X,   y + PANEL_Y, PANEL_W, PANEL_H);

        // Player inventory background
        int invBgLeft  = x + PokemonTerminalMenu.INV_X - 4;
        int invBgRight = invBgLeft + 9 * 18 + 7;
        int invMainTop = y + PokemonTerminalMenu.INV_MAIN_Y - 3;
        // Main inventory (3 rows)
        graphics.fill(invBgLeft, invMainTop, invBgRight, invMainTop + 3 * 18 + 6, COL_PANEL);
        drawBorderRect(graphics, invBgLeft, invMainTop, invBgRight - invBgLeft, 3 * 18 + 6, COL_BORDER);
        // Hotbar (1 row, 4px gap below main inv)
        int hotbarTop = y + PokemonTerminalMenu.INV_HOTBAR_Y - 3;
        graphics.fill(invBgLeft, hotbarTop, invBgRight, hotbarTop + 18 + 6, COL_PANEL);
        drawBorderRect(graphics, invBgLeft, hotbarTop, invBgRight - invBgLeft, 18 + 6, COL_BORDER);

        // Thin divider separating search row from list entries
        graphics.fill(x + 8, y + 44, x + 8 + PANEL_W - 2, y + 45, COL_BORDER);

        // Drop zone highlight during drag
        if (isDragging) {
            if (dragSource == DragSource.PARTY && isInNetworkListZone(mouseX, mouseY)) {
                UUID hoveredNetworkUUID = getNetworkEntryUUIDAt(mouseX, mouseY);
                if (hoveredNetworkUUID != null) {
                    // Highlight the specific entry that will be swapped
                    int relY = (int)(mouseY - topPos - LIST_Y);
                    int displayIdx = relY / ENTRY_HEIGHT;
                    int ey = y + LIST_Y + displayIdx * ENTRY_HEIGHT;
                    graphics.fill(x + LIST_X - 1, ey, x + LIST_X + LIST_WIDTH - 21, ey + ENTRY_HEIGHT, COL_SWAP_HOVER);
                } else {
                    // Hovering over empty list area — normal drop zone
                    graphics.fill(x + LIST_X - 1, y + LIST_Y,
                            x + LIST_X + LIST_WIDTH - 21, y + LIST_Y + VISIBLE_ENTRIES * ENTRY_HEIGHT,
                            COL_DROP_ZONE);
                }
            } else if (dragSource == DragSource.NETWORK && isInPartyZone(mouseX, mouseY)) {
                int hoveredSlot = getPartySlotAt(mouseY);
                boolean slotOccupied = hoveredSlot >= 0 && partyList.stream().anyMatch(e -> e.slot() == hoveredSlot);
                if (slotOccupied) {
                    // Highlight the specific party slot that will be swapped
                    int ey = y + PARTY_Y + 21 + hoveredSlot * PARTY_ENTRY_H;
                    graphics.fill(x + PARTY_X + 1, ey, x + PARTY_X + PANEL_W - 1, ey + PARTY_ENTRY_H, COL_SWAP_HOVER);
                } else {
                    // Hovering over empty slot — normal drop zone
                    graphics.fill(x + PARTY_X, y + PARTY_Y, x + PARTY_X + PANEL_W, y + PARTY_Y + PANEL_H,
                            COL_DROP_ZONE);
                }
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchBox != null && searchBox.isFocused()) {
            if (keyCode == 256) { // GLFW_KEY_ESCAPE — allow screen to close
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
            searchBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (searchBox != null && searchBox.isFocused()) {
            return searchBox.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);

        int x = leftPos, y = topPos;

        // ── Update FloatingStates (once per state per frame) ─────────────────
        Set<UUID> updatedUUIDs = new HashSet<>();
        int visEnd = Math.min(scrollOffset + VISIBLE_ENTRIES, displayList.size());
        for (int i = scrollOffset; i < visEnd; i++) {
            UUID uuid = displayList.get(i).uuid();
            if (updatedUUIDs.add(uuid)) {
                networkStates.computeIfAbsent(uuid, k -> new FloatingState()).updatePartialTicks(partialTick);
            }
        }
        if (selectedUUID != null && updatedUUIDs.add(selectedUUID)) {
            networkStates.computeIfAbsent(selectedUUID, k -> new FloatingState()).updatePartialTicks(partialTick);
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

        // ── List panel header ─────────────────────────────────────────────────
        String listLabel = displayList.size() < pokemonList.size()
                ? "Network (" + displayList.size() + "/" + pokemonList.size() + ")"
                : "Network (" + pokemonList.size() + ")";
        graphics.drawString(font, listLabel, x + LIST_X, y + 16, COL_LABEL, false);

        // ── List entries ──────────────────────────────────────────────────────
        for (int i = scrollOffset; i < visEnd; i++) {
            int displayIdx = i - scrollOffset;
            int ey = y + LIST_Y + displayIdx * ENTRY_HEIGHT;
            SyncPokemonListPayload.PokemonEntry entry = displayList.get(i);

            boolean selected = entry.uuid().equals(selectedUUID);
            boolean hovered  = mouseX > x + LIST_X - 1 && mouseX < x + LIST_X + LIST_WIDTH - 1
                    && mouseY >= ey && mouseY < ey + ENTRY_HEIGHT;
            if (selected) {
                graphics.fill(x + LIST_X - 1, ey, x + LIST_X + LIST_WIDTH - 1, ey + ENTRY_HEIGHT - 1, COL_SELECTED);
            } else if (hovered) {
                graphics.fill(x + LIST_X - 1, ey, x + LIST_X + LIST_WIDTH - 1, ey + ENTRY_HEIGHT - 1, COL_HOVER);
            }

            FloatingState state = networkStates.computeIfAbsent(entry.uuid(), k -> new FloatingState());
            renderPokemonSprite(graphics, partialTick, entry.species(), entry.gender(),
                    state, x + LIST_X + 10, ey + ENTRY_HEIGHT / 2 - 5, 5.85F);

            int textY = ey + (ENTRY_HEIGHT - 9) / 2;
            String label = capitalize(entry.species()) + " Lv." + entry.level();
            graphics.drawString(font, label, x + LIST_X + 23, textY, COL_TEXT, false);

            // Shiny star indicator
            if (entry.shiny()) {
                graphics.drawString(font, " ★", x + LIST_X + 23 + font.width(label), textY, COL_GOLD, false);
            }

            // Gender symbol (kept away from scroll buttons)
            String gSym = genderSymbol(entry.gender());
            graphics.drawString(font, gSym,
                    x + LIST_X + LIST_WIDTH - 26 - font.width(gSym), textY, genderColor(entry.gender()), false);
        }
        if (displayList.isEmpty()) {
            String msg = pokemonList.isEmpty() ? "(no pokemon)" : "(no matches)";
            graphics.drawString(font, msg, x + LIST_X + 4, y + LIST_Y + 5, COL_MUTED, false);
        }

        // ── Detail panel ──────────────────────────────────────────────────────
        renderDetailPanel(graphics, x, y, partialTick);

        // ── Party panel ───────────────────────────────────────────────────────
        graphics.drawString(font, "Party", x + PARTY_X + 5, y + PARTY_Y + 7, COL_LABEL, false);
        graphics.drawString(font, "← drag to deposit", x + PARTY_X + 40, y + PARTY_Y + 7, COL_MUTED, false);
        for (int i = 0; i < 6; i++) {
            int ey = y + PARTY_Y + 21 + i * PARTY_ENTRY_H;
            final int slot = i;
            var entryOpt = partyList.stream().filter(e -> e.slot() == slot).findFirst();
            if (entryOpt.isPresent()) {
                var pe = entryOpt.get();
                boolean partyHovered = mouseX >= x + PARTY_X && mouseX < x + PARTY_X + PANEL_W
                        && mouseY >= ey && mouseY < ey + PARTY_ENTRY_H;
                // Highlight slot being dragged
                if (isDragging && dragSource == DragSource.PARTY && dragIndex == slot) {
                    graphics.fill(x + PARTY_X + 1, ey, x + PARTY_X + PANEL_W - 1, ey + PARTY_ENTRY_H, 0x44FFFFFF);
                } else if (selectedPartySlot == slot) {
                    graphics.fill(x + PARTY_X + 1, ey, x + PARTY_X + PANEL_W - 1, ey + PARTY_ENTRY_H, COL_SELECTED);
                } else if (partyHovered) {
                    graphics.fill(x + PARTY_X + 1, ey, x + PARTY_X + PANEL_W - 1, ey + PARTY_ENTRY_H, COL_HOVER);
                }
                renderPokemonSprite(graphics, partialTick, pe.species(), pe.gender(),
                        partyStates[i], x + PARTY_X + 13, ey + PARTY_ENTRY_H / 2 - 5, 7.5F);
                String nameLabel = capitalize(pe.species()) + " Lv." + pe.level();
                int textY = ey + (PARTY_ENTRY_H - 9) / 2;
                graphics.drawString(font, nameLabel, x + PARTY_X + 28, textY, COL_TEXT, false);
                int nw = font.width(nameLabel);
                String gSym = " " + genderSymbol(pe.gender());
                graphics.drawString(font, gSym, x + PARTY_X + 28 + nw, textY, genderColor(pe.gender()), false);
                if (pe.shiny()) {
                    graphics.drawString(font, " ★", x + PARTY_X + 28 + nw + font.width(gSym), textY, COL_GOLD, false);
                }
            } else {
                graphics.drawString(font, "(empty)", x + PARTY_X + 28, ey + 1, COL_MUTED, false);
            }
        }

        // ── Ghost sprite while dragging ───────────────────────────────────────
        if (isDragging && dragSpecies != null) {
            dragState.updatePartialTicks(partialTick);
            renderPokemonSprite(graphics, partialTick, dragSpecies, dragGender, dragState,
                    (int) dragX, (int) dragY, 12F);
        }

        renderTooltip(graphics, mouseX, mouseY);

        // Held item widget tooltip
        if (heldItemWidgetVisible
                && mouseX >= detailHeldItemScreenX && mouseX < detailHeldItemScreenX + 16
                && mouseY >= detailHeldItemScreenY && mouseY < detailHeldItemScreenY + 16) {
            if (!detailHeldItemStack.isEmpty()) {
                Minecraft mc = Minecraft.getInstance();
                var lines = detailHeldItemStack.getTooltipLines(
                        Item.TooltipContext.of(mc.level), mc.player, TooltipFlag.Default.NORMAL);
                graphics.renderTooltip(font, lines, java.util.Optional.empty(), mouseX, mouseY);
            } else {
                graphics.renderTooltip(font,
                        List.of(Component.literal("Empty").withStyle(s -> s.withColor(COL_MUTED))),
                        java.util.Optional.empty(), mouseX, mouseY);
            }
        }
    }

    private void renderDetailPanel(GuiGraphics graphics, int x, int y, float partialTick) {
        // Resolve data source — party takes priority over network
        String species, gender, nature, ability, type1, type2, heldItem;
        int level, ivHp, ivAtk, ivDef, ivSpA, ivSpD, ivSpe;
        int evHp, evAtk, evDef, evSpA, evSpD, evSpe;
        int statHp, statAtk, statDef, statSpA, statSpD, statSpe;
        boolean shiny;
        FloatingState state;

        if (selectedPartySlot >= 0) {
            var peOpt = partyList.stream().filter(e -> e.slot() == selectedPartySlot).findFirst();
            if (peOpt.isEmpty()) {
                renderDetailEmpty(graphics, x, y);
                return;
            }
            var p = peOpt.get();
            species = p.species(); gender = p.gender(); nature = p.nature(); ability = p.ability();
            type1 = p.type1(); type2 = p.type2(); heldItem = p.heldItem();
            level = p.level(); shiny = p.shiny();
            ivHp = p.ivHp(); ivAtk = p.ivAtk(); ivDef = p.ivDef();
            ivSpA = p.ivSpA(); ivSpD = p.ivSpD(); ivSpe = p.ivSpe();
            evHp = p.evHp(); evAtk = p.evAtk(); evDef = p.evDef();
            evSpA = p.evSpA(); evSpD = p.evSpD(); evSpe = p.evSpe();
            statHp = p.statHp(); statAtk = p.statAtk(); statDef = p.statDef();
            statSpA = p.statSpA(); statSpD = p.statSpD(); statSpe = p.statSpe();
            state = partyStates[selectedPartySlot];
        } else if (selectedUUID != null) {
            var eoOpt = pokemonList.stream().filter(e -> e.uuid().equals(selectedUUID)).findFirst();
            if (eoOpt.isEmpty()) {
                renderDetailEmpty(graphics, x, y);
                return;
            }
            var e = eoOpt.get();
            species = e.species(); gender = e.gender(); nature = e.nature(); ability = e.ability();
            type1 = e.type1(); type2 = e.type2(); heldItem = e.heldItem();
            level = e.level(); shiny = e.shiny();
            ivHp = e.ivHp(); ivAtk = e.ivAtk(); ivDef = e.ivDef();
            ivSpA = e.ivSpA(); ivSpD = e.ivSpD(); ivSpe = e.ivSpe();
            evHp = e.evHp(); evAtk = e.evAtk(); evDef = e.evDef();
            evSpA = e.evSpA(); evSpD = e.evSpD(); evSpe = e.evSpe();
            statHp = e.statHp(); statAtk = e.statAtk(); statDef = e.statDef();
            statSpA = e.statSpA(); statSpD = e.statSpD(); statSpe = e.statSpe();
            state = networkStates.computeIfAbsent(selectedUUID, k -> new FloatingState());
        } else {
            renderDetailEmpty(graphics, x, y);
            return;
        }

        detailHeldItemStack   = ItemStack.EMPTY;
        detailHeldItemScreenX = -1;
        detailHeldItemScreenY = -1;
        heldItemWidgetVisible = false;

        // ── Sprite (left column, 0-84px) ─────────────────────────────────────
        renderPokemonSprite(graphics, partialTick, species, gender,
                state, x + DETAIL_X + 42, y + DETAIL_Y + 7, 32.5F);

        // ── Text (right column, 88px onwards) ────────────────────────────────
        int tx = x + DETAIL_X + 88;
        int ty = y + DETAIL_Y + 8;

        // Species name + shiny indicator
        String speciesName = capitalize(species);
        graphics.drawString(font, speciesName, tx, ty, COL_GOLD, false);
        if (shiny) {
            graphics.drawString(font, " ★", tx + font.width(speciesName), ty, COL_GOLD, false);
        }
        ty += 16;

        // Level + gender
        String levelStr  = "Lv." + level + "  ";
        String genderStr = genderSymbol(gender);
        graphics.drawString(font, levelStr,  tx, ty, COL_TEXT, false);
        graphics.drawString(font, genderStr, tx + font.width(levelStr), ty, genderColor(gender), false);

        ty += 14;

        // Held item widget — fixed position below level/nature/ability, before types divider
        // Widget: 18×18 border box at (tx, ty), 16×16 item inside
        int widgetX = tx;
        int widgetY = ty;
        ItemStack heldStack = buildItemStack(heldItem);
        // Draw border box (always shown so player knows they can interact)
        drawBorderRect(graphics, widgetX - 1, widgetY - 1, 18, 18, COL_BORDER);
        graphics.fill(widgetX, widgetY, widgetX + 16, widgetY + 16, COL_PANEL);
        if (!heldStack.isEmpty()) {
            graphics.renderItem(heldStack, widgetX, widgetY);
        }
        detailHeldItemStack   = heldStack;  // may be empty — widget is always active
        detailHeldItemScreenX = widgetX;
        detailHeldItemScreenY = widgetY;
        heldItemWidgetVisible = true;
        ty += 20;

        graphics.drawString(font, formatNature(nature), tx, ty, COL_TEXT, false);
        ty += 14;

        String abilityLabel = "Abil: ";
        graphics.drawString(font, abilityLabel, tx, ty, COL_LABEL, false);
        graphics.drawString(font, formatAbility(ability),
                tx + font.width(abilityLabel), ty, COL_TEXT, false);
        ty += 14;

        // ── Types ─────────────────────────────────────────────────────────────
        int badgeX = tx;
        if (!type1.isEmpty()) {
            badgeX = drawTypeBadge(graphics, type1, badgeX, ty);
            badgeX += 4;
        }
        if (!type2.isEmpty()) {
            drawTypeBadge(graphics, type2, badgeX, ty);
        }

        // ── Divider ───────────────────────────────────────────────────────────
        // divY = DETAIL_Y+87+20 (extra 20px from held item widget) = DETAIL_Y+107
        int divY = y + DETAIL_Y + 107;
        graphics.fill(x + DETAIL_X + 5, divY, x + DETAIL_X + DETAIL_W - 5, divY + 1, COL_BORDER);

        // ── Unified IVs / EVs / Stats table ──────────────────────────────────
        // Layout: one shared column-header row, then three value rows with row labels.
        // Row label column is 22px wide; 6 data columns start at offset 22 from panel left.
        //   divY+8  : column headers (HP Atk Def SpA SpD Spe)
        //   divY+20 : IVs row
        //   divY+32 : EVs row
        //   divY+44 : Stats row
        // Max used = DETAIL_Y+107+44+9 = DETAIL_Y+160 < DETAIL_H=205 ✓

        int tableBaseX  = x + DETAIL_X + 5;
        int labelColW   = 22;                        // width reserved for row label on the left
        String[] colNames  = {"HP", "Atk", "Def", "SpA", "SpD", "Spe"};
        int[]    colOffsets = {0, 23, 46, 69, 92, 115}; // relative to (tableBaseX + labelColW)

        // Column header row
        int headerY = divY + 8;
        for (int col = 0; col < 6; col++) {
            graphics.drawString(font, colNames[col],
                    tableBaseX + labelColW + colOffsets[col], headerY, COL_MUTED, false);
        }

        // IVs row
        int[] ivValues = {ivHp, ivAtk, ivDef, ivSpA, ivSpD, ivSpe};
        int ivRowY = divY + 20;
        graphics.drawString(font, "IVs", tableBaseX, ivRowY, COL_LABEL, false);
        for (int col = 0; col < 6; col++) {
            int cx = tableBaseX + labelColW + colOffsets[col];
            String val = String.valueOf(ivValues[col]);
            int colW = col < 5 ? colOffsets[col + 1] - colOffsets[col] : 20;
            int vw = font.width(val);
            graphics.drawString(font, val, cx + Math.max(0, (colW - vw) / 2), ivRowY, ivColor(ivValues[col]), false);
        }

        // EVs row
        int[] evValues = {evHp, evAtk, evDef, evSpA, evSpD, evSpe};
        int evRowY = divY + 32;
        graphics.drawString(font, "EVs", tableBaseX, evRowY, COL_LABEL, false);
        for (int col = 0; col < 6; col++) {
            int cx = tableBaseX + labelColW + colOffsets[col];
            String val = String.valueOf(evValues[col]);
            int colW = col < 5 ? colOffsets[col + 1] - colOffsets[col] : 20;
            int vw = font.width(val);
            graphics.drawString(font, val, cx + Math.max(0, (colW - vw) / 2), evRowY, evColor(evValues[col]), false);
        }

        // Stats row
        int[] statValues = {statHp, statAtk, statDef, statSpA, statSpD, statSpe};
        int statRowY = divY + 44;
        graphics.drawString(font, "Stat", tableBaseX, statRowY, COL_LABEL, false);
        for (int col = 0; col < 6; col++) {
            int cx = tableBaseX + labelColW + colOffsets[col];
            String val = String.valueOf(statValues[col]);
            int colW = col < 5 ? colOffsets[col + 1] - colOffsets[col] : 20;
            int vw = font.width(val);
            graphics.drawString(font, val, cx + Math.max(0, (colW - vw) / 2), statRowY, COL_TEXT, false);
        }
    }

    private void renderDetailEmpty(GuiGraphics graphics, int x, int y) {
        int midY = y + DETAIL_Y + DETAIL_H / 2 - 9;
        graphics.drawString(font, "Select a Pokemon", x + DETAIL_X + 29, midY, COL_MUTED, false);
        graphics.drawString(font, "drag to party →", x + DETAIL_X + 35, midY + 14, COL_MUTED, false);
    }

    // ── Sprite rendering ─────────────────────────────────────────────────────

    /**
     * Render an animated Pokemon profile sprite at the given GUI screen center position.
     * @param state FloatingState — must have updatePartialTicks() called exactly once this frame.
     */
    private void renderPokemonSprite(GuiGraphics graphics, float partialTick,
                                     String species, String gender,
                                     FloatingState state,
                                     int centerX, int centerY, float scale) {
        // Strip any characters not valid in a ResourceLocation path (e.g. Farfetch'd → farfetchd)
        String safePath = species.toLowerCase().replaceAll("[^a-z0-9/._-]", "");
        if (safePath.isEmpty()) return;
        ResourceLocation speciesId = ResourceLocation.fromNamespaceAndPath("cobblemon", safePath);
        if (PokemonSpecies.INSTANCE.getByIdentifier(speciesId) == null) return;

        Set<String> aspects = new HashSet<>();
        if ("FEMALE".equalsIgnoreCase(gender)) aspects.add("female");
        state.setCurrentAspects(aspects);

        var poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(centerX, centerY, 200.0);

        PokemonGuiUtilsKt.drawProfilePokemon(
                speciesId, poseStack, freshProfileRotation(), PoseType.PROFILE, state,
                partialTick, scale, true, false, false,
                1f, 1f, 1f, 1f, 0f, 0f);

        poseStack.popPose();
    }

    // ── Input ────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // Held item widget click — must check BEFORE drag logic to avoid conflict
            if (heldItemWidgetVisible
                    && mouseX >= detailHeldItemScreenX && mouseX < detailHeldItemScreenX + 16
                    && mouseY >= detailHeldItemScreenY && mouseY < detailHeldItemScreenY + 16) {
                if (selectedUUID != null || selectedPartySlot >= 0) {
                    PacketDistributor.sendToServer(new SetHeldItemPayload(
                            menu.containerId,
                            selectedUUID,
                            selectedPartySlot));
                    return true;
                }
            }

            // Check network list — record drag source and update selection
            int relX = (int)(mouseX - leftPos - LIST_X);
            int relY = (int)(mouseY - topPos - LIST_Y);
            if (relX >= 0 && relX < LIST_WIDTH && relY >= 0 && relY < VISIBLE_ENTRIES * ENTRY_HEIGHT) {
                int idx = relY / ENTRY_HEIGHT + scrollOffset;
                if (idx >= 0 && idx < displayList.size()) {
                    var entry = displayList.get(idx);
                    selectedUUID      = entry.uuid();
                    selectedPartySlot = -1;
                    dragSource  = DragSource.NETWORK;
                    dragIndex   = idx;
                    dragSpecies = entry.species();
                    dragGender  = entry.gender();
                    dragStartX  = mouseX;
                    dragStartY  = mouseY;
                    dragX       = mouseX;
                    dragY       = mouseY;
                    isDragging  = false;
                    return true;
                }
            }
            // Check party slots
            for (int i = 0; i < 6; i++) {
                int ey = topPos + PARTY_Y + 21 + i * PARTY_ENTRY_H;
                if (mouseX >= leftPos + PARTY_X && mouseX < leftPos + PARTY_X + PANEL_W
                        && mouseY >= ey && mouseY < ey + PARTY_ENTRY_H) {
                    final int slot = i;
                    var entryOpt = partyList.stream().filter(e -> e.slot() == slot).findFirst();
                    if (entryOpt.isPresent()) {
                        var pe = entryOpt.get();
                        selectedPartySlot = slot;
                        selectedUUID      = null;
                        dragSource  = DragSource.PARTY;
                        dragIndex   = slot;
                        dragSpecies = pe.species();
                        dragGender  = pe.gender();
                        dragStartX  = mouseX;
                        dragStartY  = mouseY;
                        dragX       = mouseX;
                        dragY       = mouseY;
                        isDragging  = false;
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && dragSource != DragSource.NONE) {
            this.dragX = mouseX;
            this.dragY = mouseY;
            if (!isDragging) {
                double dx = mouseX - dragStartX;
                double dy = mouseY - dragStartY;
                if (Math.sqrt(dx * dx + dy * dy) >= 4.0) {
                    isDragging = true;
                }
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && dragSource != DragSource.NONE) {
            if (isDragging) {
                if (dragSource == DragSource.PARTY && isInNetworkListZone(mouseX, mouseY)) {
                    UUID targetUUID = getNetworkEntryUUIDAt(mouseX, mouseY);
                    if (targetUUID != null) {
                        PacketDistributor.sendToServer(new SwapPokemonPayload(menu.containerId, targetUUID, dragIndex));
                    } else {
                        PacketDistributor.sendToServer(new DepositPokemonPayload(menu.containerId, dragIndex));
                    }
                } else if (dragSource == DragSource.NETWORK && isInPartyZone(mouseX, mouseY) && selectedUUID != null) {
                    int targetSlot = getPartySlotAt(mouseY);
                    boolean slotOccupied = targetSlot >= 0 && partyList.stream().anyMatch(e -> e.slot() == targetSlot);
                    if (slotOccupied) {
                        PacketDistributor.sendToServer(new SwapPokemonPayload(menu.containerId, selectedUUID, targetSlot));
                    } else {
                        PacketDistributor.sendToServer(new WithdrawPokemonPayload(menu.containerId, selectedUUID));
                    }
                }
            }
            dragSource  = DragSource.NONE;
            isDragging  = false;
            dragIndex   = -1;
            dragSpecies = null;
            dragGender  = null;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean isInNetworkListZone(double mouseX, double mouseY) {
        int lx = leftPos + LIST_X;
        int ly = topPos + LIST_Y;
        return mouseX >= lx && mouseX < lx + LIST_WIDTH - 20
                && mouseY >= ly && mouseY < ly + VISIBLE_ENTRIES * ENTRY_HEIGHT;
    }

    private boolean isInPartyZone(double mouseX, double mouseY) {
        return mouseX >= leftPos + PARTY_X && mouseX < leftPos + PARTY_X + PANEL_W
                && mouseY >= topPos + PARTY_Y && mouseY < topPos + PARTY_Y + PANEL_H;
    }

    /** Returns the UUID of the network list entry under the mouse, or null if none. */
    private UUID getNetworkEntryUUIDAt(double mouseX, double mouseY) {
        int relY = (int)(mouseY - topPos - LIST_Y);
        if (relY < 0 || relY >= VISIBLE_ENTRIES * ENTRY_HEIGHT) return null;
        int idx = relY / ENTRY_HEIGHT + scrollOffset;
        if (idx >= 0 && idx < displayList.size()) {
            return displayList.get(idx).uuid();
        }
        return null;
    }

    /** Returns the party slot index (0-5) under the mouse Y, or -1 if none. */
    private int getPartySlotAt(double mouseY) {
        for (int i = 0; i < 6; i++) {
            int ey = topPos + PARTY_Y + 21 + i * PARTY_ENTRY_H;
            if (mouseY >= ey && mouseY < ey + PARTY_ENTRY_H) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll(scrollY < 0 ? 1 : -1);
        return true;
    }

    // ── Draw helpers ─────────────────────────────────────────────────────────

    private static void drawPanel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, COL_PANEL);
        drawBorderRect(g, x, y, w, h, COL_BORDER);
    }

    private static void drawBorderRect(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x,         y,         x + w, y + 1,     color);
        g.fill(x,         y + h - 1, x + w, y + h,     color);
        g.fill(x,         y,         x + 1, y + h,     color);
        g.fill(x + w - 1, y,         x + w, y + h,     color);
    }

    // ── Stat color helpers ────────────────────────────────────────────────────

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

    // ── Text / display helpers ────────────────────────────────────────────────

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
        graphics.fill(x, y - 1, x + w, y + 9, typeColor(type));
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

    private static String formatAbility(String ability) {
        if (ability == null || ability.isEmpty()) return "???";
        String path = ability.contains(":") ? ability.substring(ability.indexOf(':') + 1) : ability;
        String[] parts = path.split("[_\\-]");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!sb.isEmpty()) sb.append(" ");
            if (!part.isEmpty()) sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return !sb.isEmpty() ? sb.toString() : capitalize(path);
    }
}
