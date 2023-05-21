package net.messer.mystical_index.recipe;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.item.custom.VillagerBook;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.collection.DefaultedList;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class BabyVillagerRecipe extends ShapedRecipe {
    final int width;
    final int height;
    final DefaultedList<Ingredient> input;
    final ItemStack output;
    private final Identifier id;
    final String group;
    final CraftingRecipeCategory category;
    final boolean showNotification;
    public BabyVillagerRecipe(Identifier id, String group, CraftingRecipeCategory category, int width, int height, DefaultedList<Ingredient> input, ItemStack output, boolean showNotification) {
        super(id, group, category, width, height, input, output, showNotification);
        this.width = width;
        this.height = height;
        this.input = input;
        this.output = output;
        this.id = id;
        this.group = group;
        this.category = category;
        this.showNotification = showNotification;
    }

    public ItemStack getOutput() {
        MysticalIndex.LOGGER.info("getOutput");
        return output;
    }

    @Override
    public DefaultedList<ItemStack> getRemainder(RecipeInputInventory inventory) {
        var itemList = DefaultedList.ofSize(9, ItemStack.EMPTY);
        for(int i = 0; i < inventory.size(); ++i) {
            ItemStack itemStack = inventory.getStack(i);
            if(itemStack.getItem() instanceof VillagerBook){
                itemList.add(itemStack);
            }
        }
        return itemList;
    }

    @Override
    public ItemStack craft(RecipeInputInventory recipeInputInventory, DynamicRegistryManager dynamicRegistryManager) {
        return super.craft(recipeInputInventory, dynamicRegistryManager);
    }

    static Map<String, Ingredient> readSymbols(JsonObject json) {
        HashMap<String, Ingredient> map = Maps.newHashMap();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            if (entry.getKey().length() != 1) {
                throw new JsonSyntaxException("Invalid key entry: '" + entry.getKey() + "' is an invalid symbol (must be 1 character only).");
            }
            if (" ".equals(entry.getKey())) {
                throw new JsonSyntaxException("Invalid key entry: ' ' is a reserved symbol.");
            }
            map.put(entry.getKey(), Ingredient.fromJson(entry.getValue()));
        }
        map.put(" ", Ingredient.EMPTY);
        return map;
    }

    static String[] removePadding(String ... pattern) {
        int i = Integer.MAX_VALUE;
        int j = 0;
        int k = 0;
        int l = 0;
        for (int m = 0; m < pattern.length; ++m) {
            String string = pattern[m];
            i = Math.min(i, BabyVillagerRecipe.findFirstSymbol(string));
            int n = BabyVillagerRecipe.findLastSymbol(string);
            j = Math.max(j, n);
            if (n < 0) {
                if (k == m) {
                    ++k;
                }
                ++l;
                continue;
            }
            l = 0;
        }
        if (pattern.length == l) {
            return new String[0];
        }
        String[] strings = new String[pattern.length - l - k];
        for (int o = 0; o < strings.length; ++o) {
            strings[o] = pattern[o + k].substring(i, j + 1);
        }
        return strings;
    }
    private static int findFirstSymbol(String line) {
        int i;
        for (i = 0; i < line.length() && line.charAt(i) == ' '; ++i) {
        }
        return i;
    }

    private static int findLastSymbol(String pattern) {
        int i;
        for (i = pattern.length() - 1; i >= 0 && pattern.charAt(i) == ' '; --i) {
        }
        return i;
    }


    static String[] getPattern(JsonArray json) {
        String[] strings = new String[json.size()];
        if (strings.length > 3) {
            throw new JsonSyntaxException("Invalid pattern: too many rows, 3 is maximum");
        }
        if (strings.length == 0) {
            throw new JsonSyntaxException("Invalid pattern: empty pattern not allowed");
        }
        for (int i = 0; i < strings.length; ++i) {
            String string = JsonHelper.asString(json.get(i), "pattern[" + i + "]");
            if (string.length() > 3) {
                throw new JsonSyntaxException("Invalid pattern: too many columns, 3 is maximum");
            }
            if (i > 0 && strings[0].length() != string.length()) {
                throw new JsonSyntaxException("Invalid pattern: each row must be the same width");
            }
            strings[i] = string;
        }
        return strings;
    }

    static DefaultedList<Ingredient> createPatternMatrix(String[] pattern, Map<String, Ingredient> symbols, int width, int height) {
        DefaultedList<Ingredient> defaultedList = DefaultedList.ofSize(width * height, Ingredient.EMPTY);
        HashSet<String> set = Sets.newHashSet(symbols.keySet());
        set.remove(" ");
        for (int i = 0; i < pattern.length; ++i) {
            for (int j = 0; j < pattern[i].length(); ++j) {
                String string = pattern[i].substring(j, j + 1);
                Ingredient ingredient = symbols.get(string);
                if (ingredient == null) {
                    throw new JsonSyntaxException("Pattern references symbol '" + string + "' but it's not defined in the key");
                }
                set.remove(string);
                defaultedList.set(j + width * i, ingredient);
            }
        }
        if (!set.isEmpty()) {
            throw new JsonSyntaxException("Key defines symbols that aren't used in pattern: " + set);
        }
        return defaultedList;
    }

    public static class Type implements RecipeType<BabyVillagerRecipe> {
        private Type() { }
        public static final Type INSTANCE = new Type();
        public static final String ID = "baby_villager";
    }


    public static class BabyVillagerRecipeSerializer implements RecipeSerializer<BabyVillagerRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        public static final String ID = "baby_villager";
        @Override
        public BabyVillagerRecipe read(Identifier id, JsonObject json) {
            String string = JsonHelper.getString(json, "group", "");
            CraftingRecipeCategory craftingRecipeCategory = CraftingRecipeCategory.CODEC.byId(JsonHelper.getString(json, "category", null), CraftingRecipeCategory.MISC);
            Map<String, Ingredient> map = BabyVillagerRecipe.readSymbols(JsonHelper.getObject(json, "key"));
            String[] strings = BabyVillagerRecipe.removePadding(BabyVillagerRecipe.getPattern(JsonHelper.getArray(json, "pattern")));
            int i = strings[0].length();
            int j = strings.length;
            DefaultedList<Ingredient> defaultedList = BabyVillagerRecipe.createPatternMatrix(strings, map, i, j);
            ItemStack itemStack = ShapedRecipe.outputFromJson(JsonHelper.getObject(json, "result"));
            boolean bl = JsonHelper.getBoolean(json, "show_notification", true);

            return new BabyVillagerRecipe(id, string, craftingRecipeCategory, i, j, defaultedList, itemStack, bl);

        }

        @Override
        public BabyVillagerRecipe read(Identifier id, PacketByteBuf buf) {
            int i = buf.readVarInt();
            int j = buf.readVarInt();
            String string = buf.readString();
            CraftingRecipeCategory craftingRecipeCategory = buf.readEnumConstant(CraftingRecipeCategory.class);
            DefaultedList<Ingredient> defaultedList = DefaultedList.ofSize(i * j, Ingredient.EMPTY);
            for (int k = 0; k < defaultedList.size(); ++k) {
                defaultedList.set(k, Ingredient.fromPacket(buf));
            }
            ItemStack itemStack = buf.readItemStack();
            boolean bl = buf.readBoolean();
            return new BabyVillagerRecipe(id, string, craftingRecipeCategory, i, j, defaultedList, itemStack, bl);
        }

        @Override
        public void write(PacketByteBuf buf, BabyVillagerRecipe recipe) {
            buf.writeVarInt(recipe.getWidth());
            buf.writeVarInt(recipe.getHeight());
            buf.writeString(recipe.getGroup());
            buf.writeEnumConstant(recipe.getCategory());
            for (Ingredient ingredient : recipe.getIngredients()) {
                ingredient.write(buf);
            }
            buf.writeItemStack(recipe.getOutput());
            buf.writeBoolean(recipe.showNotification());
        }

    }
}
