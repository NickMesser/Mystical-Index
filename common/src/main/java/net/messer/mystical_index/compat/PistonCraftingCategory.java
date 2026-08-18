package net.messer.mystical_index.compat;

import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.messer.mystical_index.MysticalIndex;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class PistonCraftingCategory implements DisplayCategory<PistonCraftingDisplay> {

// CategoryIdentifier.of(String) parses the argument as a full identifier, so a bare mod id
// would land in the minecraft namespace. The namespace has to be passed separately.
    public static final CategoryIdentifier<PistonCraftingDisplay> PISTON_CRAFTING =
            CategoryIdentifier.of(MysticalIndex.MOD_ID, "piston_crafting");

// REI's Panel border eats a few pixels on each side of the recipe base.
    private static final int PANEL_PADDING = 6;

    @Override
    public CategoryIdentifier<? extends PistonCraftingDisplay> getCategoryIdentifier() {
        return PISTON_CRAFTING;
    }

    @Override
    public Component getTitle() {
        return Component.literal("Piston Crafting");
    }

    @Override
    public Renderer getIcon() {
        return EntryStacks.of(Items.PISTON);
    }

    @Override
    public int getDisplayHeight() {
        return 84;
    }

    @Override
    public List<Widget> setupDisplay(PistonCraftingDisplay display, Rectangle bounds) {
        var startPoint = new Point(bounds.getCenterX() - 58, bounds.getCenterY() - 27);
        List<Widget> widgets = new ArrayList<>();

        widgets.add(Widgets.createRecipeBase(bounds));
        widgets.add(Widgets.createArrow(new Point(startPoint.x + 60, startPoint.y + 18)));

        var inputs = display.getInputEntries();
        for (int i = 0; i < inputs.size() && i < 9; i++) {
            widgets.add(Widgets.createSlot(new Point(startPoint.x + 1 + (i % 3) * 18, startPoint.y + 1 + (i / 3) * 18))
                    .entries(inputs.get(i))
                    .markInput());
        }

// Render every output, not just the first. A single output keeps the classic oversized
// result slot; multiple outputs stack in a column centred on the same anchor so none is
// hidden behind another. The large result background only reads well behind one slot, so it
// is drawn for the single-output case; stacked slots rely on their own slot backgrounds.
        var outputs = display.getOutputEntries();
        int resultX = startPoint.x + 95;
        int resultY = startPoint.y + 19;

        if (outputs.size() <= 1)
            widgets.add(Widgets.createResultSlotBackground(new Point(resultX, resultY)));

        int firstOutputY = resultY - ((outputs.size() - 1) * 18) / 2;
        for (int i = 0; i < outputs.size(); i++) {
            widgets.add(Widgets.createSlot(new Point(resultX, firstOutputY + i * 18))
                    .entries(outputs.get(i))
                    .markOutput());
        }

        var note = display.getNote();
        if (note != null) {
// The panel is only 150 wide, so a note is measured against the usable inner width and
// wrapped onto stacked lines instead of spilling out over the screen behind it.
            var lines = wrapNote(note, bounds.getWidth() - 2 * PANEL_PADDING);
            int lineY = startPoint.y + (lines.size() > 1 ? 52 : 58);

            for (var line : lines) {
                widgets.add(Widgets.createLabel(new Point(bounds.getCenterX(), lineY), line).centered());
                lineY += 10;
            }
        }

        return widgets;
    }

    private static List<Component> wrapNote(Component note, int maxWidth) {
        var client = Minecraft.getInstance();
        if (client.font == null || client.font.width(note) <= maxWidth)
            return List.of(note);

        var lines = new ArrayList<Component>();
        for (var line : client.font.getSplitter().splitLines(note, maxWidth, Style.EMPTY))
            lines.add(Component.literal(line.getString()));

        return lines;
    }
}
