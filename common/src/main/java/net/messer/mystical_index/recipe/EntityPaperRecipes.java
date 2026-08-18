package net.messer.mystical_index.recipe;

import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.item.ModItems;
import net.messer.util.MysticalUtil;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.Level;

public class EntityPaperRecipes extends CustomRecipe {

    public EntityPaperRecipes() {

    }

    @Override
    public boolean matches(CraftingInput recipeInputInventory, Level world) {
        if (!(recipeInputInventory.width() == 3 && recipeInputInventory.height() == 3)) {
            return false;
        } else {
            for(int i = 0; i < recipeInputInventory.size(); ++i) {
                ItemStack itemStack = recipeInputInventory.getItem(i);
                // Centre slot takes the egg, every other slot takes entity paper.
                if (i == 4) {
                    if (itemStack.getItem() != Items.EGG)
                        return false;
                } else if (itemStack.getItem() != ModItems.ENTITY_PAPER.get()) {
                    return false;
                }
            }

            return true;
        }
    }

    @Override
    public ItemStack assemble(CraftingInput inventory) {
        var firstItem = inventory.getItem(0);
        var nbt = MysticalUtil.getCustomData(firstItem);
        if(nbt == null)
            return ItemStack.EMPTY;

        if(!nbt.contains("entity"))
            return ItemStack.EMPTY;

        boolean allNbtMatch = true;
        for(var item : inventory.items()){
            if(item.getItem() == Items.EGG)
                continue;

            if(!nbt.equals(MysticalUtil.getCustomData(item)))
                allNbtMatch = false;
        }

        if(!allNbtMatch)
            return ItemStack.EMPTY;

        // The stored id can name an entity that no longer exists (removed mod) or one with no
        // spawn egg, so neither lookup is guaranteed to resolve.
        var entityType = EntityType.byString(nbt.getStringOr("entity", "")).orElse(null);
        if(entityType == null)
            return ItemStack.EMPTY;

        var spawnEgg = SpawnEggItem.byId(entityType);
        if(spawnEgg == null)
            return ItemStack.EMPTY;

        return spawnEgg.map(h -> h.value().getDefaultInstance()).orElse(ItemStack.EMPTY);
    }


    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }
    @Override
    public RecipeSerializer<EntityPaperRecipes> getSerializer() {
        return ModRecipe.PAPER_SHAPED.get();
    }
}
