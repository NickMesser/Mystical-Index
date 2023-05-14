package net.messer.mystical_index.recipe;


import net.messer.mystical_index.MysticalIndex;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModRecipe {

    public static void registerRecipes() {
        MysticalIndex.LOGGER.info("Registering recipes");
        Registry.register(Registries.RECIPE_SERIALIZER, new Identifier(MysticalIndex.MOD_ID, BabyVillagerRecipe.BabyVillagerRecipeSerializer.ID), BabyVillagerRecipe.BabyVillagerRecipeSerializer.INSTANCE);
        Registry.register(Registries.RECIPE_TYPE, new Identifier(MysticalIndex.MOD_ID, BabyVillagerRecipe.Type.ID), BabyVillagerRecipe.Type.INSTANCE);
    }
}
