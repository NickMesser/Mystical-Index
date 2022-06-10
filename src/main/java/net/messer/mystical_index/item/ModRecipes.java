package net.messer.mystical_index.item;

import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.item.recipe.MysticalBookRecipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialRecipeSerializer;

public class ModRecipes {
    public static final SpecialRecipeSerializer<MysticalBookRecipe> MYSTICAL_BOOK = RecipeSerializer.register(
            MysticalIndex.MOD_ID + ":crafting_special_custom_book",
            new SpecialRecipeSerializer<>(MysticalBookRecipe::new));

    public static void registerModRecipes(){
        MysticalIndex.LOGGER.info("Registering recipes for " + MysticalIndex.MOD_ID);
    }
}
