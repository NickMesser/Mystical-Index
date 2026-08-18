package net.messer.mystical_index.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ExperienceScreen extends AbstractContainerScreen<ExperienceScreenHandler> {

    private static final Identifier TEXTURE =
            Identifier.parse("mystical_index:textures/gui/experience_gui.png");

    private static final int WIDTH = 176;
    private static final int HEIGHT = 166;

    // Size of the PNG file, not of the panel: blit divides the UVs by these, so they describe the
    // image on disk. Passing the panel size squashes the whole canvas into the panel.
    private static final int TEXTURE_SIZE = 256;

    // Readout lines, panel-local.
    private static final int TEXT_X = 8;
    private static final int STORED_Y = 22;
    private static final int POINTS_Y = 34;

    // Two small transfer buttons on the readout row, then the wide mode switch beneath.
    private static final int SMALL_W = 20;
    private static final int SMALL_H = 20;
    private static final int WITHDRAW_X = 100;
    private static final int DEPOSIT_X = 124;
    private static final int SMALL_Y = 18;
    private static final int MODE_X = 8;
    private static final int MODE_Y = 46;
    private static final int MODE_W = 160;
    private static final int MODE_H = 20;

    private Button depositButton;
    private Button withdrawButton;
    private Button modeButton;

    public ExperienceScreen(ExperienceScreenHandler menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, HEIGHT);
        // Not the usual HEIGHT-94 (=72): that would land inside the mode button, which ends at 66.
        this.inventoryLabelY = 74;
    }

    @Override
    protected void init() {
        super.init();

        withdrawButton = Button.builder(Component.literal("-"), b -> press(ExperienceScreenHandler.BUTTON_WITHDRAW))
                .bounds(leftPos + WITHDRAW_X, topPos + SMALL_Y, SMALL_W, SMALL_H).build();
        depositButton = Button.builder(Component.literal("+"), b -> press(ExperienceScreenHandler.BUTTON_DEPOSIT))
                .bounds(leftPos + DEPOSIT_X, topPos + SMALL_Y, SMALL_W, SMALL_H).build();
        modeButton = Button.builder(modeLabel(), b -> press(ExperienceScreenHandler.BUTTON_AUTO))
                .bounds(leftPos + MODE_X, topPos + MODE_Y, MODE_W, MODE_H).build();

        addRenderableWidget(withdrawButton);
        addRenderableWidget(depositButton);
        addRenderableWidget(modeButton);
    }

    private void press(int id) {
        var gameMode = Minecraft.getInstance().gameMode;
        if (gameMode != null)
            gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    private Component modeLabel() {
        var data = menu.data();
        boolean on = data != null && data.autoCollect();
        return Component.translatable("gui.mystical_index.experience.auto",
                Component.translatable(on ? "tooltip.mystical_index.experience_book.auto_on"
                                          : "tooltip.mystical_index.experience_book.auto_off"));
    }

    @Override
    public void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0.0F, 0.0F,
                WIDTH, HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);

        // Faces and enabled-state refresh from the LIVE stack every frame: auto-collect can flip
        // the stored total while this is open, and the mode can change from elsewhere.
        var data = menu.data();
        var player = Minecraft.getInstance().player;

        if (modeButton != null)
            modeButton.setMessage(modeLabel());

        if (depositButton != null)
            depositButton.active = player != null && (player.experienceLevel > 0 || player.experienceProgress > 0.0F);

        if (withdrawButton != null)
            withdrawButton.active = data != null && !data.isEmpty();

        super.extractContents(context, mouseX, mouseY, delta);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        super.extractLabels(context, mouseX, mouseY);

        var data = menu.data();
        long points = data == null ? 0L : data.points();
        int levels = data == null ? 0 : data.storedLevels();

        // Same green as the tooltip's stored line, and the same shadowless style as the vanilla
        // "Inventory" label beside it.
        context.text(this.font, Component.translatable("gui.mystical_index.experience.stored", levels),
                TEXT_X, STORED_Y, 0xFF3BA55D, false);
        context.text(this.font, Component.translatable("gui.mystical_index.experience.points", points),
                TEXT_X, POINTS_Y, 0xFF808080, false);
    }
}
