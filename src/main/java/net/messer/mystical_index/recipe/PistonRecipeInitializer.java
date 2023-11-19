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

    private static final PistonRecipeInitializer INSTANCE = new PistonRecipeInitializer();

    private static final List<PistonRecipe> pistonRecipes = new ArrayList<>();

    public static PistonRecipeInitializer getInstance() {
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
        pistonRecipes.clear();
        for (Identifier id : manager.findResources("piston_recipes", path -> path.getPath().endsWith(".json")).keySet()) {
            try (InputStream stream = manager.getResource(id).get().getInputStream()) {
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
        JsonArray inputs = JsonHelper.getArray(json, "input");
        for (JsonElement input : inputs) {
            JsonObject inputObj = input.getAsJsonObject();
            String itemName = JsonHelper.getString(inputObj, "item");
            int amount = JsonHelper.getInt(inputObj, "amount");
            String nbtData = inputObj.has("nbt") ? inputObj.get("nbt").getAsString() : null;
            Item item = Registries.ITEM.get(new Identifier(itemName));
            recipe.addInput(item, amount, nbtData);
        }
        JsonArray outputs = JsonHelper.getArray(json, "output");
        for (JsonElement output : outputs) {
            JsonObject outputObj = output.getAsJsonObject();
            String itemName = JsonHelper.getString(outputObj, "item");
            int amount = JsonHelper.getInt(outputObj, "amount");
            String nbtData = outputObj.has("nbt") ? outputObj.get("nbt").getAsString() : null;
            Item item = Registries.ITEM.get(new Identifier(itemName));
            recipe.addOutput(item, amount, nbtData);
        }
        pistonRecipes.add(recipe);
    }

    public PistonRecipe getRecipe(List<ItemStack> inputStacks) {
        for (PistonRecipe recipe : pistonRecipes) {
            boolean inputsContainNBT = recipe.getInputs().values().stream().anyMatch(itemEntry -> itemEntry.nbt.isPresent());

            if(inputsContainNBT){
                boolean isMatch = recipe.getInputs().keySet().stream().allMatch(item ->
                        inputStacks.stream().anyMatch(stack ->
                                stack.getItem().equals(item) &&
                                        stack.getCount() == recipe.getInputs().get(item).count &&
                                        stack.getNbt().equals(recipe.getInputs().get(item).nbt.get())
                        )
                );
                boolean allInputsCovered = recipe.getInputs().keySet().stream().allMatch(item ->
                        inputStacks.stream().anyMatch(stack -> stack.getItem().equals(item))
                );
                if (isMatch && allInputsCovered) {
                    return recipe;
                }
            }

            boolean isMatch = recipe.getInputs().keySet().stream().allMatch(item ->
                    inputStacks.stream().anyMatch(stack ->
                            stack.getItem().equals(item) &&
                                    stack.getCount() == recipe.getInputs().get(item).count
                    )
            );
            boolean allInputsCovered = recipe.getInputs().keySet().stream().allMatch(item ->
                    inputStacks.stream().anyMatch(stack -> stack.getItem().equals(item))
            );
            if (isMatch && allInputsCovered) {
                return recipe;
            }
        }
        return null;
    }
}
