package cc.nilm.mcmod.ae2mon.common.network;

import cc.nilm.mcmod.ae2mon.CobblemonAE2;
import cc.nilm.mcmod.ae2mon.common.key.PokemonKey;
import cc.nilm.mcmod.ae2mon.common.menu.PokemonTerminalMenu;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.Comparator;

public class ModPayloads {

    public static void register(IEventBus modBus) {
        modBus.addListener(ModPayloads::onRegisterPayloads);
    }

    private static int getDexNumber(PokemonKey key) {
        String speciesId = key.getData().getString("Species");
        ResourceLocation loc = ResourceLocation.tryParse(speciesId);
        if (loc == null) return Integer.MAX_VALUE;
        var species = PokemonSpecies.INSTANCE.getByIdentifier(loc);
        return species != null ? species.getNationalPokedexNumber() : Integer.MAX_VALUE;
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(CobblemonAE2.MOD_ID);

        registrar.playToClient(
                SyncPokemonListPayload.TYPE,
                SyncPokemonListPayload.STREAM_CODEC,
                ModPayloads::handleSyncList
        );

        registrar.playToServer(
                WithdrawPokemonPayload.TYPE,
                WithdrawPokemonPayload.STREAM_CODEC,
                ModPayloads::handleWithdraw
        );

        registrar.playToServer(
                DepositPokemonPayload.TYPE,
                DepositPokemonPayload.STREAM_CODEC,
                ModPayloads::handleDeposit
        );

        registrar.playToServer(
                SwapPokemonPayload.TYPE,
                SwapPokemonPayload.STREAM_CODEC,
                ModPayloads::handleSwap
        );

    }

