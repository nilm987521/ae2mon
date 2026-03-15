package cc.nilm.mcmod.ae2mon.common.network;

import cc.nilm.mcmod.ae2mon.CobblemonAE2;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Sent client → server when the player clicks the Held Item widget in the Detail panel.
 * Server reads menu.getCarried() and swaps it with the selected Pokemon's held item.
 * Exactly one of networkUUID / partySlot identifies the target Pokemon.
 */
public record SetHeldItemPayload(int containerId, @Nullable UUID networkUUID, int partySlot)
        implements CustomPacketPayload {

    public static final Type<SetHeldItemPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CobblemonAE2.MOD_ID, "set_held_item"));

    public static final StreamCodec<FriendlyByteBuf, SetHeldItemPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeInt(payload.containerId());
                boolean hasUUID = payload.networkUUID() != null;
                buf.writeBoolean(hasUUID);
                if (hasUUID) buf.writeUUID(payload.networkUUID());
                buf.writeInt(payload.partySlot());
            },
            buf -> {
                int containerId = buf.readInt();
                boolean hasUUID = buf.readBoolean();
                UUID networkUUID = hasUUID ? buf.readUUID() : null;
                int partySlot = buf.readInt();
                return new SetHeldItemPayload(containerId, networkUUID, partySlot);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
