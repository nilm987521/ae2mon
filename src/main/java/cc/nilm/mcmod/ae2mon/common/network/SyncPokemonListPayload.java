package cc.nilm.mcmod.ae2mon.common.network;

import cc.nilm.mcmod.ae2mon.CobblemonAE2;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record SyncPokemonListPayload(int containerId, List<PokemonEntry> entries, List<PartyEntry> partyEntries, boolean powered) implements CustomPacketPayload {

    public static final Type<SyncPokemonListPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CobblemonAE2.MOD_ID, "sync_pokemon_list"));

    public record PokemonEntry(UUID uuid, String species, int level, String nature, String gender,
                               String ability, int ivHp, int ivAtk, int ivDef, int ivSpA, int ivSpD, int ivSpe,
                               String type1, String type2, String heldItem,
                               int evHp, int evAtk, int evDef, int evSpA, int evSpD, int evSpe) {
        public static final StreamCodec<FriendlyByteBuf, PokemonEntry> STREAM_CODEC = StreamCodec.of(
                (buf, entry) -> {
                    buf.writeUUID(entry.uuid());
                    buf.writeUtf(entry.species());
                    buf.writeInt(entry.level());
                    buf.writeUtf(entry.nature());
                    buf.writeUtf(entry.gender());
                    buf.writeUtf(entry.ability());
                    buf.writeInt(entry.ivHp());
                    buf.writeInt(entry.ivAtk());
                    buf.writeInt(entry.ivDef());
                    buf.writeInt(entry.ivSpA());
                    buf.writeInt(entry.ivSpD());
                    buf.writeInt(entry.ivSpe());
                    buf.writeUtf(entry.type1());
                    buf.writeUtf(entry.type2());
                    buf.writeUtf(entry.heldItem());
                    buf.writeInt(entry.evHp());
                    buf.writeInt(entry.evAtk());
                    buf.writeInt(entry.evDef());
                    buf.writeInt(entry.evSpA());
                    buf.writeInt(entry.evSpD());
                    buf.writeInt(entry.evSpe());
                },
                buf -> new PokemonEntry(
                        buf.readUUID(),
                        buf.readUtf(),
                        buf.readInt(),
                        buf.readUtf(),
                        buf.readUtf(),
                        buf.readUtf(),
                        buf.readInt(), buf.readInt(), buf.readInt(),
                        buf.readInt(), buf.readInt(), buf.readInt(),
                        buf.readUtf(), buf.readUtf(), buf.readUtf(),
                        buf.readInt(), buf.readInt(), buf.readInt(),
                        buf.readInt(), buf.readInt(), buf.readInt()
                )
        );
    }

    public record PartyEntry(int slot, String species, int level, String nature, String gender,
                             String ability, int ivHp, int ivAtk, int ivDef, int ivSpA, int ivSpD, int ivSpe,
                             String type1, String type2, String heldItem,
                             int evHp, int evAtk, int evDef, int evSpA, int evSpD, int evSpe) {
        public static final StreamCodec<FriendlyByteBuf, PartyEntry> STREAM_CODEC = StreamCodec.of(
                (buf, entry) -> {
                    buf.writeInt(entry.slot());
                    buf.writeUtf(entry.species());
                    buf.writeInt(entry.level());
                    buf.writeUtf(entry.nature());
                    buf.writeUtf(entry.gender());
                    buf.writeUtf(entry.ability());
                    buf.writeInt(entry.ivHp());
                    buf.writeInt(entry.ivAtk());
                    buf.writeInt(entry.ivDef());
                    buf.writeInt(entry.ivSpA());
                    buf.writeInt(entry.ivSpD());
                    buf.writeInt(entry.ivSpe());
                    buf.writeUtf(entry.type1());
                    buf.writeUtf(entry.type2());
                    buf.writeUtf(entry.heldItem());
                    buf.writeInt(entry.evHp());
                    buf.writeInt(entry.evAtk());
                    buf.writeInt(entry.evDef());
                    buf.writeInt(entry.evSpA());
                    buf.writeInt(entry.evSpD());
                    buf.writeInt(entry.evSpe());
                },
                buf -> new PartyEntry(
                        buf.readInt(),
                        buf.readUtf(), buf.readInt(),
                        buf.readUtf(), buf.readUtf(), buf.readUtf(),
                        buf.readInt(), buf.readInt(), buf.readInt(),
                        buf.readInt(), buf.readInt(), buf.readInt(),
                        buf.readUtf(), buf.readUtf(), buf.readUtf(),
                        buf.readInt(), buf.readInt(), buf.readInt(),
                        buf.readInt(), buf.readInt(), buf.readInt()
                )
        );
    }

    public static final StreamCodec<FriendlyByteBuf, SyncPokemonListPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeInt(payload.containerId());
                buf.writeBoolean(payload.powered());
                buf.writeInt(payload.entries().size());
                for (PokemonEntry entry : payload.entries()) {
                    PokemonEntry.STREAM_CODEC.encode(buf, entry);
                }
                buf.writeInt(payload.partyEntries().size());
                for (PartyEntry entry : payload.partyEntries()) {
                    PartyEntry.STREAM_CODEC.encode(buf, entry);
                }
            },
            buf -> {
                int containerId = buf.readInt();
                boolean powered = buf.readBoolean();
                int size = buf.readInt();
                List<PokemonEntry> entries = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    entries.add(PokemonEntry.STREAM_CODEC.decode(buf));
                }
                int partySize = buf.readInt();
                List<PartyEntry> partyEntries = new ArrayList<>(partySize);
                for (int i = 0; i < partySize; i++) {
                    partyEntries.add(PartyEntry.STREAM_CODEC.decode(buf));
                }
                return new SyncPokemonListPayload(containerId, entries, partyEntries, powered);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
