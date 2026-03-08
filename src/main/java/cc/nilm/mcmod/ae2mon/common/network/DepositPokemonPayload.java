package cc.nilm.mcmod.ae2mon.common.network;

import cc.nilm.mcmod.ae2mon.CobblemonAE2;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DepositPokemonPayload(int containerId, int partySlot) implements CustomPacketPayload {

    public static final Type<DepositPokemonPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CobblemonAE2.MOD_ID, "deposit_pokemon"));

    public static final StreamCodec<FriendlyByteBuf, DepositPokemonPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeInt(payload.containerId());
                buf.writeInt(payload.partySlot());
            },
            buf -> new DepositPokemonPayload(buf.readInt(), buf.readInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
