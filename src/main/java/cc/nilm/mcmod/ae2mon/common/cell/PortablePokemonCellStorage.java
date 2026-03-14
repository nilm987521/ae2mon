package cc.nilm.mcmod.ae2mon.common.cell;

import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PortablePokemonCellStorage {

    public static final int MAX_POKEMON = 6;
    private static final String TAG_STORED = "StoredPokemon";

    public record CellEntry(UUID uuid, String species, int level) {}

    private PortablePokemonCellStorage() {}

    private static CompoundTag getTag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null ? data.copyTag() : new CompoundTag();
    }

    private static void setTag(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static ListTag getRawList(ItemStack stack) {
        return getTag(stack).getList(TAG_STORED, Tag.TAG_COMPOUND);
    }

    private static void setRawList(ItemStack stack, ListTag list) {
        CompoundTag tag = getTag(stack);
        tag.put(TAG_STORED, list);
        setTag(stack, tag);
    }

    /** Heals the Pokemon, serializes it, and stores it in the ItemStack NBT. Returns false if full. */
    public static boolean deposit(Pokemon pokemon, ItemStack stack, ServerPlayer player) {
        ListTag list = getRawList(stack);
        if (list.size() >= MAX_POKEMON) return false;

        pokemon.heal();

        CompoundTag pokemonNbt = pokemon.saveToNBT(player.level().registryAccess(), new CompoundTag());
        CompoundTag entry = new CompoundTag();
        entry.putUUID("uuid", pokemon.getUuid());
        entry.put("pokemonData", pokemonNbt);
        list.add(entry);
        setRawList(stack, list);
        return true;
    }

    /** Finds the Pokemon by UUID, removes it from NBT, and returns it. Returns null if not found. */
    public static Pokemon withdraw(UUID uuid, ItemStack stack, ServerPlayer player) {
        ListTag list = getRawList(stack);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (entry.hasUUID("uuid") && entry.getUUID("uuid").equals(uuid)) {
                CompoundTag pokemonNbt = entry.getCompound("pokemonData");
                list.remove(i);
                setRawList(stack, list);
                try {
                    return Pokemon.Companion.loadFromNBT(player.level().registryAccess(), pokemonNbt);
                } catch (Exception e) {
                    // Rollback on failure
                    list.add(i, entry);
                    setRawList(stack, list);
                    return null;
                }
            }
        }
        return null;
    }

    /** Returns the summary list of stored Pokemon (UUID + display info). */
    public static List<CellEntry> getStoredList(ItemStack stack) {
        ListTag list = getRawList(stack);
        List<CellEntry> result = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (entry.hasUUID("uuid")) {
                UUID uuid = entry.getUUID("uuid");
                CompoundTag pokemonData = entry.getCompound("pokemonData");
                String speciesId = pokemonData.getString("Species");
                String species = speciesId.contains(":") ? speciesId.substring(speciesId.indexOf(':') + 1) : speciesId;
                int level = pokemonData.getInt("Level");
                result.add(new CellEntry(uuid, species, level));
            }
        }
        return result;
    }

    /** Returns true if the cell is at maximum capacity. */
    public static boolean isFull(ItemStack stack) {
        return getRawList(stack).size() >= MAX_POKEMON;
    }

    /** Returns the number of stored Pokemon. */
    public static int getStoredCount(ItemStack stack) {
        return getRawList(stack).size();
    }
}