    private static void handleSyncList(SyncPokemonListPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientHandlers.handleSyncList(payload));
    }

    private static void handleWithdraw(WithdrawPokemonPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player.containerMenu instanceof PokemonTerminalMenu menu
                    && menu.containerId == payload.containerId()) {
                menu.withdrawPokemon(payload.uuid(), player);
                sendSyncToPlayer(menu, player);
            }
        });
    }

    private static void handleDeposit(DepositPokemonPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player.containerMenu instanceof PokemonTerminalMenu menu
                    && menu.containerId == payload.containerId()) {
                menu.depositPokemon(payload.partySlot(), player);
                sendSyncToPlayer(menu, player);
            }
        });
    }

    private static void handleSwap(SwapPokemonPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player.containerMenu instanceof PokemonTerminalMenu menu
                    && menu.containerId == payload.containerId()) {
                menu.swapPokemon(payload.networkUUID(), payload.partySlot(), player);
                sendSyncToPlayer(menu, player);
            }
        });
    }

    /** Extract ability name from Pokemon NBT (handles CompoundTag or plain String formats). */
    private static String extractAbility(CompoundTag data) {
        if (data.contains("Ability")) {
            var abilityNbt = data.get("Ability");
            if (abilityNbt instanceof CompoundTag abilityTag) {
                String name = abilityTag.getString("AbilityName");
                if (!name.isEmpty()) return name;
                name = abilityTag.getString("Name");
                if (!name.isEmpty()) return name;
            }
            String ability = data.getString("Ability");
            if (!ability.isEmpty()) return ability;
        }
        return "";
    }

    /** Extract primary and secondary type names from Pokemon NBT via species lookup. */
    private static String[] extractTypes(CompoundTag data) {
        String speciesId = data.getString("Species");
        ResourceLocation loc = ResourceLocation.tryParse(speciesId);
        if (loc == null) return new String[]{"", ""};
        var species = PokemonSpecies.INSTANCE.getByIdentifier(loc);
        if (species == null) return new String[]{"", ""};
        String t1 = species.getPrimaryType().getName();
        var t2type = species.getSecondaryType();
        String t2 = t2type != null ? t2type.getName() : "";
        return new String[]{t1, t2};
    }

    /** Extract types from a live Pokemon object. */
    private static String[] extractTypesFromPokemon(Pokemon pokemon) {
        String t1 = pokemon.getPrimaryType().getName();
        var t2type = pokemon.getSecondaryType();
        String t2 = t2type != null ? t2type.getName() : "";
        return new String[]{t1, t2};
    }

    /** Extract held item full registry ID ("namespace:path") from Pokemon NBT. */
    private static String extractHeldItem(CompoundTag data) {
        if (data.contains("HeldItem", Tag.TAG_COMPOUND)) {
            String id = data.getCompound("HeldItem").getString("id");
            if (!id.isEmpty() && !id.equals("minecraft:air")) return id;
        }
        if (data.contains("HeldItem", Tag.TAG_STRING)) {
            String id = data.getString("HeldItem");
            if (!id.isEmpty() && !id.equals("minecraft:air")) return id;
        }
        return "";
    }

    /** Extract held item full registry ID from a live Pokemon object. */
    private static String extractHeldItemFromPokemon(Pokemon pokemon) {
        var held = pokemon.heldItem();
        if (held.isEmpty()) return "";
        var key = BuiltInRegistries.ITEM.getKey(held.getItem());
        return key != null ? key.toString() : "";
    }

    /** Extract all 6 IVs from Pokemon NBT. Returns [hp, atk, def, spA, spD, spe]. */
    private static int[] extractIVs(CompoundTag data) {
        CompoundTag base = data.getCompound("IVs").getCompound("Base");
        return new int[]{
                base.getInt("cobblemon:hp"),
                base.getInt("cobblemon:attack"),
                base.getInt("cobblemon:defence"),
                base.getInt("cobblemon:special_attack"),
                base.getInt("cobblemon:special_defence"),
                base.getInt("cobblemon:speed")
        };
    }

    /** Extract all 6 EVs from Pokemon NBT. Returns [hp, atk, def, spA, spD, spe]. */
    private static int[] extractEVs(CompoundTag data) {
        CompoundTag base = data.getCompound("EVs").getCompound("Base");
        return new int[]{
                base.getInt("cobblemon:hp"),
                base.getInt("cobblemon:attack"),
                base.getInt("cobblemon:defence"),
                base.getInt("cobblemon:special_attack"),
                base.getInt("cobblemon:special_defence"),
                base.getInt("cobblemon:speed")
        };
    }

    public static void sendSyncToPlayer(PokemonTerminalMenu menu, ServerPlayer player) {
        var entries = menu.getAvailableKeys().stream()
                .sorted(Comparator.comparingInt(ModPayloads::getDexNumber))
                .map(key -> {
                    CompoundTag data = key.getData();
                    String species = data.getString("Species");
                    species = species.contains(":") ? species.substring(species.indexOf(':') + 1) : species;
                    int level = data.getInt("Level");
                    String nature = data.getString("Nature");
                    String gender = data.getString("Gender");
                    String ability = extractAbility(data);
                    int[] ivs = extractIVs(data);
                    String[] types = extractTypes(data);
                    String heldItem = extractHeldItem(data);
                    int[] evs = extractEVs(data);
                    boolean shiny = data.getBoolean("Shiny");
                    return new SyncPokemonListPayload.PokemonEntry(
                            key.getUuid(), species, level, nature, gender,
                            ability, ivs[0], ivs[1], ivs[2], ivs[3], ivs[4], ivs[5],
                            types[0], types[1], heldItem,
                            evs[0], evs[1], evs[2], evs[3], evs[4], evs[5], shiny);
                })
                .toList();

        var party = com.cobblemon.mod.common.Cobblemon.INSTANCE.getStorage().getParty(player);
        var partyEntries = new java.util.ArrayList<SyncPokemonListPayload.PartyEntry>();
        for (int i = 0; i < 6; i++) {
            var pokemon = party.get(i);
            if (pokemon != null) {
                // Serialize to NBT for consistent IV/ability extraction
                CompoundTag nbt = pokemon.saveToNBT(player.level().registryAccess(), new CompoundTag());
                String species = pokemon.getSpecies().getName();
                int level = pokemon.getLevel();
                String nature = nbt.getString("Nature");
                String gender = nbt.getString("Gender");
                String ability = extractAbility(nbt);
                int[] ivs = extractIVs(nbt);
                String[] types = extractTypesFromPokemon(pokemon);
                String heldItem = extractHeldItemFromPokemon(pokemon);
                int[] evs = extractEVs(nbt);
                boolean shiny = pokemon.getShiny();
                partyEntries.add(new SyncPokemonListPayload.PartyEntry(
                        i, species, level, nature, gender,
                        ability, ivs[0], ivs[1], ivs[2], ivs[3], ivs[4], ivs[5],
                        types[0], types[1], heldItem,
                        evs[0], evs[1], evs[2], evs[3], evs[4], evs[5], shiny));
            }
        }

        boolean powered = menu.getHost() != null && menu.getHost().isPowered();

        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                new SyncPokemonListPayload(menu.containerId, entries, partyEntries, powered));
    }

}
