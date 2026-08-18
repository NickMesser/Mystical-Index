package net.messer.mystical_index.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class LibraryInventoryScreen extends AbstractContainerScreen<LibraryInventoryScreenHandler> {
    private static final Identifier TEXTURE = Identifier.parse("mystical_index:textures/gui/library_screen.png");

    private static final int WIDTH = 176;
    private static final int HEIGHT = 133;

    // Size of the PNG file, not of the panel. blit's last two arguments are what the UVs are
    // divided by, so they have to describe the image on disk; passing the panel size instead makes
    // the whole canvas - including the empty area past the artwork - get squashed into the panel.
    private static final int TEXTURE_SIZE = 256;

    public LibraryInventoryScreen(LibraryInventoryScreenHandler menu, Inventory inventory, Component title) {
        // imageWidth/imageHeight are final now and can only be set through the constructor.
        super(menu, inventory, title, WIDTH, HEIGHT);
        this.inventoryLabelY = HEIGHT - 94;
    }

    // Screens no longer expose a background hook of their own; the panel is emitted as part of the
    // contents pass, before super draws the slots over it. The pipeline argument replaces the
    // manual shader and texture binding this used to need.
    @Override
    public void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0.0F, 0.0F,
                WIDTH, HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);
        super.extractContents(context, mouseX, mouseY, delta);
    }
}
