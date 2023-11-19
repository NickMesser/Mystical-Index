package net.messer.mystical_index.recipe;


import net.messer.mystical_index.MysticalIndex;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModRecipe {

    public static RecipeSerializer<ShapedBookRecipe> BOOK_SHAPED;
    public static RecipeSerializer<EntityPaperRecicpe> PAPER_SHAPED;

    public static void registerRecipes() {
        MysticalIndex.LOGGER.info("Registering recipes");
        PAPER_SHAPED = Registry.register(Registries.RECIPE_SERIALIZER, new Identifier(MysticalIndex.MOD_ID, "paper_shaped"), new EntityPaperRecicpe.Serializer());
        BOOK_SHAPED = Registry.register(Registries.RECIPE_SERIALIZER, new Identifier(MysticalIndex.MOD_ID, "book_shaped"), new ShapedBookRecipe.Serializer());
    }
}
