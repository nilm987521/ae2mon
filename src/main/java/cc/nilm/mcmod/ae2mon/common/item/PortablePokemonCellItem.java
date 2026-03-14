package cc.nilm.mcmod.ae2mon.common.item;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.implementations.items.IAEItemPowerStorage;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.api.upgrades.Upgrades;
import cc.nilm.mcmod.ae2mon.common.cell.PortablePokemonCellStorage;
import cc.nilm.mcmod.ae2mon.common.network.ModPayloads;
import cc.nilm.mcmod.ae2mon.common.registry.ModMenuTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;

public class PortablePokemonCellItem extends Item implements IAEItemPowerStorage {

    private static final double BASE_POWER = 1700.0;
    private static final String TAG_POWER = "AEPower";

    public PortablePokemonCellItem() {
        super(new Item.Properties().stacksTo(1));
    }

    public IUpgradeInventory getUpgrades(ItemStack is) {
        return UpgradeInventories.forItem(is, 1);
    }

    // ── Power NBT helpers ─────────────────────────────────────────────────

    private static double readPower(ItemStack is) {
        CustomData data = is.get(DataComponents.CUSTOM_DATA);
        return data != null ? data.copyTag().getDouble(TAG_POWER) : 0.0;
    }

    private static void writePower(ItemStack is, double power) {
        CustomData existing = is.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = existing != null ? existing.copyTag() : new CompoundTag();
        tag.putDouble(TAG_POWER, power);
        is.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    // ── IAEItemPowerStorage ───────────────────────────────────────────────

    @Override
    public double getAEMaxPower(ItemStack is) {
        int multiplier = Upgrades.getEnergyCardMultiplier(getUpgrades(is));
        return BASE_POWER * (multiplier > 0 ? multiplier : 1);
    }

    @Override
    public double getAECurrentPower(ItemStack is) {
        return readPower(is);
    }

    @Override
    public double injectAEPower(ItemStack is, double amount, Actionable mode) {
        double current = readPower(is);
        double max = getAEMaxPower(is);
        double canAccept = max - current;
        double toAccept = Math.min(amount, canAccept);
        if (mode == Actionable.MODULATE) {
            writePower(is, current + toAccept);
        }
        return amount - toAccept; // leftover not stored
    }

    @Override
    public double extractAEPower(ItemStack is, double amount, Actionable mode) {
        double current = readPower(is);
        double toExtract = Math.min(amount, current);
        if (mode == Actionable.MODULATE) {
            writePower(is, current - toExtract);
        }
        return toExtract;
    }

    @Override
    public AccessRestriction getPowerFlow(ItemStack is) {
        return AccessRestriction.READ_WRITE;
    }

    @Override
    public double getChargeRate(ItemStack is) {
        return 160.0;
    }

    /** Returns true if the item has at least {@code amount} AE stored. */
    public boolean hasPower(ItemStack is, double amount) {
        return readPower(is) >= amount;
    }

    // ── Item bar (energy display in slot) ────────────────────────────────

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        double filled = getAECurrentPower(stack) / getAEMaxPower(stack);
        return Mth.clamp((int) Math.round(filled * 13), 0, 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return Mth.hsvToRgb(1 / 3.0F, 1.0F, 1.0F); // green
    }

    // ── Item overrides ────────────────────────────────────────────────────

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!hasPower(stack, 50.0)) {
            if (!level.isClientSide()) {
                player.displayClientMessage(
                        Component.translatable("item.ae2mon.portable_pokemon_cell.no_power"), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(
                    new SimpleMenuProvider(
                            (containerId, inventory, p) -> new cc.nilm.mcmod.ae2mon.common.menu.PortableCellTerminalMenu(
                                    ModMenuTypes.PORTABLE_POKEMON_CELL.get(), containerId, inventory, hand),
                            Component.translatable("item.ae2mon.portable_pokemon_cell")),
                    buf -> buf.writeBoolean(hand == InteractionHand.MAIN_HAND)
            );
            if (serverPlayer.containerMenu instanceof cc.nilm.mcmod.ae2mon.common.menu.PokemonTerminalMenu menu) {
                ModPayloads.sendSyncToPlayer(menu, serverPlayer);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        int stored = PortablePokemonCellStorage.getStoredCount(stack);
        tooltip.add(Component.literal(stored + " / " + PortablePokemonCellStorage.MAX_POKEMON + " Pokémon stored"));
        double current = readPower(stack);
        double max = getAEMaxPower(stack);
        tooltip.add(Component.literal(String.format("%.1f / %.0f AE", current, max)));
    }
}
