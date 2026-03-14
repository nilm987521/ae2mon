package cc.nilm.mcmod.ae2mon.common.registry;

import cc.nilm.mcmod.ae2mon.CobblemonAE2;
import cc.nilm.mcmod.ae2mon.common.menu.PortableCellTerminalMenu;
import cc.nilm.mcmod.ae2mon.common.menu.PokemonTerminalMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(BuiltInRegistries.MENU, CobblemonAE2.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<PokemonTerminalMenu>> POKEMON_TERMINAL_PART =
            MENU_TYPES.register("pokemon_terminal_part",
                    () -> IMenuTypeExtension.create(PokemonTerminalMenu::forPart));

    public static final DeferredHolder<MenuType<?>, MenuType<PortableCellTerminalMenu>> PORTABLE_POKEMON_CELL =
            MENU_TYPES.register("portable_pokemon_cell",
                    () -> IMenuTypeExtension.create(PortableCellTerminalMenu::fromNetwork));

    public static void register(IEventBus modBus) {
        MENU_TYPES.register(modBus);
    }
}
