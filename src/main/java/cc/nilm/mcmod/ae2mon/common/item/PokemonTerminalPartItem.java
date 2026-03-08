package cc.nilm.mcmod.ae2mon.common.item;

import appeng.api.parts.IPartItem;
import appeng.api.parts.PartHelper;
import cc.nilm.mcmod.ae2mon.common.part.PokemonTerminalPart;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public class PokemonTerminalPartItem extends Item implements IPartItem<PokemonTerminalPart> {

    public PokemonTerminalPartItem() {
        super(new Item.Properties().stacksTo(64));
    }

    @Override
    public Class<PokemonTerminalPart> getPartClass() {
        return PokemonTerminalPart.class;
    }

    @Override
    public PokemonTerminalPart createPart() {
        return new PokemonTerminalPart(this);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        return PartHelper.usePartItem(ctx);
    }
}
