package cc.nilm.mcmod.ae2mon.common.cell;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.cells.CellState;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.cells.StorageCell;
import cc.nilm.mcmod.ae2mon.common.item.PokemonCellItem;
import cc.nilm.mcmod.ae2mon.common.key.PokemonKey;
import cc.nilm.mcmod.ae2mon.common.registry.ModItems;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PokemonStorageCell implements StorageCell {

    public static final int MAX_POKEMON = 32;
    private static final String TAG_STORED = "StoredPokemon";

    private final ItemStack stack;
    private final @Nullable ISaveProvider saveProvider;
    private final @Nullable String requiredType;

    public PokemonStorageCell(ItemStack stack, @Nullable ISaveProvider saveProvider) {
        this.stack = stack;
        this.saveProvider = saveProvider;

        String found = null;
        if (stack.getItem() instanceof PokemonCellItem cellItem) {
            for (ItemStack card : cellItem.getUpgrades(stack)) {
                if (!card.isEmpty()) {
                    String type = ModItems.ITEM_TO_TYPE.get(card.getItem());
                    if (type != null) {
                        found = type;
                        break;
                    }
                }
            }
        }
        this.requiredType = found;
    }

    private CompoundTag getTag() {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null ? customData.copyTag() : new CompoundTag();
    }

    private void setTag(CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private ListTag getStoredList() {
        CompoundTag tag = getTag();
        return tag.getList(TAG_STORED, Tag.TAG_COMPOUND);
    }

    private void setStoredList(ListTag list) {
        CompoundTag tag = getTag();
        tag.put(TAG_STORED, list);
        setTag(tag);
    }

    @Override
    public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
        if (!(what instanceof PokemonKey pokemonKey)) return 0;
        if (amount <= 0) return 0;

        if (requiredType != null) {
            String[] types = getTypesFromData(pokemonKey.getData());
            if (!requiredType.equals(types[0]) && !requiredType.equals(types[1])) {
                return 0;
            }
        }

        ListTag list = getStoredList();
        if (list.size() >= MAX_POKEMON) return 0;

        UUID targetUuid = pokemonKey.getUuid();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (entry.hasUUID("uuid") && entry.getUUID("uuid").equals(targetUuid)) {
                return 0;
            }
        }

        if (mode == Actionable.MODULATE) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("uuid", pokemonKey.getUuid());
            entry.put("pokemonData", pokemonKey.getData().copy());
            list.add(entry);
            setStoredList(list);
            persist();
        }

        return 1;
    }

    @Override
    public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
        if (!(what instanceof PokemonKey pokemonKey)) return 0;
        if (amount <= 0) return 0;

        ListTag list = getStoredList();
        UUID targetUuid = pokemonKey.getUuid();

        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (entry.hasUUID("uuid") && entry.getUUID("uuid").equals(targetUuid)) {
                if (mode == Actionable.MODULATE) {
                    list.remove(i);
                    setStoredList(list);
                    persist();
                }
                return 1;
            }
        }

        return 0;
    }

    @Override
    public void getAvailableStacks(KeyCounter out) {
        ListTag list = getStoredList();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (entry.hasUUID("uuid")) {
                UUID uuid = entry.getUUID("uuid");
                CompoundTag pokemonData = entry.getCompound("pokemonData");
                out.add(new PokemonKey(uuid, pokemonData), 1);
            }
        }
    }

    @Override
    public CellState getStatus() {
        ListTag list = getStoredList();
        if (list.isEmpty()) return CellState.EMPTY;
        if (list.size() >= MAX_POKEMON) return CellState.FULL;
        return CellState.NOT_EMPTY;
    }

    @Override
    public Component getDescription() {
        return Component.literal("Pokemon Cell");
    }

    @Override
    public double getIdleDrain() {
        return 0.5;
    }

    @Override
    public void persist() {
        if (saveProvider != null) {
            saveProvider.saveChanges();
        }
    }

    public int getStoredCount() {
        return getStoredList().size();
    }

    private static String[] getTypesFromData(CompoundTag data) {
        String speciesId = data.getString("Species");
        ResourceLocation loc = ResourceLocation.tryParse(speciesId);
        if (loc == null) return new String[]{"", ""};
        var species = PokemonSpecies.INSTANCE.getByIdentifier(loc);
        if (species == null) return new String[]{"", ""};
        String t1 = species.getPrimaryType().getName().toLowerCase();
        var t2 = species.getSecondaryType();
        return new String[]{t1, t2 != null ? t2.getName().toLowerCase() : ""};
    }

    /** Returns UUID → insertion index (0-based) in the order pokemon were deposited. */
    public Map<UUID, Integer> getInsertionOrderMap() {
        ListTag list = getStoredList();
        Map<UUID, Integer> map = new HashMap<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (entry.hasUUID("uuid")) {
                map.put(entry.getUUID("uuid"), i);
            }
        }
        return map;
    }
}
