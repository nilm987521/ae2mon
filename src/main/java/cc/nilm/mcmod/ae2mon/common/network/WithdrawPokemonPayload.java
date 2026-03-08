package cc.nilm.mcmod.ae2mon.common.network;

import cc.nilm.mcmod.ae2mon.CobblemonAE2;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record WithdrawPokemonPayload(int containerId, UUID uuid) implements CustomPacketPayload {

    public static final Type<WithdrawPokemonPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CobblemonAE2.MOD_ID, "withdraw_pokemon"));

    public static final StreamCodec<FriendlyByteBuf, WithdrawPokemonPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeInt(payload.containerId());
                buf.writeUUID(payload.uuid());
            },
            buf -> new WithdrawPokemonPayload(buf.readInt(), buf.readUUID())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
