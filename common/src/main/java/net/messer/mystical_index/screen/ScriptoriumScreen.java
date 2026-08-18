package net.messer.mystical_index.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

public class ScriptoriumScreen extends AbstractContainerScreen<ScriptoriumScreenHandler> {

    private static final Identifier TEXTURE =
            Identifier.parse("mystical_index:textures/gui/scriptorium_gui.png");

    private static final int WIDTH = 176;
    private static final int HEIGHT = 133;

    // Size of the PNG file, not of the panel: blit divides the UVs by these, so they describe the
    // image on disk. Passing the panel size squashes the whole canvas into the panel.
    private static final int TEXTURE_SIZE = 256;

    public ScriptoriumScreen(ScriptoriumScreenHandler menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, HEIGHT);
        this.inventoryLabelY = HEIGHT - 94;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0.0F, 0.0F,
                WIDTH, HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);
        super.extractContents(context, mouseX, mouseY, delta);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        super.extractLabels(context, mouseX, mouseY);

        // Hint on an empty book slot, so the acceptance rule is discoverable without trial and
        // error. Uses the inherited hoveredSlot rather than custom slot rendering.
        if (hoveredSlot == null || hoveredSlot.index >= ScriptoriumScreenHandler.SIZE || hoveredSlot.hasItem())
            return;

        context.setTooltipForNextFrame(font,
                List.of(Component.translatable("gui.mystical_index.scriptorium.slot_hint")),
                Optional.empty(), mouseX, mouseY);
    }
}
