package cc.nilm.mcmod.ae2mon.common.menu;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import cc.nilm.mcmod.ae2mon.CobblemonAE2;
import cc.nilm.mcmod.ae2mon.common.key.PokemonKey;
import cc.nilm.mcmod.ae2mon.common.registry.ModMenuTypes;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PokemonTerminalMenu extends AbstractContainerMenu {

    @Nullable
    private final IPokemonTerminalHost host;

    private List<PokemonKey> availableKeys = new ArrayList<>();

    // Client-side factory called by IMenuTypeExtension
    public static PokemonTerminalMenu forPart(int containerId, Inventory inventory, FriendlyByteBuf buf) {
        return new PokemonTerminalMenu(ModMenuTypes.POKEMON_TERMINAL_PART.get(), containerId, inventory, null);
    }

    public PokemonTerminalMenu(MenuType<?> menuType, int containerId, Inventory playerInventory, @Nullable IPokemonTerminalHost host) {
        super(menuType, containerId);
        this.host = host;
        if (host != null) {
            refreshAvailableKeys();
        }
    }

    private void refreshAvailableKeys() {
        if (host == null) return;
        MEStorage storage = host.getNetworkStorage();
        if (storage == null) {
            availableKeys = new ArrayList<>();
            return;
        }
        availableKeys = new ArrayList<>();
        KeyCounter counter = new KeyCounter();
        storage.getAvailableStacks(counter);
        for (var entry : counter) {
            AEKey key = entry.getKey();
            if (key instanceof PokemonKey pokemonKey) {
                availableKeys.add(pokemonKey);
            }
        }
    }

    public List<PokemonKey> getAvailableKeys() {
        return availableKeys;
    }

    @Nullable
    public IPokemonTerminalHost getHost() {
        return host;
    }

    public boolean withdrawPokemon(UUID uuid, ServerPlayer player) {
        CobblemonAE2.LOGGER.info("withdrawPokemon: player={} uuid={}", player.getName().getString(), uuid);
        if (host == null) {
            CobblemonAE2.LOGGER.warn("withdrawPokemon: host is null");
            return false;
        }
        MEStorage storage = host.getNetworkStorage();
        if (storage == null) {
            CobblemonAE2.LOGGER.warn("withdrawPokemon: network storage unavailable");
            return false;
        }

        IActionSource source = IActionSource.ofMachine(host);
        KeyCounter counter = new KeyCounter();
        storage.getAvailableStacks(counter);

        for (var entry : counter) {
            if (entry.getKey() instanceof PokemonKey pokemonKey && pokemonKey.getUuid().equals(uuid)) {
                var data = pokemonKey.getData();
                String species = data.getString("Species");
                int level = data.getInt("Level");
                CobblemonAE2.LOGGER.info("withdrawPokemon: found {} Lv.{} uuid={}", species, level, uuid);

                long extracted = storage.extract(pokemonKey, 1, Actionable.SIMULATE, source);
                if (extracted <= 0) {
                    CobblemonAE2.LOGGER.warn("withdrawPokemon: SIMULATE extract returned 0 for uuid={}", uuid);
                    return false;
                }

                storage.extract(pokemonKey, 1, Actionable.MODULATE, source);

                try {
                    Pokemon pokemon = Pokemon.Companion.loadFromNBT(player.registryAccess(), pokemonKey.getData().copy());
                    PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
                    party.add(pokemon);
                    refreshAvailableKeys();
                    CobblemonAE2.LOGGER.info("withdrawPokemon: success -> player={}", player.getName().getString());
                    return true;
                } catch (Exception e) {
                    CobblemonAE2.LOGGER.error("withdrawPokemon: exception loading pokemon uuid={}, rolling back", uuid, e);
                    storage.insert(pokemonKey, 1, Actionable.MODULATE, source);
                    return false;
                }
            }
        }
        CobblemonAE2.LOGGER.warn("withdrawPokemon: pokemon not found in network for uuid={}", uuid);
        return false;
    }

    public boolean depositPokemon(int partySlot, ServerPlayer player) {
        CobblemonAE2.LOGGER.info("depositPokemon: player={} partySlot={}", player.getName().getString(), partySlot);
        if (host == null) {
            CobblemonAE2.LOGGER.warn("depositPokemon: host is null");
            return false;
        }
        MEStorage storage = host.getNetworkStorage();
        if (storage == null) {
            CobblemonAE2.LOGGER.warn("depositPokemon: network storage unavailable");
            return false;
        }

        IActionSource source = IActionSource.ofMachine(host);

        try {
            PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
            Pokemon pokemon = party.get(partySlot);
            if (pokemon == null) {
                CobblemonAE2.LOGGER.warn("depositPokemon: no pokemon at party slot {}", partySlot);
                return false;
            }

            String species = pokemon.getSpecies().getName();
            int level = pokemon.getLevel();
            UUID uuid = pokemon.getUuid();
            CobblemonAE2.LOGGER.info("depositPokemon: {} Lv.{} uuid={}", species, level, uuid);

            PokemonKey key = PokemonKey.of(player, pokemon);

            long inserted = storage.insert(key, 1, Actionable.SIMULATE, source);
            if (inserted <= 0) {
                CobblemonAE2.LOGGER.warn("depositPokemon: SIMULATE insert returned 0 (cell full or no Pokemon Cell in network)");
                return false;
            }

            party.remove(pokemon);
            storage.insert(key, 1, Actionable.MODULATE, source);

            refreshAvailableKeys();
            CobblemonAE2.LOGGER.info("depositPokemon: success {} uuid={} -> network", species, uuid);
            return true;
        } catch (Exception e) {
            CobblemonAE2.LOGGER.error("depositPokemon: exception", e);
            return false;
        }
    }

    public boolean swapPokemon(UUID networkUUID, int partySlot, ServerPlayer player) {
        CobblemonAE2.LOGGER.info("swapPokemon: player={} networkUUID={} partySlot={}", player.getName().getString(), networkUUID, partySlot);
        if (host == null) {
            CobblemonAE2.LOGGER.warn("swapPokemon: host is null");
            return false;
        }
        MEStorage storage = host.getNetworkStorage();
        if (storage == null) {
            CobblemonAE2.LOGGER.warn("swapPokemon: network storage unavailable");
            return false;
        }

        IActionSource source = IActionSource.ofMachine(host);

        try {
            PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);

            // Fallback: target slot is empty — just do a normal withdraw
            Pokemon evicted = party.get(partySlot);
            if (evicted == null) {
                CobblemonAE2.LOGGER.info("swapPokemon: party slot {} is empty, falling back to withdraw", partySlot);
                return withdrawPokemon(networkUUID, player);
            }

            // Find the network key for networkUUID
            KeyCounter counter = new KeyCounter();
            storage.getAvailableStacks(counter);
            PokemonKey networkKey = null;
            for (var entry : counter) {
                if (entry.getKey() instanceof PokemonKey pk && pk.getUuid().equals(networkUUID)) {
                    networkKey = pk;
                    break;
                }
            }
            if (networkKey == null) {
                CobblemonAE2.LOGGER.warn("swapPokemon: network pokemon not found uuid={}", networkUUID);
                return false;
            }

            // SIMULATE extract network pokemon
            long canExtract = storage.extract(networkKey, 1, Actionable.SIMULATE, source);
            if (canExtract <= 0) {
                CobblemonAE2.LOGGER.warn("swapPokemon: SIMULATE extract returned 0 for uuid={}", networkUUID);
                return false;
            }

            // Execute swap
            PokemonKey evictedKey = PokemonKey.of(player, evicted);
            String evictedSpecies = evicted.getSpecies().getName();
            String networkSpecies = networkKey.getData().getString("Species");
            CobblemonAE2.LOGGER.info("swapPokemon: swapping party {} with network {}", evictedSpecies, networkSpecies);

            // 1. Remove evicted from party slot
            party.remove(evicted);

            // 2. Extract network pokemon from storage
            storage.extract(networkKey, 1, Actionable.MODULATE, source);

            // 3. Place network pokemon into the vacated party slot
            Pokemon networkPokemon = Pokemon.Companion.loadFromNBT(player.registryAccess(), networkKey.getData().copy());
            party.set(partySlot, networkPokemon);

            // 4. Insert evicted party pokemon into network
            long inserted = storage.insert(evictedKey, 1, Actionable.MODULATE, source);
            if (inserted <= 0) {
                // Rollback: restore both sides
                CobblemonAE2.LOGGER.error("swapPokemon: insert evicted failed (type filter?), rolling back");
                party.set(partySlot, null);
                storage.insert(networkKey, 1, Actionable.MODULATE, source);
                party.set(partySlot, evicted);
                return false;
            }

            refreshAvailableKeys();
            CobblemonAE2.LOGGER.info("swapPokemon: success — party {} <-> network {}", evictedSpecies, networkSpecies);
            return true;
        } catch (Exception e) {
            CobblemonAE2.LOGGER.error("swapPokemon: exception", e);
            return false;
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return host != null && host.isPowered();
    }
}
