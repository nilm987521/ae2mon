package cc.nilm.mcmod.ae2mon.common.item;

import appeng.api.config.FuzzyMode;
import appeng.api.storage.cells.ICellWorkbenchItem;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import cc.nilm.mcmod.ae2mon.common.cell.PokemonStorageCell;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

public class PokemonCellItem extends Item implements ICellWorkbenchItem {

    public PokemonCellItem() {
        super(new Item.Properties().stacksTo(1));
    }

    // ── ICellWorkbenchItem ────────────────────────────────────────────────────

    @Override
    public boolean isEditable(ItemStack is) {
        return true;
    }

    @Override
    public FuzzyMode getFuzzyMode(ItemStack is) {
        CustomData data = is.get(DataComponents.CUSTOM_DATA);
        if (data == null) return FuzzyMode.IGNORE_ALL;
        int ordinal = data.copyTag().getInt("fuzzyMode");
        FuzzyMode[] values = FuzzyMode.values();
        return (ordinal >= 0 && ordinal < values.length) ? values[ordinal] : FuzzyMode.IGNORE_ALL;
    }

    @Override
    public IUpgradeInventory getUpgrades(ItemStack is) {
        return UpgradeInventories.forItem(is, 3);
    }

    @Override
    public void setFuzzyMode(ItemStack is, FuzzyMode fzMode) {
        is.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, existing -> {
            CompoundTag tag = existing.copyTag();
            tag.putInt("fuzzyMode", fzMode.ordinal());
            return CustomData.of(tag);
        });
    }

    // ── Tooltip ───────────────────────────────────────────────────────────────

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        int stored = getStoredCount(stack);
        tooltip.add(Component.literal(stored + " / " + PokemonStorageCell.MAX_POKEMON + " Pokémon stored"));
    }

    private int getStoredCount(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return 0;
        CompoundTag tag = customData.copyTag();
        ListTag list = tag.getList("StoredPokemon", Tag.TAG_COMPOUND);
        return list.size();
    }
}
