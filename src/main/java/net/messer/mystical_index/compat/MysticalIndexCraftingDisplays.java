package net.messer.mystical_index.compat;

import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.plugin.common.displays.crafting.DefaultCustomDisplay;
import net.messer.mystical_index.item.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

// Both of these are SpecialCraftingRecipes, which REI's default filler skips because they are
// ignored in the recipe book. DefaultCustomDisplay treats its input list as a raw 9 slot grid,
// which is what the vanilla crafting category and the lectern transfer handler both expect.
public class MysticalIndexCraftingDisplays {

    private static final int GRID_SIZE = 9;
    private static final int CENTRE = 4;

    private static final int[] RING = {0, 1, 2, 3, 5, 6, 7, 8};
    private static final int[] CROSS = {1, 3, 5, 7};

    private MysticalIndexCraftingDisplays() {
    }

    public static List<DefaultCustomDisplay> buildAll() {
        var displays = new ArrayList<DefaultCustomDisplay>();
        displays.addAll(holdingBookUpgrades());
        displays.addAll(entityPaperSpawnEggs());
        return displays;
    }

    private static List<EntryIngredient> emptyGrid() {
        var grid = new ArrayList<EntryIngredient>(GRID_SIZE);
        for (int i = 0; i < GRID_SIZE; i++)
            grid.add(EntryIngredient.empty());

        return grid;
    }

    // HoldingBookUpgradeRecipe counts occupied material slots, so these layouts cost exactly what
    // the recipe requires: eight around the book, or four for netherite.
    private static DefaultCustomDisplay upgrade(Item book, Item material, int[] positions, Item result) {
        var grid = emptyGrid();
        grid.set(CENTRE, EntryIngredients.of(book));
        for (int slot : positions)
            grid.set(slot, EntryIngredients.of(material));

        return new DefaultCustomDisplay(null, grid, List.of(EntryIngredients.of(result)));
    }

    private static List<DefaultCustomDisplay> holdingBookUpgrades() {
        return List.of(
                upgrade(ModItems.HOLDING_BOOK_TIER1, Items.GOLD_INGOT, RING, ModItems.HOLDING_BOOK_TIER2),
                upgrade(ModItems.HOLDING_BOOK_TIER2, Items.DIAMOND, RING, ModItems.HOLDING_BOOK_TIER3),
                upgrade(ModItems.HOLDING_BOOK_TIER3, Items.NETHERITE_INGOT, CROSS, ModItems.HOLDING_BOOK_TIER4));
    }

    private static List<DefaultCustomDisplay> entityPaperSpawnEggs() {
        var displays = new ArrayList<DefaultCustomDisplay>();

        for (var spawnEgg : SpawnEggItem.getAll()) {
            var entityType = spawnEgg.getEntityType(null);
            if (entityType == null)
                continue;

            var paper = entityPaper(entityType);
            var grid = emptyGrid();
            for (int slot = 0; slot < GRID_SIZE; slot++) {
                grid.set(slot, slot == CENTRE
                        ? EntryIngredients.of(Items.EGG)
                        : EntryIngredients.of(paper.copy()));
            }

            displays.add(new DefaultCustomDisplay(null, grid, List.of(EntryIngredients.of(new ItemStack(spawnEgg)))));
        }

        return displays;
    }

    private static ItemStack entityPaper(EntityType<?> entityType) {
        var stack = new ItemStack(ModItems.ENTITY_PAPER);
        stack.getOrCreateNbt().putString("entity", Registries.ENTITY_TYPE.getId(entityType).toString());
        // Same name EntityPaper.onCraft applies.
        stack.setCustomName(Text.of(entityType.getName().getString() + " Paper"));
        return stack;
    }
}
