package net.messer.mystical_index.recipe;


import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.messer.mystical_index.MysticalIndex;
import net.minecraft.world.item.crafting.RecipeSerializer;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.core.registries.Registries;

public class ModRecipe {

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(MysticalIndex.MOD_ID, Registries.RECIPE_SERIALIZER);

    public static final RegistrySupplier<RecipeSerializer<EntityPaperRecipes>> PAPER_SHAPED =
            RECIPE_SERIALIZERS.register("paper_shaped", () -> serializerFor(new EntityPaperRecipes()));

    public static final RegistrySupplier<RecipeSerializer<HoldingBookUpgradeRecipe>> HOLDING_BOOK_UPGRADE =
            RECIPE_SERIALIZERS.register("holding_book_upgrade", () -> serializerFor(new HoldingBookUpgradeRecipe()));

    // RecipeSerializer became a record of codecs. These recipes hold no data of their own, so
    // both codecs are units over a single stateless instance.
    private static <T extends net.minecraft.world.item.crafting.Recipe<?>> RecipeSerializer<T> serializerFor(T recipe) {
        return new RecipeSerializer<>(MapCodec.unit(recipe), StreamCodec.unit(recipe));
    }

    public static void registerRecipes() {
        MysticalIndex.LOGGER.info("Registering recipes");
        RECIPE_SERIALIZERS.register();
    }
}
