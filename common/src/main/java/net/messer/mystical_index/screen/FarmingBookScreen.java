package net.messer.mystical_index.screen;

import net.messer.mystical_index.item.provider.FarmingGrowthModel;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class FarmingBookScreen extends AbstractContainerScreen<FarmingBookScreenHandler> {

    private static final Identifier TEXTURE =
            Identifier.parse("mystical_index:textures/gui/farming_book_gui.png");

    private static final int WIDTH = 176;
    private static final int HEIGHT = 133;

    // Size of the PNG file, not of the panel. blit's last two arguments are what the UVs are
    // divided by, so they have to describe the image on disk; passing the panel size instead makes
    // the whole canvas - including the empty area past the artwork - get squashed into the panel.
    private static final int TEXTURE_SIZE = 256;

    public FarmingBookScreen(FarmingBookScreenHandler menu, Inventory inventory, Component title) {
        // imageWidth/imageHeight are final now and can only be set through the constructor.
        super(menu, inventory, title, WIDTH, HEIGHT);
        this.inventoryLabelY = HEIGHT - 94;
    }

    // Screens no longer expose a background hook of their own; the panel is emitted as part of the
    // contents pass, before super draws the slots over it.
    // Furnace-idiom progress arrow, sitting between the two input slots and the outputs.
    // X and width are not eyeballed: the seed slot's 18x18 border ends at x=69 and the first
    // output's begins at x=97, so the free gap is [69,97) and this is that gap inset by 2px
    // on each side. Changing either slot's coordinates changes what belongs here.
    private static final int ARROW_X = 71;
    private static final int ARROW_Y = 20;
    private static final int ARROW_W = 24;
    private static final int ARROW_H = 17;
    private static final int ARROW_EMPTY_V = 140;
    private static final int ARROW_FULL_V = 160;

    @Override
    public void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0.0F, 0.0F,
                WIDTH, HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);

        // Drawn after the panel and before super, so the slots still land on top.
        drawProgressArrow(context);

        super.extractContents(context, mouseX, mouseY, delta);
    }

    /**
     * Fills the arrow left to right with the book's growth.
     *
     * <p>Read straight off the held stack every frame rather than synced through the menu: the
     * growth number already rides along in the stack's components, so the client has it for free
     * and no ContainerData is needed. The stored clock only advances once a second, so the fill
     * steps rather than glides - which is honest about what the book is actually doing.
     */
    private void drawProgressArrow(GuiGraphicsExtractor context) {
        var book = FarmingBookScreenHandler.heldBook(this.minecraft.player);
        float progress = book.isEmpty() ? 0.0F : FarmingGrowthModel.progressOf(book);

        context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                leftPos + ARROW_X, topPos + ARROW_Y, 0.0F, (float) ARROW_EMPTY_V,
                ARROW_W, ARROW_H, TEXTURE_SIZE, TEXTURE_SIZE);

        int filled = Math.round(ARROW_W * Math.max(0.0F, Math.min(1.0F, progress)));
        if (filled <= 0)
            return;

        context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                leftPos + ARROW_X, topPos + ARROW_Y, 0.0F, (float) ARROW_FULL_V,
                filled, ARROW_H, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    // No extra labels: the growth readout lives in the item tooltip, where it is visible without
    // opening the book at all. The inherited pass already draws the title and the inventory label.
}
