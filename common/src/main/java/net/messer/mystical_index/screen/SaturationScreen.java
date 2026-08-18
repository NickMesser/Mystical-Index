package net.messer.mystical_index.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class SaturationScreen extends AbstractContainerScreen<SaturationScreenHandler> {

    private static final Identifier TEXTURE =
            Identifier.parse("mystical_index:textures/gui/saturation_gui.png");

    private static final int WIDTH = 176;
    private static final int HEIGHT = 133;

    // Size of the PNG file, not of the panel: blit divides the UVs by these.
    private static final int TEXTURE_SIZE = 256;

    public SaturationScreen(SaturationScreenHandler menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, HEIGHT);
        this.inventoryLabelY = HEIGHT - 94;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0.0F, 0.0F,
                WIDTH, HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);
        super.extractContents(context, mouseX, mouseY, delta);
    }
}
