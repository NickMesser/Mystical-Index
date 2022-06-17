package net.messer.mystical_index.client.tooltip;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.util.BigStack;
import net.messer.mystical_index.util.MathUtil;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class ItemStorageTooltipComponent extends DrawableHelper implements TooltipComponent {
    private static final Identifier CIRCLES_TEXTURE = MysticalIndex.id("textures/gui/circles.png");
    private static final int CIRCLE_TEXTURES_SIZE = 256;
    private static final Map<Integer, Identifier> CIRCLE_TEXTURES = Map.of(
            24, MysticalIndex.id("textures/gui/circle_24.png"),
            48, MysticalIndex.id("textures/gui/circle_48.png")
    );
    private static final int SECONDARY_CIRCLE_ITEM_COUNT = 7;
    private static final int TERNARY_CIRCLE_ITEM_COUNT = 19;

    private final ItemStorageTooltipData data;

    public ItemStorageTooltipComponent(ItemStorageTooltipData data) {
        this.data = data;
    }

    public int getSize() {
        if (data.size <= 0) return 0;
        if (data.size <= 1) return 22;
        if (data.size <= SECONDARY_CIRCLE_ITEM_COUNT) return 64;
        if (data.size <= TERNARY_CIRCLE_ITEM_COUNT) return 112;
        return 112;
    }

    @Override
    public int getHeight() {
        return getSize();
    }

    @Override
    public int getWidth(TextRenderer textRenderer) {
        return getSize();
    }

    @Override
    public void drawItems(TextRenderer textRenderer, int x, int y, MatrixStack matrices, ItemRenderer itemRenderer, int z) {
        if (data.contents.getAll().isEmpty()) return;

        var centerX = x + getWidth(textRenderer) / 2;
        var centerY = y + getHeight() / 2;

        var stacks = data.contents.getAll();
        var primary = stacks.get(0);
        var secondary = stacks.size() > 1 ?
                stacks.subList(1, Math.min(SECONDARY_CIRCLE_ITEM_COUNT, stacks.size())) : null;
        var ternary = stacks.size() > SECONDARY_CIRCLE_ITEM_COUNT ?
                stacks.subList(SECONDARY_CIRCLE_ITEM_COUNT, Math.min(TERNARY_CIRCLE_ITEM_COUNT, stacks.size())) : null;

        if (secondary != null) drawItemCircle(textRenderer, centerX, centerY, matrices, itemRenderer, 24, secondary);
        if (ternary != null) drawItemCircle(textRenderer, centerX, centerY, matrices, itemRenderer, 48, ternary);
        drawItem(textRenderer, centerX, centerY, matrices, itemRenderer, primary, true);
    }

    private void drawItemCircle(TextRenderer textRenderer, int x, int y, MatrixStack matrices, ItemRenderer itemRenderer, int radius, List<BigStack> items) {
        var itemCount = items.size();
        var circleTexture = CIRCLE_TEXTURES.get(radius);

        RenderSystem.setShaderTexture(0, circleTexture);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();

        drawTexture(
                matrices,
                x - CIRCLE_TEXTURES_SIZE / 2, y - CIRCLE_TEXTURES_SIZE / 2,
                0, 0, CIRCLE_TEXTURES_SIZE, CIRCLE_TEXTURES_SIZE
        );

        for (int i = 0; i < itemCount; i++) {
            var bigStack = items.get(i);
            var offset = (2 * Math.PI) / itemCount * i - (Math.PI / 2);

            var itemX = (int) (radius * Math.cos(offset)) + x;
            var itemY = (int) (radius * Math.sin(offset)) + y;

            drawItem(textRenderer, itemX, itemY, matrices, itemRenderer, bigStack, false);
        }
    }

    private void drawItem(TextRenderer textRenderer, int x, int y, MatrixStack matrices, ItemRenderer itemRenderer, BigStack stack, boolean isPrimary) {
        var count = stack.getAmount();

        RenderSystem.setShaderTexture(0, CIRCLES_TEXTURE);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();

        drawTexture(matrices, x - 12, y - 12, isPrimary ? 24 : 0, 0, 24, 24);

        itemRenderer.renderInGuiWithOverrides(stack.getItemStack(), x - 8, y - 8);
        itemRenderer.renderGuiItemOverlay(textRenderer, stack.getItemStack(), x - 8, y - 8,
                count > 1 ? MathUtil.shortNumberFormat(count) : "");
    }
}
