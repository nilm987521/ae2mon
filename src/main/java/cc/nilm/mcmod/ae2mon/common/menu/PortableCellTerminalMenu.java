package cc.nilm.mcmod.ae2mon.common.menu;

import appeng.api.config.Actionable;
import appeng.api.networking.IGridNode;
import appeng.api.storage.MEStorage;
import cc.nilm.mcmod.ae2mon.CobblemonAE2;
import cc.nilm.mcmod.ae2mon.common.cell.PortablePokemonCellStorage;
import cc.nilm.mcmod.ae2mon.common.cell.PokemonStorageCell;
import cc.nilm.mcmod.ae2mon.common.item.PortablePokemonCellItem;
import cc.nilm.mcmod.ae2mon.common.registry.ModMenuTypes;
import com.cobblemon.mod.common.Cobblemon;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class PortableCellTerminalMenu extends PokemonTerminalMenu {

    private final InteractionHand hand;

    /** Client-side factory called by IMenuTypeExtension. */
    public static PortableCellTerminalMenu fromNetwork(int containerId, Inventory inventory, FriendlyByteBuf buf) {
        InteractionHand hand = buf.readBoolean() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        return new PortableCellTerminalMenu(ModMenuTypes.PORTABLE_POKEMON_CELL.get(), containerId, inventory, hand);
    }

    public PortableCellTerminalMenu(MenuType<?> type, int containerId, Inventory playerInventory, InteractionHand hand) {
        super(type, containerId, playerInventory, new PortableCellHost(playerInventory.player, hand));
        this.hand = hand;
    }

    public ItemStack getCellStack(Player player) {
        return player.getItemInHand(hand);
    }

    // ── Energy-aware deposit / withdraw ──────────────────────────────────

    @Override
    public boolean depositPokemon(int partySlot, ServerPlayer player) {
        ItemStack stack = getCellStack(player);
        if (!(stack.getItem() instanceof PortablePokemonCellItem cellItem)) return false;
        if (!cellItem.hasPower(stack, 100.0)) {
            CobblemonAE2.LOGGER.warn("portable deposit: insufficient power");
            return false;
        }
        // Heal the pokemon before the parent serialises it into the key
        try {
            var pokemon = Cobblemon.INSTANCE.getStorage().getParty(player).get(partySlot);
            if (pokemon != null) pokemon.heal();
        } catch (Exception ignored) {}

        boolean ok = super.depositPokemon(partySlot, player);
        if (ok) cellItem.extractAEPower(stack, 100.0, Actionable.MODULATE);
        return ok;
    }

    @Override
    public boolean withdrawPokemon(UUID uuid, ServerPlayer player) {
        ItemStack stack = getCellStack(player);
        if (!(stack.getItem() instanceof PortablePokemonCellItem cellItem)) return false;
        if (!cellItem.hasPower(stack, 50.0)) {
            CobblemonAE2.LOGGER.warn("portable withdraw: insufficient power");
            return false;
        }
        boolean ok = super.withdrawPokemon(uuid, player);
        if (ok) cellItem.extractAEPower(stack, 50.0, Actionable.MODULATE);
        return ok;
    }

    @Override
    public boolean stillValid(Player player) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof PortablePokemonCellItem cellItem)) return false;
        return cellItem.hasPower(stack, 50.0);
    }

    // ── Host ──────────────────────────────────────────────────────────────

    private static class PortableCellHost implements IPokemonTerminalHost {

        private final Player player;
        private final InteractionHand hand;

        PortableCellHost(Player player, InteractionHand hand) {
            this.player = player;
            this.hand = hand;
        }

        @Override
        public @Nullable MEStorage getNetworkStorage() {
            ItemStack stack = player.getItemInHand(hand);
            if (!(stack.getItem() instanceof PortablePokemonCellItem)) return null;
            return new PokemonStorageCell(stack, null, PortablePokemonCellStorage.MAX_POKEMON);
        }

        @Override
        public boolean isPowered() {
            ItemStack stack = player.getItemInHand(hand);
            if (!(stack.getItem() instanceof PortablePokemonCellItem cellItem)) return false;
            return cellItem.hasPower(stack, 50.0);
        }

        @Override
        public @Nullable IGridNode getActionableNode() {
            return null; // no AE2 network node needed for portable cell
        }
    }
}
