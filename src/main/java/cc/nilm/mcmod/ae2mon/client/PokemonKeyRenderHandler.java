package cc.nilm.mcmod.ae2mon.client;

import appeng.api.client.AEKeyRenderHandler;
import cc.nilm.mcmod.ae2mon.common.key.PokemonKey;
import com.cobblemon.mod.common.api.pokeball.PokeBalls;
import com.cobblemon.mod.common.pokeball.PokeBall;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PokemonKeyRenderHandler implements AEKeyRenderHandler<PokemonKey> {

    private ItemStack getBallStack(PokemonKey stack) {
        String caughtBall = stack.getData().getString("CaughtBall");
        if (!caughtBall.isEmpty()) {
            ResourceLocation ballId = ResourceLocation.tryParse(caughtBall);
            if (ballId != null) {
                PokeBall ball = PokeBalls.INSTANCE.getPokeBall(ballId);
                if (ball != null) {
                    return ball.stack(1);
                }
            }
        }
        return new ItemStack(Items.ENDER_PEARL);
    }

    @Override
    public void drawInGui(Minecraft minecraft, GuiGraphics guiGraphics, int x, int y, PokemonKey stack) {
        guiGraphics.renderItem(getBallStack(stack), x, y);
    }

    @Override
    public void drawOnBlockFace(PoseStack poseStack, MultiBufferSource buffers, PokemonKey what,
                                float scale, int combinedLight, Level level) {
        // Not rendered on block faces
    }

    @Override
    public Component getDisplayName(PokemonKey stack) {
        return stack.getDisplayName();
    }
}
