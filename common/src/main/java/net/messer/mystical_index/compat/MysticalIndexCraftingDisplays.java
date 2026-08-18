package net.messer.mystical_index.compat;

import net.minecraft.core.registries.Registries;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.plugin.common.displays.crafting.DefaultCustomDisplay;
import net.messer.mystical_index.item.ModItems;
import net.messer.util.MysticalUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Optional;
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

        return new DefaultCustomDisplay(grid, List.of(EntryIngredients.of(result)), Optional.empty());
    }

    private static List<DefaultCustomDisplay> holdingBookUpgrades() {
        return List.of(
                upgrade(ModItems.HOLDING_BOOK_TIER1.get(), Items.GOLD_INGOT, RING, ModItems.HOLDING_BOOK_TIER2.get()),
                upgrade(ModItems.HOLDING_BOOK_TIER2.get(), Items.DIAMOND, RING, ModItems.HOLDING_BOOK_TIER3.get()),
                upgrade(ModItems.HOLDING_BOOK_TIER3.get(), Items.NETHERITE_INGOT, CROSS, ModItems.HOLDING_BOOK_TIER4.get()));
    }

    private static List<DefaultCustomDisplay> entityPaperSpawnEggs() {
        var displays = new ArrayList<DefaultCustomDisplay>();

        for (var spawnEgg : BuiltInRegistries.ITEM.stream().filter(SpawnEggItem.class::isInstance).map(SpawnEggItem.class::cast).toList()) {
            // getEntityType reads the stack's entity_data component now, so it dereferences what
            // it is handed. A plain stack carries no override and falls back to the egg's own type.
            var entityType = SpawnEggItem.getType(new ItemStack(spawnEgg));
            if (entityType == null)
                continue;

            var paper = entityPaper(entityType);
            var grid = emptyGrid();
            for (int slot = 0; slot < GRID_SIZE; slot++) {
                grid.set(slot, slot == CENTRE
                        ? EntryIngredients.of(Items.EGG)
                        : EntryIngredients.of(paper.copy()));
            }

            displays.add(new DefaultCustomDisplay(grid, List.of(EntryIngredients.of(new ItemStack(spawnEgg))), Optional.empty()));
        }

        return displays;
    }

    private static ItemStack entityPaper(EntityType<?> entityType) {
        var stack = new ItemStack(ModItems.ENTITY_PAPER.get());
        MysticalUtil.editCustomData(stack,
                nbt -> nbt.putString("entity", BuiltInRegistries.ENTITY_TYPE.getKey(entityType).toString()));
        // Same name EntityPaper.onCraftByPlayer applies. It has to match exactly, or the variant
        // this display advertises would not compare equal to the papers the network actually holds.
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable("item.mystical_index.entity_paper.named", entityType.getDescription()));
        return stack;
    }
}
