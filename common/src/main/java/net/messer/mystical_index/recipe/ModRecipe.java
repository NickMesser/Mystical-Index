package net.messer.mystical_index.recipe;


import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.messer.mystical_index.MysticalIndex;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialRecipeSerializer;
import net.minecraft.registry.RegistryKeys;

public class ModRecipe {

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(MysticalIndex.MOD_ID, RegistryKeys.RECIPE_SERIALIZER);

    public static final RegistrySupplier<RecipeSerializer<EntityPaperRecipes>> PAPER_SHAPED =
            RECIPE_SERIALIZERS.register("paper_shaped", () -> new SpecialRecipeSerializer<>(EntityPaperRecipes::new));

    public static final RegistrySupplier<RecipeSerializer<HoldingBookUpgradeRecipe>> HOLDING_BOOK_UPGRADE =
            RECIPE_SERIALIZERS.register("holding_book_upgrade", () -> new SpecialRecipeSerializer<>(HoldingBookUpgradeRecipe::new));

    public static void registerRecipes() {
        MysticalIndex.LOGGER.info("Registering recipes");
        RECIPE_SERIALIZERS.register();
    }
}
