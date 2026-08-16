package net.messer.mystical_index.compat;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.messer.mystical_index.item.ModItems;
import net.messer.mystical_index.recipe.PistonRecipe;
import net.messer.mystical_index.recipe.PistonRecipeInitializer;
import net.messer.util.MysticalUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PistonCraftingDisplay extends BasicDisplay {

    // Kept short enough to fit inside the 150 wide recipe panel; the category wraps anything that
    // still does not fit rather than letting it clip.
    private static final Text DROP_NOTE = Text.literal("Drop onto an Iron Block");
    private static final Text CHARGE_NOTE = Text.literal("Paper must match the mob");

    @Nullable
    private final Text note;

    public PistonCraftingDisplay(List<EntryIngredient> inputs, List<EntryIngredient> outputs, @Nullable Text note) {
        super(inputs, outputs, Optional.empty());
        this.note = note;
    }

    @Nullable
    public Text getNote() {
        return note;
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return PistonCraftingCategory.PISTON_CRAFTING;
    }

    public static List<PistonCraftingDisplay> buildAll() {
        var displays = new ArrayList<PistonCraftingDisplay>();

        // Loaded by a SERVER_DATA reload listener, so this is populated once a world is running.
        for (var recipe : PistonRecipeInitializer.getInstance().getRecipes()) {
            var inputs = new ArrayList<EntryIngredient>();
            for (var entry : recipe.getInputs().values())
                inputs.add(EntryIngredients.of(toStack(entry)));

            var outputs = new ArrayList<EntryIngredient>();
            for (var entry : recipe.getOutputs().values())
                outputs.add(EntryIngredients.of(toStack(entry)));

            if (inputs.isEmpty() || outputs.isEmpty())
                continue;

            displays.add(new PistonCraftingDisplay(inputs, outputs, DROP_NOTE));
        }

        displays.add(bookCharging());
        return displays;
    }

    private static ItemStack toStack(PistonRecipe.ItemEntry entry) {
        var stack = new ItemStack(entry.item, Math.max(1, entry.count));

        var nbt = entry.nbt.orElse(null);
        if (nbt == null)
            return stack;

        MysticalUtil.setCustomData(stack, nbt.copy());
        // Same naming EntityPaper.onCraftByPlayer applies, without needing a World here.
        var entityType = EntityType.get(nbt.getString("entity")).orElse(null);
        if (entityType != null)
            stack.set(DataComponentTypes.CUSTOM_NAME, Text.of(entityType.getName().getString() + " Paper"));

        return stack;
    }

    // The book charging path in PistonEntityHook is not a piston recipe json, so it is documented
    // by hand: paper matching the book's mob is consumed and each one adds a kill.
    private static PistonCraftingDisplay bookCharging() {
        var charged = new ItemStack(ModItems.HOSTILE_BOOK);
        charged.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Book of Hostile (+1 kill per Paper)"));

        return new PistonCraftingDisplay(
                List.of(EntryIngredients.of(new ItemStack(ModItems.HOSTILE_BOOK)),
                        EntryIngredients.of(new ItemStack(ModItems.ENTITY_PAPER))),
                List.of(EntryIngredients.of(charged)),
                CHARGE_NOTE);
    }
}
