package cc.nilm.mcmod.ae2mon.common.cell;

import appeng.api.storage.cells.ICellHandler;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.cells.StorageCell;
import cc.nilm.mcmod.ae2mon.common.item.PortablePokemonCellItem;
import cc.nilm.mcmod.ae2mon.common.item.PokemonCellItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class PokemonCellHandler implements ICellHandler {

    public static final PokemonCellHandler INSTANCE = new PokemonCellHandler();

    private PokemonCellHandler() {}

    @Override
    public boolean isCell(ItemStack is) {
        return is != null && (is.getItem() instanceof PokemonCellItem
                || is.getItem() instanceof PortablePokemonCellItem);
    }

    @Override
    public @Nullable StorageCell getCellInventory(ItemStack is, @Nullable ISaveProvider host) {
        if (is.getItem() instanceof PokemonCellItem) {
            return new PokemonStorageCell(is, host, PokemonStorageCell.MAX_POKEMON);
        }
        if (is.getItem() instanceof PortablePokemonCellItem) {
            return new PokemonStorageCell(is, host, PortablePokemonCellStorage.MAX_POKEMON);
        }
        return null;
    }
}
