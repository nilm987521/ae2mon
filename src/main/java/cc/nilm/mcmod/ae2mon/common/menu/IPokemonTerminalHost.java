package cc.nilm.mcmod.ae2mon.common.menu;

import appeng.api.networking.security.IActionHost;
import appeng.api.storage.MEStorage;
import org.jetbrains.annotations.Nullable;

public interface IPokemonTerminalHost extends IActionHost {
    @Nullable
    MEStorage getNetworkStorage();

    boolean isPowered();
}
