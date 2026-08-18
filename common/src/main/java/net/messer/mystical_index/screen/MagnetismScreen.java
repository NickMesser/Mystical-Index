package net.messer.mystical_index.screen;

import net.messer.mystical_index.item.inventory.MagnetFilterData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class MagnetismScreen extends AbstractContainerScreen<MagnetismScreenHandler> {

    private static final Identifier TEXTURE =
            Identifier.parse("mystical_index:textures/gui/magnetism_gui.png");

    private static final int WIDTH = 176;
    private static final int HEIGHT = 166;

    // Size of the PNG file, not of the panel: blit divides the UVs by these, so they describe the
    // image on disk. Passing the panel size squashes the whole canvas into the panel.
    private static final int TEXTURE_SIZE = 256;

    // A real vanilla Button: it brings its own raised bevel, hover highlight and pressed state, so
    // it reads as something to click rather than as another slot. Wide enough for the longest
    // label ("Mode: Blacklist") with room to spare, centred under the filter row.
    private static final int MODE_W = 100;
    private static final int MODE_H = 20;
    private static final int MODE_X = (WIDTH - MODE_W) / 2;
    private static final int MODE_Y = 45;

    private Button modeButton;

    public MagnetismScreen(MagnetismScreenHandler menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, HEIGHT);
        // Not the usual HEIGHT-94: this panel's player inventory starts at y=84, and the label has
        // to clear the mode button, which ends at y=65.
        this.inventoryLabelY = 74;
    }

    @Override
    protected void init() {
        super.init();

        modeButton = Button.builder(modeLabel(), button -> {
            // Vanilla's menu-button channel: the handler applies the change server-side through
            // the same setMode the sneak gesture uses, so no payload and no divergent bookkeeping.
            var gameMode = Minecraft.getInstance().gameMode;
            if (gameMode != null)
                gameMode.handleInventoryButtonClick(menu.containerId, MagnetismScreenHandler.MODE_BUTTON_ID);
        }).bounds(leftPos + MODE_X, topPos + MODE_Y, MODE_W, MODE_H).build();

        addRenderableWidget(modeButton);
    }

    private Component modeLabel() {
        var mode = MagnetFilterData.modeOf(menu.filter().stack);
        return Component.translatable("gui.mystical_index.magnetism.mode_label",
                Component.translatable(mode.translationKey()));
    }

    @Override
    public void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0.0F, 0.0F,
                WIDTH, HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);

        // The mode can change under an open screen (the sneak gesture still works with the book in
        // hand), so the face is refreshed from the stack rather than only on click.
        if (modeButton != null)
            modeButton.setMessage(modeLabel());

        super.extractContents(context, mouseX, mouseY, delta);
    }
}
