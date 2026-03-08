package cc.nilm.mcmod.ae2mon.common.key;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class PokemonKeyType extends AEKeyType {
    // ID must be declared BEFORE INSTANCE so it's initialized before the constructor runs
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath("ae2mon", "pokemon");

    public static final PokemonKeyType INSTANCE = new PokemonKeyType();

    private PokemonKeyType() {
        super(ID, PokemonKey.class, Component.literal("Pokémon"));
    }

    @Override
    public MapCodec<? extends AEKey> codec() {
        return PokemonKey.MAP_CODEC;
    }

    @Override
    public @Nullable AEKey readFromPacket(RegistryFriendlyByteBuf buf) {
        return PokemonKey.readKey(buf);
    }

    @Override
    public @Nullable AEKey loadKeyFromTag(HolderLookup.Provider registries, CompoundTag tag) {
        if (!tag.hasUUID("uuid")) return null;
        UUID uuid = tag.getUUID("uuid");
        CompoundTag data = tag.getCompound("data");
        return new PokemonKey(uuid, data);
    }
}
