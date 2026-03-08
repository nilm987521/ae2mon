package cc.nilm.mcmod.ae2mon.common.key;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class PokemonKey extends AEKey {

    public static final PokemonKeyType TYPE = PokemonKeyType.INSTANCE;

    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

    private static final Codec<CompoundTag> SNBT_CODEC = Codec.STRING.flatXmap(
            s -> {
                try {
                    return DataResult.success(TagParser.parseTag(s));
                } catch (CommandSyntaxException e) {
                    return DataResult.error(e::getMessage);
                }
            },
            tag -> DataResult.success(tag.toString())
    );

    public static final MapCodec<PokemonKey> MAP_CODEC = RecordCodecBuilder.mapCodec(builder ->
            builder.group(
                    UUID_CODEC.fieldOf("uuid").forGetter(PokemonKey::getUuid),
                    SNBT_CODEC.fieldOf("data").forGetter(PokemonKey::getData)
            ).apply(builder, PokemonKey::new)
    );

    private final UUID uuid;
    private final CompoundTag data;

    public PokemonKey(UUID uuid, CompoundTag data) {
        this.uuid = uuid;
        this.data = data;
    }

    public static PokemonKey of(Player player, Pokemon pokemon) {
        //noinspection resource  -- Level (MC world) is not AutoCloseable; false positive
        CompoundTag tag = pokemon.saveToNBT(player.level().registryAccess(), new CompoundTag());
        return new PokemonKey(pokemon.getUuid(), tag);
    }

    public static PokemonKey readKey(RegistryFriendlyByteBuf buf) {
        UUID uuid = buf.readUUID();
        CompoundTag data = buf.readNbt();
        return new PokemonKey(uuid, data != null ? data : new CompoundTag());
    }

    @Override
    public AEKeyType getType() {
        return TYPE;
    }

    @Override
    public AEKey dropSecondary() {
        return this;
    }

    @Override
    public CompoundTag toTag(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("uuid", uuid);
        tag.put("data", data.copy());
        return tag;
    }

    @Override
    public Object getPrimaryKey() {
        // Must return a stable singleton for reference-equality KeyCounter (Reference2ObjectOpenHashMap).
        // If we returned getUuid(), each getAvailableStacks() call would create new UUID objects,
        // causing KeyCounter.removeAll() to never match old vs new entries → infinite terminal refresh.
        return PokemonKeyType.INSTANCE;
    }

    @Override
    public ResourceLocation getId() {
        return ResourceLocation.fromNamespaceAndPath("ae2mon", "pokemon");
    }

    @Override
    public void writeToPacket(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeNbt(data);
    }

    @Override
    protected Component computeDisplayName() {
        String speciesId = data.getString("Species");
        String nature    = data.getString("Nature");
        int level        = data.getInt("Level");
        // "cobblemon:pikachu" -> "pikachu"
        String species = speciesId.contains(":") ? speciesId.substring(speciesId.indexOf(':') + 1) : speciesId;
        species = capitalize(species);
        return Component.literal(species + " Lv." + level + (nature.isEmpty() ? "" : " [" + nature + "]"));
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    /**
     * Returns a placeholder ItemStack so standard AE2 terminals can render this key
     * without crashing. Players cannot interact with it from a standard terminal.
     */
    @Override
    public ItemStack wrapForDisplayOrFilter() {
        ItemStack stack = new ItemStack(Items.ENDER_PEARL);
        CompoundTag tag = new CompoundTag();
        tag.putUUID("pokemon_uuid", uuid);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    @Override
    public void addDrops(long amount, List<ItemStack> drops, Level level, BlockPos pos) {
        // Pokemon do not drop as items
    }

    @Override
    public boolean hasComponents() {
        return false;
    }

    public UUID getUuid() {
        return uuid;
    }

    public CompoundTag getData() {
        return data.copy();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PokemonKey other)) return false;
        return Objects.equals(uuid, other.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(uuid);
    }
}
