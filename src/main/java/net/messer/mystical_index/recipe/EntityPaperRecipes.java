package net.messer.mystical_index.recipe;

import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.item.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class EntityPaperRecipes extends SpecialCraftingRecipe {

    public EntityPaperRecipes(Identifier id, CraftingRecipeCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(RecipeInputInventory recipeInputInventory, World world) {
        if (!this.fits(recipeInputInventory.getWidth(), recipeInputInventory.getHeight())) {
            return false;
        } else {
            for(int i = 0; i < recipeInputInventory.size(); ++i) {
                ItemStack itemStack = recipeInputInventory.getStack(i);
                // Centre slot takes the egg, every other slot takes entity paper.
                if (i == 4) {
                    if (itemStack.getItem() != Items.EGG)
                        return false;
                } else if (itemStack.getItem() != ModItems.ENTITY_PAPER) {
                    return false;
                }
            }

            return true;
        }
    }

    @Override
    public ItemStack craft(RecipeInputInventory inventory, DynamicRegistryManager registryManager) {
        //var firstItem = inventory.getInputStacks().get(0);
        var firstItem = inventory.getStack(0);
        if(firstItem.getNbt() == null)
            return ItemStack.EMPTY;

        var nbt = firstItem.getNbt();
        if(!nbt.contains("entity"))
            return ItemStack.EMPTY;

        boolean allNbtMatch = true;
        for(var item : inventory.getInputStacks()){
            if(item.getItem() == Items.EGG)
                continue;

            if(!nbt.equals(item.getNbt()))
                allNbtMatch = false;
        }

        if(!allNbtMatch)
            return ItemStack.EMPTY;

        // The stored id can name an entity that no longer exists (removed mod) or one with no
        // spawn egg, so neither lookup is guaranteed to resolve.
        var entityType = EntityType.get(nbt.getString("entity")).orElse(null);
        if(entityType == null)
            return ItemStack.EMPTY;

        var spawnEgg = SpawnEggItem.forEntity(entityType);
        if(spawnEgg == null)
            return ItemStack.EMPTY;

        return spawnEgg.getDefaultStack();
    }

    @Override
    public boolean fits(int width, int height) {
        return width == 3 && height == 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipe.PAPER_SHAPED;
    }
}
