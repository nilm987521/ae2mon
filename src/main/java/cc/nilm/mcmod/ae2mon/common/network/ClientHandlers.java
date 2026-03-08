package cc.nilm.mcmod.ae2mon.common.network;

import cc.nilm.mcmod.ae2mon.client.screen.PokemonTerminalScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientHandlers {

    @OnlyIn(Dist.CLIENT)
    public static void handleSyncList(SyncPokemonListPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof PokemonTerminalScreen screen) {
            screen.updatePokemonList(payload.entries(), payload.partyEntries(), payload.powered());
        }
    }
}
