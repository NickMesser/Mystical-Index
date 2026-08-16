package net.messer.mystical_index.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.messer.mystical_index.item.inventory.LibraryNetwork;
import net.messer.mystical_index.network.LecternClientNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;

public class MysticalLecternScreen extends HandledScreen<MysticalLecternScreenHandler> {
    private static final Identifier TEXTURE = Identifier.of("mystical_index:textures/gui/lectern_gui.png");
    // 1.20.2+ split the creative tabs sheet into individual GUI sprites, so the thumb is drawn
    // from the sprite atlas instead of by uv offset into a texture that no longer exists.
    private static final Identifier SCROLLER = Identifier.of("container/creative_inventory/scroller");
    private static final Identifier SCROLLER_DISABLED = Identifier.of("container/creative_inventory/scroller_disabled");

    private static final int COLUMNS = 9;
    private static final int ROWS = 3;
    private static final int NETWORK_X = 8;
    private static final int NETWORK_Y = 18;

    private static final int SCROLLBAR_X = 172;
    private static final int SCROLLBAR_Y = 17;
    private static final int SCROLLBAR_TRAVEL = 38;

    private int scrollRow;
    private boolean scrolling;

    public MysticalLecternScreen(MysticalLecternScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 195;
        this.backgroundHeight = 221;
        this.playerInventoryTitleY = 127;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        context.drawTexture(TEXTURE, x, y, 0, 0, backgroundWidth, backgroundHeight);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        super.drawForeground(context, mouseX, mouseY);

        var entries = handler.getClientEntries();
        scrollRow = MathHelper.clamp(scrollRow, 0, maxScrollRow());

        int hoveredX = -1;
        int hoveredY = -1;

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int index = (scrollRow + row) * COLUMNS + col;
                if (index >= entries.size())
                    continue;

                var entry = entries.get(index);
                int cellX = NETWORK_X + col * 18;
                int cellY = NETWORK_Y + row * 18;

                context.drawItem(entry.variant().toStack(1), cellX, cellY);
                drawCount(context, formatCount(entry.count()), cellX, cellY);

                if (isPointWithinBounds(cellX, cellY, 16, 16, mouseX, mouseY)) {
                    hoveredX = cellX;
                    hoveredY = cellY;
                }
            }
        }

        if (hoveredX >= 0) {
            context.getMatrices().push();
            context.getMatrices().translate(0.0F, 0.0F, 200.0F);
            context.fill(hoveredX, hoveredY, hoveredX + 16, hoveredY + 16, 0x80FFFFFF);
            context.getMatrices().pop();
        }

        drawScrollbar(context);
    }

    private void drawScrollbar(DrawContext context) {
        context.fill(SCROLLBAR_X, SCROLLBAR_Y, SCROLLBAR_X + 15, SCROLLBAR_Y + 55, 0xFF373737);
        context.fill(SCROLLBAR_X + 1, SCROLLBAR_Y + 1, SCROLLBAR_X + 14, SCROLLBAR_Y + 54, 0xFF8B8B8B);

        int max = maxScrollRow();
        int thumbOffset = max == 0 ? 0 : (int) (SCROLLBAR_TRAVEL * (scrollRow / (float) max));
        context.drawGuiTexture(max > 0 ? SCROLLER : SCROLLER_DISABLED,
                SCROLLBAR_X + 1, SCROLLBAR_Y + 1 + thumbOffset, 12, 15);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);

        if (!handler.getCursorStack().isEmpty())
            return;

        var entry = entryAt(mouseX, mouseY);
        if (entry == null)
            return;

        var tooltip = new ArrayList<>(getTooltipFromItem(client, entry.variant().toStack(1)));
        tooltip.add(Text.literal("Stored: " + entry.count()).formatted(Formatting.GRAY));
        context.drawTooltip(textRenderer, tooltip, Optional.empty(), mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isOverNetwork(mouseX, mouseY)) {
            if (!handler.getCursorStack().isEmpty()) {
                LecternClientNetworking.sendInsert(handler.syncId, button);
            } else {
                var entry = entryAt(mouseX, mouseY);
                if (entry != null)
                    LecternClientNetworking.sendExtract(handler.syncId, entry.variant(), button, hasShiftDown());
            }
            return true;
        }

        if (isOverScrollbar(mouseX, mouseY)) {
            scrolling = true;
            updateScrollFromMouse(mouseY);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (scrolling) {
            updateScrollFromMouse(mouseY);
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        scrolling = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double amount) {
        int max = maxScrollRow();
        if (isOverNetwork(mouseX, mouseY) && max > 0) {
            scrollRow = MathHelper.clamp(scrollRow - (int) Math.signum(amount), 0, max);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, amount);
    }

    private int maxScrollRow() {
        int rows = (handler.getClientEntries().size() + COLUMNS - 1) / COLUMNS;
        return Math.max(0, rows - ROWS);
    }

    private boolean isOverScrollbar(double mouseX, double mouseY) {
        return mouseX >= x + SCROLLBAR_X && mouseX < x + SCROLLBAR_X + 15
                && mouseY >= y + SCROLLBAR_Y && mouseY < y + SCROLLBAR_Y + 55;
    }

    private void updateScrollFromMouse(double mouseY) {
        int max = maxScrollRow();
        // Centre the thumb on the cursor: half of the 15px thumb, over the 38px of travel.
        scrollRow = MathHelper.clamp((int) Math.round((mouseY - (y + SCROLLBAR_Y + 1) - 7.5) / SCROLLBAR_TRAVEL * max), 0, max);
    }

    private boolean isOverNetwork(double mouseX, double mouseY) {
        return mouseX >= x + NETWORK_X && mouseX < x + NETWORK_X + COLUMNS * 18
                && mouseY >= y + NETWORK_Y && mouseY < y + NETWORK_Y + ROWS * 18;
    }

    private LibraryNetwork.Entry entryAt(double mouseX, double mouseY) {
        var entries = handler.getClientEntries();

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int cellX = x + NETWORK_X + col * 18;
                int cellY = y + NETWORK_Y + row * 18;
                if (mouseX < cellX || mouseX >= cellX + 16 || mouseY < cellY || mouseY >= cellY + 16)
                    continue;

                int index = (scrollRow + row) * COLUMNS + col;
                return index < entries.size() ? entries.get(index) : null;
            }
        }

        return null;
    }

    private void drawCount(DrawContext context, String text, int cellX, int cellY) {
        var matrices = context.getMatrices();
        matrices.push();
        matrices.translate(0.0F, 0.0F, 200.0F);
        matrices.scale(0.5F, 0.5F, 1.0F);
        context.drawTextWithShadow(textRenderer, text, cellX * 2 + 33 - textRenderer.getWidth(text), cellY * 2 + 24, 0xFFFFFF);
        matrices.pop();
    }

    private static String formatCount(long count) {
        if (count < 1000)
            return Long.toString(count);
        if (count < 10000)
            return String.format(Locale.ROOT, "%.1fK", count / 1000.0);
        if (count < 1000000)
            return (count / 1000) + "K";
        if (count < 10000000)
            return String.format(Locale.ROOT, "%.1fM", count / 1000000.0);
        return (count / 1000000) + "M";
    }
}
