package net.messer.mystical_index.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class PistonRecipeInitializer implements SimpleSynchronousResourceReloadListener {

    private static PistonRecipeInitializer INSTANCE = new PistonRecipeInitializer();

    private static  List<PistonRecipe> pistonRecipes = new ArrayList<>(0);

    public static PistonRecipeInitializer getInstance()
    {
        return INSTANCE;
    }

    public List<PistonRecipe> getRecipes() {
        return pistonRecipes;
    }

    @Override
    public Identifier getFabricId() {
        return new Identifier("mystical_index", "piston_recipes");
    }

    @Override
    public void reload(ResourceManager manager) {
        for(Identifier id : manager.findResources("piston_recipes", path -> path.getPath().endsWith(".json")).keySet()) {
            var resource = manager.getResource(id);
            try(InputStream stream = resource.get().getInputStream()) {
                Reader reader = new InputStreamReader(stream);
                JsonElement json = JsonParser.parseReader(reader);
                processInputs(json.getAsJsonObject());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void processInputs(JsonObject json) {
        PistonRecipe recipe = new PistonRecipe();
        JsonArray inputs = json.getAsJsonArray("input");
        for (JsonElement input : inputs) {
            String itemName = JsonHelper.getString(input.getAsJsonObject(), "item");
            int amount = JsonHelper.getInt(input.getAsJsonObject(), "amount");
            var item = Registries.ITEM.get(Identifier.tryParse(itemName));
            recipe.addInput(item, amount);
        }
        JsonArray outputs = json.getAsJsonArray("output");
        for (JsonElement output : outputs) {
            String itemName = JsonHelper.getString(output.getAsJsonObject(), "item");
            int amount = JsonHelper.getInt(output.getAsJsonObject(), "amount");
            var item = Registries.ITEM.get(Identifier.tryParse(itemName));
            recipe.addOutput(item, amount);
        }
        pistonRecipes.add(recipe);
    }
    public PistonRecipe getRecipe(List<ItemStack> inputStacks) {
        for (var recipe : pistonRecipes) {
            var recipeInputs = recipe.getInputs();

            if (inputStacks.size() != recipeInputs.size()) {
                continue;
            }

            boolean isMatch = inputStacks.stream().allMatch(stack ->
                    recipeInputs.getOrDefault(stack.getItem(), Integer.valueOf(0)).equals(stack.getCount())
            );

            boolean allInputsCovered = recipeInputs.keySet().stream().allMatch(item ->
                    inputStacks.stream().anyMatch(stack -> stack.getItem().equals(item))
            );

            if (isMatch && allInputsCovered) {
                return recipe;
            }
        }
        return null;
    }
}
