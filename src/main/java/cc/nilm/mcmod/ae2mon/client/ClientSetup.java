package cc.nilm.mcmod.ae2mon.client;

import appeng.api.client.AEKeyRendering;
import cc.nilm.mcmod.ae2mon.client.screen.PokemonTerminalScreen;
import cc.nilm.mcmod.ae2mon.common.key.PokemonKey;
import cc.nilm.mcmod.ae2mon.common.key.PokemonKeyType;
import cc.nilm.mcmod.ae2mon.common.registry.ModMenuTypes;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public class ClientSetup {

    public static void onClientSetup(FMLClientSetupEvent event) {
        AEKeyRendering.register(PokemonKeyType.INSTANCE, PokemonKey.class, new PokemonKeyRenderHandler());
    }

    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.POKEMON_TERMINAL_PART.get(), PokemonTerminalScreen::new);
    }
}
