package cc.nilm.mcmod.ae2mon;

import appeng.api.ids.AEItemIds;
import appeng.api.parts.PartModels;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.AEKeyTypes;
import appeng.api.storage.StorageCells;
import appeng.api.upgrades.Upgrades;
import cc.nilm.mcmod.ae2mon.client.ClientSetup;
import cc.nilm.mcmod.ae2mon.common.cell.PokemonCellHandler;
import cc.nilm.mcmod.ae2mon.common.key.PokemonKeyType;
import cc.nilm.mcmod.ae2mon.common.network.ModPayloads;
import cc.nilm.mcmod.ae2mon.common.part.PokemonTerminalPart;
import cc.nilm.mcmod.ae2mon.common.registry.ModItems;
import cc.nilm.mcmod.ae2mon.common.registry.ModMenuTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(CobblemonAE2.MOD_ID)
public class CobblemonAE2 {

    public static final String MOD_ID = "ae2mon";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public CobblemonAE2(IEventBus modBus) {
        modBus.addListener(this::onCommonSetup);
        modBus.addListener(this::onRegister);

        PartModels.registerModels(
                PokemonTerminalPart.MODEL_OFF,
                PokemonTerminalPart.MODEL_ON,
                PokemonTerminalPart.MODEL_HAS_CHANNEL
        );

        ModItems.register(modBus);
        ModMenuTypes.register(modBus);
        ModPayloads.register(modBus);

        if (FMLEnvironment.dist.isClient()) {
            modBus.addListener(ClientSetup::onClientSetup);
            modBus.addListener(ClientSetup::onRegisterMenuScreens);
        }
    }

    private void onRegister(RegisterEvent event) {
        if (event.getRegistryKey().equals(AEKeyType.REGISTRY_KEY)) {
            AEKeyTypes.register(PokemonKeyType.INSTANCE);
            LOGGER.info("Registered PokemonKeyType into ae2:keytypes");
        }
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Cobblemon AE2 initialized!");
        event.enqueueWork(() -> {
            StorageCells.addCellHandler(PokemonCellHandler.INSTANCE);

            var fuzzyCard = BuiltInRegistries.ITEM.get(AEItemIds.FUZZY_CARD);
            var inverterCard = BuiltInRegistries.ITEM.get(AEItemIds.INVERTER_CARD);
            Upgrades.add(fuzzyCard, ModItems.POKEMON_CELL.get(), 1);
            Upgrades.add(inverterCard, ModItems.POKEMON_CELL.get(), 1);
            for (var entry : ModItems.POKEMON_TYPE_CARDS.entrySet()) {
                var item = entry.getValue().get();
                ModItems.ITEM_TO_TYPE.put(item, entry.getKey());
                Upgrades.add(item, ModItems.POKEMON_CELL.get(), 1);
            }
        });
    }
}
