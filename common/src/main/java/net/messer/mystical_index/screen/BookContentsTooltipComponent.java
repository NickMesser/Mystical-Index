package net.messer.mystical_index.screen;

import net.messer.mystical_index.item.inventory.BookContentsTooltipData;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

import java.util.List;
import java.util.Locale;

public class BookContentsTooltipComponent implements ClientTooltipComponent {

    private static final int CELL = 18;
    private static final int MAX_COLUMNS = 9;
    // A full tier-IV book (40 types) fits in 5 rows, but config-raised tiers can hold far more.
    // Cap the drawn grid so the tooltip can never grow taller than the screen; any overflow is
    // rolled up into a "+N" indicator in the last visible cell.
    private static final int MAX_ROWS = 6;
    private static final int MAX_CELLS = MAX_ROWS * MAX_COLUMNS;

    private final List<BookContentsTooltipData.TypeSummary> summaries;

    public BookContentsTooltipComponent(BookContentsTooltipData data) {
        this.summaries = data.summaries();
    }

    private int columns() {
        return Math.min(MAX_COLUMNS, summaries.size());
    }

    private int rows() {
        int visible = Math.min(summaries.size(), MAX_CELLS);
        return (visible + MAX_COLUMNS - 1) / MAX_COLUMNS;
    }

    @Override
    public int getHeight(Font font) {
        return summaries.isEmpty() ? 0 : rows() * CELL + 2;
    }

    @Override
    public int getWidth(Font font) {
        return summaries.isEmpty() ? 0 : columns() * CELL + 2;
    }

    @Override
    public void extractImage(Font font, int x, int y, int width, int height, GuiGraphicsExtractor context) {
        boolean overflow = summaries.size() > MAX_CELLS;
        // When there are more types than cells, give up the last cell to a "+N" indicator so the
        // hidden types are still accounted for instead of silently dropped off the bottom.
        int drawn = overflow ? MAX_CELLS - 1 : summaries.size();

        for (int i = 0; i < drawn; i++) {
            var summary = summaries.get(i);
            int cellX = x + (i % MAX_COLUMNS) * CELL + 1;
            int cellY = y + (i / MAX_COLUMNS) * CELL + 1;

            context.item(summary.representative(), cellX, cellY);
            drawCount(context, font, formatCount(summary.total()), cellX, cellY);
        }

        if (overflow) {
            int cellX = x + (drawn % MAX_COLUMNS) * CELL + 1;
            int cellY = y + (drawn / MAX_COLUMNS) * CELL + 1;
            drawCount(context, font, "+" + (summaries.size() - drawn), cellX, cellY);
        }
    }

    private static void drawCount(GuiGraphicsExtractor context, Font font, String text, int cellX, int cellY) {
        var pose = context.pose();
        context.nextStratum();
        pose.pushMatrix();
        pose.scale(0.5F, 0.5F);
        context.text(font, text, cellX * 2 + 33 - font.width(text), cellY * 2 + 24, 0xFFFFFFFF);
        pose.popMatrix();
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
