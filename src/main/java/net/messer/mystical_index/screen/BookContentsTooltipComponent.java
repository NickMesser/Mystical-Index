package net.messer.mystical_index.screen;

import net.messer.mystical_index.item.inventory.BookContentsTooltipData;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;

import java.util.List;
import java.util.Locale;

public class BookContentsTooltipComponent implements TooltipComponent {

    private static final int CELL = 18;
    private static final int MAX_COLUMNS = 9;

    private final List<BookContentsTooltipData.TypeSummary> summaries;

    public BookContentsTooltipComponent(BookContentsTooltipData data) {
        this.summaries = data.summaries();
    }

    private int columns() {
        return Math.min(MAX_COLUMNS, summaries.size());
    }

    private int rows() {
        return (summaries.size() + MAX_COLUMNS - 1) / MAX_COLUMNS;
    }

    @Override
    public int getHeight() {
        return summaries.isEmpty() ? 0 : rows() * CELL + 2;
    }

    @Override
    public int getWidth(TextRenderer textRenderer) {
        return summaries.isEmpty() ? 0 : columns() * CELL + 2;
    }

    @Override
    public void drawItems(TextRenderer textRenderer, int x, int y, DrawContext context) {
        for (int i = 0; i < summaries.size(); i++) {
            var summary = summaries.get(i);
            int cellX = x + (i % MAX_COLUMNS) * CELL + 1;
            int cellY = y + (i / MAX_COLUMNS) * CELL + 1;

            context.drawItem(summary.representative(), cellX, cellY);
            drawCount(context, textRenderer, formatCount(summary.total()), cellX, cellY);
        }
    }

    private static void drawCount(DrawContext context, TextRenderer textRenderer, String text, int cellX, int cellY) {
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
