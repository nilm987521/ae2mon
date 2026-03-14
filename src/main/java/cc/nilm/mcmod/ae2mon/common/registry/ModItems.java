package cc.nilm.mcmod.ae2mon.common.registry;

import appeng.api.upgrades.Upgrades;
import cc.nilm.mcmod.ae2mon.CobblemonAE2;
import cc.nilm.mcmod.ae2mon.common.item.PortablePokemonCellItem;
import cc.nilm.mcmod.ae2mon.common.item.PokemonCellItem;
import cc.nilm.mcmod.ae2mon.common.item.PokemonTerminalPartItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ModItems {

    public static final List<String> POKEMON_TYPES = List.of(
            "normal", "fire", "water", "grass", "electric", "ice",
            "fighting", "poison", "ground", "flying", "psychic", "bug",
            "rock", "ghost", "dragon", "dark", "steel", "fairy"
    );

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(BuiltInRegistries.ITEM, CobblemonAE2.MOD_ID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(BuiltInRegistries.CREATIVE_MODE_TAB, CobblemonAE2.MOD_ID);

    public static final DeferredHolder<Item, PokemonCellItem> POKEMON_CELL =
            ITEMS.register("pokemon_cell", PokemonCellItem::new);

    public static final DeferredHolder<Item, PokemonTerminalPartItem> POKEMON_TERMINAL_PART =
            ITEMS.register("pokemon_terminal_part", PokemonTerminalPartItem::new);

    public static final DeferredHolder<Item, PortablePokemonCellItem> PORTABLE_POKEMON_CELL =
            ITEMS.register("portable_pokemon_cell", PortablePokemonCellItem::new);

    /** Registered type filter cards keyed by type name. */
    public static final Map<String, DeferredHolder<Item, Item>> POKEMON_TYPE_CARDS = new LinkedHashMap<>();

    /** Populated during enqueueWork — maps registered Item instance → type name. */
    public static final Map<Item, String> ITEM_TO_TYPE = new HashMap<>();

    static {
        for (String type : POKEMON_TYPES) {
            POKEMON_TYPE_CARDS.put(type,
                    ITEMS.register("pokemon_type_card_" + type,
                            () -> Upgrades.createUpgradeCardItem(new Item.Properties().stacksTo(1))));
        }
    }

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> AE2MON_TAB =
            CREATIVE_TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.ae2mon"))
                    .icon(() -> POKEMON_CELL.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        output.accept(POKEMON_CELL.get());
                        output.accept(PORTABLE_POKEMON_CELL.get());
                        output.accept(POKEMON_TERMINAL_PART.get());
                        POKEMON_TYPE_CARDS.values().forEach(h -> output.accept(h.get()));
                    })
                    .build());

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        CREATIVE_TABS.register(modBus);
    }
}
