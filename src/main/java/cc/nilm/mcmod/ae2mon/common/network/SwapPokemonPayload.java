package cc.nilm.mcmod.ae2mon.common.network;

import cc.nilm.mcmod.ae2mon.CobblemonAE2;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record SwapPokemonPayload(int containerId, UUID networkUUID, int partySlot) implements CustomPacketPayload {

    public static final Type<SwapPokemonPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CobblemonAE2.MOD_ID, "swap_pokemon"));

    public static final StreamCodec<FriendlyByteBuf, SwapPokemonPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeInt(payload.containerId());
                buf.writeUUID(payload.networkUUID());
                buf.writeInt(payload.partySlot());
            },
            buf -> new SwapPokemonPayload(buf.readInt(), buf.readUUID(), buf.readInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
