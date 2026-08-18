package net.messer.mystical_index.recipe;

import net.minecraft.core.registries.Registries;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.messer.util.MysticalUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Loads the {@code piston_recipes} datapack folder.
 *
 * <p>This used to implement Fabric's {@code SimpleSynchronousResourceReloadListener}. Vanilla's
 * {@link ResourceReloader} is implemented directly instead so the class stays loader-free; the
 * default method that wrapper supplied is reproduced in {@link #reload} below, which is why the
 * whole load still happens in one synchronous step on the apply executor.
 */
public class PistonRecipeInitializer extends SimplePreparableReloadListener<List<PistonRecipe>> {

    public static final Identifier ID = Identifier.fromNamespaceAndPath("mystical_index", "piston_recipes");

    private static final PistonRecipeInitializer INSTANCE = new PistonRecipeInitializer();

    private static final List<PistonRecipe> pistonRecipes = new ArrayList<>();

    public static PistonRecipeInitializer getInstance() {
        return INSTANCE;
    }

    public List<PistonRecipe> getRecipes() {
        return pistonRecipes;
    }

    @Override
    public String getName() {
        return ID.toString();
    }

    // prepare() does the reading off-thread and apply() publishes the result, which is the same
    // load-then-swap the old synchronizer-based override performed in one step.
    @Override
    protected List<PistonRecipe> prepare(ResourceManager manager, ProfilerFiller profiler) {
        var loaded = new ArrayList<PistonRecipe>();
        for (Identifier id : manager.listResources("piston_recipes", path -> path.getPath().endsWith(".json")).keySet()) {
            try (InputStream stream = manager.getResource(id).get().open()) {
                Reader reader = new InputStreamReader(stream);
                JsonElement json = JsonParser.parseReader(reader);
                processInputs(json.getAsJsonObject(), loaded);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return loaded;
    }

    @Override
    protected void apply(List<PistonRecipe> loaded, ResourceManager manager, ProfilerFiller profiler) {
        pistonRecipes.clear();
        pistonRecipes.addAll(loaded);
    }


    protected void processInputs(JsonObject json, List<PistonRecipe> sink) {
        PistonRecipe recipe = new PistonRecipe();
        JsonArray inputs = GsonHelper.getAsJsonArray(json, "input");
        for (JsonElement input : inputs) {
            JsonObject inputObj = input.getAsJsonObject();
            String itemName = GsonHelper.getAsString(inputObj, "item");
            int amount = GsonHelper.getAsInt(inputObj, "amount");
            String nbtData = inputObj.has("nbt") ? inputObj.get("nbt").getAsString() : null;
            Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(itemName));
            recipe.addInput(item, amount, nbtData);
        }
        JsonArray outputs = GsonHelper.getAsJsonArray(json, "output");
        for (JsonElement output : outputs) {
            JsonObject outputObj = output.getAsJsonObject();
            String itemName = GsonHelper.getAsString(outputObj, "item");
            int amount = GsonHelper.getAsInt(outputObj, "amount");
            String nbtData = outputObj.has("nbt") ? outputObj.get("nbt").getAsString() : null;
            Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(itemName));
            recipe.addOutput(item, amount, nbtData);
        }
        sink.add(recipe);
    }

    public PistonRecipe getRecipe(List<ItemStack> inputStacks) {
        for (PistonRecipe recipe : pistonRecipes) {
            var recipeInputs = recipe.getInputs();

            // Check for size match first.
            if(recipe.getInputs().size() != inputStacks.size())
                continue;

            // check for nbt inputs and compare against input stacks
            boolean inputsContainNBT = recipe.getInputs().values().stream().anyMatch(itemEntry -> itemEntry.nbt.isPresent());
            boolean inputStacksContainNBT = inputStacks.stream().anyMatch(MysticalUtil::hasCustomData);

            if(inputsContainNBT != inputStacksContainNBT)
                continue;

            boolean nbtMatches = true;
            if(inputsContainNBT){
                for(var input: recipeInputs.keySet()){
                    var entry = recipeInputs.get(input);
                    var nbt = entry.nbt.orElse(null);
                    if(nbt == null)
                        continue;
                    var nbtKeys = nbt.keySet();
                    for(var key: nbtKeys){
                        if(!inputStacks.stream().anyMatch(stack -> {
                            var stackNbt = MysticalUtil.copyCustomData(stack);
                            return stackNbt.contains(key) && stackNbt.get(key).equals(nbt.get(key));
                        })){

                            nbtMatches = false;
                            break;
                        }
                    }
                }
            }

            // Check that count matches
            boolean isMatch = recipe.getInputs().keySet().stream().allMatch(item ->
                    inputStacks.stream().anyMatch(stack ->
                            stack.getItem().equals(item) &&
                                    stack.getCount() == recipe.getInputs().get(item).count
                    )
            );

            //check that all inputs are covered
            boolean allInputsCovered = recipe.getInputs().keySet().stream().allMatch(item ->
                    inputStacks.stream().anyMatch(stack -> stack.getItem().equals(item))
            );

            if (isMatch && allInputsCovered && nbtMatches) {
                return recipe;
            }
        }
        return null;
    }
}
