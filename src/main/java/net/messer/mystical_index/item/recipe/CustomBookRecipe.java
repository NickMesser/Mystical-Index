package net.messer.mystical_index.item.recipe;

import com.google.common.collect.Maps;
import net.messer.mystical_index.item.ModItems;
import net.messer.mystical_index.item.ModRecipes;
import net.messer.mystical_index.item.custom.book.BookItem;
import net.messer.mystical_index.item.custom.page.ActionPageItem;
import net.messer.mystical_index.item.custom.page.AttributePageItem;
import net.messer.mystical_index.item.custom.page.PageItem;
import net.messer.mystical_index.item.custom.book.CustomIndexBook;
import net.messer.mystical_index.item.custom.book.CustomInventoryBook;
import net.messer.mystical_index.item.custom.page.TypePageItem;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtString;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Map;

public class CustomBookRecipe extends SpecialCraftingRecipe {
    private static final Ingredient BINDING = Ingredient.ofItems(Items.LEATHER);
    // Defines how many pages are supported by each catalyst item.
    private static final Map<Item, Integer> CATALYSTS = Util.make(Maps.newHashMap(), hashMap -> {
        hashMap.put(Items.AMETHYST_SHARD, 2);
        hashMap.put(Items.EMERALD, 4);
        hashMap.put(Items.DIAMOND, 6);
    });
    private static final Ingredient CATALYST = Ingredient.ofItems(CATALYSTS.keySet().toArray(new Item[0]));

    public CustomBookRecipe(Identifier identifier) {
        super(identifier);
    }

    @Override
    public boolean matches(CraftingInventory craftingInventory, World world) {
        var binding = false;
        var catalyst = 0;
        TypePageItem typePage = null;
        ActionPageItem actionPage = null;
        var pages = new ArrayList<PageItem>();

        // Check binding and catalyst, and store type page.
        for (int i = 0; i < craftingInventory.size(); ++i) {
            var itemStack = craftingInventory.getStack(i);
            if (itemStack.isEmpty()) continue;
            if (BINDING.test(itemStack)) {
                if (binding) {
                    return false;
                }
                binding = true;
                continue;
            }
            if (CATALYST.test(itemStack)) {
                if (catalyst > 0) {
                    return false;
                }
                catalyst = CATALYSTS.get(itemStack.getItem());
                continue;
            }
            if (itemStack.getItem() instanceof TypePageItem page) {
                if (typePage != null) {
                    return false;
                }
                typePage = page;
                pages.add(page);
                continue;
            }
            if (itemStack.getItem() instanceof PageItem) {
                continue;
            }
            return false;
        }

        // Store action page.
        for (int i = 0; i < craftingInventory.size(); ++i) {
            var itemStack = craftingInventory.getStack(i);
            if (itemStack.isEmpty()) continue;
            if (itemStack.getItem() instanceof ActionPageItem page) {
                if (actionPage != null) {
                    return false;
                }
                actionPage = page;
                pages.add(page);
            }
        }

        // Get attribute pages and check if all pages are compatible.
        for (int i = 0; i < craftingInventory.size(); ++i) {
            var itemStack = craftingInventory.getStack(i);
            if (itemStack.isEmpty()) continue;
            if (itemStack.getItem() instanceof AttributePageItem page) {
                if (!page.getCompatibleTypes(itemStack).contains(typePage)) {
                    return false;
                }
                var incompatiblePages = page.getIncompatibleAttributes(itemStack);
                if (!page.bookCanHaveMultiple(itemStack) && (pages.contains(page) ||
                        pages.stream().anyMatch(i1 -> incompatiblePages.stream().anyMatch(i1::equals)))) {
                    return false;
                }
                pages.add(page);
            }
        }

        // Return true if all requirements are met.
        return binding && catalyst > 0 && typePage != null && pages.size() <= catalyst;
    }

    @Override
    public ItemStack craft(CraftingInventory craftingInventory) {
        var book = new ItemStack(ModItems.CUSTOM_BOOK);
        var nbt = book.getOrCreateNbt();

        for (int i = 0; i < craftingInventory.size(); ++i) {
            var stack = craftingInventory.getStack(i);
            if (stack.getItem() instanceof TypePageItem pageItem) {
                pageItem.onCraftToBook(stack, book);
                nbt.put(BookItem.TYPE_PAGE_TAG, NbtString.of(Registry.ITEM.getId(pageItem).toString()));
                break;
            }
        }

        var pagesList = nbt.getList(BookItem.ATTRIBUTE_PAGES_TAG, NbtElement.STRING_TYPE);
        for (int i = 0; i < craftingInventory.size(); ++i) {
            var stack = craftingInventory.getStack(i);
            if (stack.getItem() instanceof AttributePageItem pageItem) {
                pageItem.onCraftToBook(stack, book);
                pagesList.add(NbtString.of(Registry.ITEM.getId(pageItem).toString()));
            }
        }

        for (int i = 0; i < craftingInventory.size(); ++i) {
            var stack = craftingInventory.getStack(i);
            if (stack.getItem() instanceof ActionPageItem pageItem) {
                pageItem.onCraftToBook(stack, book);
                nbt.put(BookItem.ACTION_PAGE_TAG, NbtString.of(Registry.ITEM.getId(pageItem).toString()));
                break;
            }
        }

        return book;
    }

    @Override
    public boolean fits(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getOutput() {
        return new ItemStack(ModItems.CUSTOM_BOOK);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.CUSTOM_BOOK;
    }
}
