package net.messer.mystical_index.compat;

import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.messer.mystical_index.MysticalIndex;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

public class PistonCraftingCategory implements DisplayCategory<PistonCraftingDisplay> {

    public static final Identifier IDENTIFIER = new Identifier(MysticalIndex.MOD_ID, "textures/gui/piston_crafting_gui.png");

    public static  final CategoryIdentifier<PistonCraftingDisplay> PISTON_CRAFTING = CategoryIdentifier.of(MysticalIndex.MOD_ID);

    @Override
    public CategoryIdentifier<? extends PistonCraftingDisplay> getCategoryIdentifier() {
        return PISTON_CRAFTING;
    }

    @Override
    public Text getTitle() {
        return Text.literal("Piston Crafting");
    }

    @Override
    public Renderer getIcon() {
        return EntryStacks.of(Items.LECTERN.asItem().getDefaultStack());
    }

    @Override
    public List<Widget> setupDisplay(PistonCraftingDisplay display, me.shedaniel.math.Rectangle bounds) {
        return DisplayCategory.super.setupDisplay(display, bounds);
    }
}
