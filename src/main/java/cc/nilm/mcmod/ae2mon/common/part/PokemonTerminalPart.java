package cc.nilm.mcmod.ae2mon.common.part;

import appeng.api.networking.GridFlags;
import appeng.api.networking.storage.IStorageService;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.storage.MEStorage;
import appeng.parts.AEBasePart;
import appeng.parts.PartModel;
import cc.nilm.mcmod.ae2mon.CobblemonAE2;
import cc.nilm.mcmod.ae2mon.common.menu.IPokemonTerminalHost;
import cc.nilm.mcmod.ae2mon.common.menu.PokemonTerminalMenu;
import cc.nilm.mcmod.ae2mon.common.network.ModPayloads;
import cc.nilm.mcmod.ae2mon.common.registry.ModMenuTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class PokemonTerminalPart extends AEBasePart implements IPokemonTerminalHost {

    private static final ResourceLocation MODEL_BASE =
            ResourceLocation.fromNamespaceAndPath("ae2", "part/display_base");
    private static final ResourceLocation MODEL_STATUS_OFF =
            ResourceLocation.fromNamespaceAndPath("ae2", "part/display_status_off");
    private static final ResourceLocation MODEL_STATUS_ON =
            ResourceLocation.fromNamespaceAndPath("ae2", "part/display_status_on");
    private static final ResourceLocation MODEL_STATUS_HAS_CHANNEL =
            ResourceLocation.fromNamespaceAndPath("ae2", "part/display_status_has_channel");

    public static final ResourceLocation MODEL_OFF =
            ResourceLocation.fromNamespaceAndPath(CobblemonAE2.MOD_ID, "part/pokemon_terminal_off");
    public static final ResourceLocation MODEL_ON =
            ResourceLocation.fromNamespaceAndPath(CobblemonAE2.MOD_ID, "part/pokemon_terminal_on");
    public static final ResourceLocation MODEL_HAS_CHANNEL =
            ResourceLocation.fromNamespaceAndPath(CobblemonAE2.MOD_ID, "part/pokemon_terminal_has_channel");

    public static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE, MODEL_OFF, MODEL_STATUS_OFF);
    public static final IPartModel MODELS_ON = new PartModel(MODEL_BASE, MODEL_ON, MODEL_STATUS_ON);
    public static final IPartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE, MODEL_ON, MODEL_STATUS_HAS_CHANNEL);

    public PokemonTerminalPart(IPartItem<?> partItem) {
        super(partItem);
        getMainNode()
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .setIdlePowerUsage(0.5);
    }

    @Override
    @Nullable
    public MEStorage getNetworkStorage() {
        var grid = getMainNode().getGrid();
        if (grid == null) return null;
        return grid.getService(IStorageService.class).getInventory();
    }

    @Override
    public boolean onUseWithoutItem(Player player, Vec3 pos) {
        if (!player.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, p) -> new PokemonTerminalMenu(
                            ModMenuTypes.POKEMON_TERMINAL_PART.get(), containerId, inventory, this),
                    Component.translatable("item.ae2mon.pokemon_terminal_part")));
            if (serverPlayer.containerMenu instanceof PokemonTerminalMenu menu) {
                ModPayloads.sendSyncToPlayer(menu, serverPlayer);
            }
        }
        return true;
    }

    @Override
    public void getBoxes(IPartCollisionHelper bch) {
        bch.addBox(2, 2, 14, 14, 14, 16);
        bch.addBox(4, 4, 13, 12, 12, 14);
    }

    @Override
    public IPartModel getStaticModels() {
        if (isActive()) return MODELS_HAS_CHANNEL;
        if (isPowered()) return MODELS_ON;
        return MODELS_OFF;
    }
}
