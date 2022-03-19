package net.messer.mystical_index.item.recipe;

import com.google.common.collect.Maps;
import eu.pb4.polymer.api.item.PolymerRecipe;
import net.messer.mystical_index.item.ModItems;
import net.messer.mystical_index.item.ModRecipes;
import net.messer.mystical_index.item.custom.PageItem;
import net.messer.mystical_index.item.custom.book.CustomIndexBook;
import net.messer.mystical_index.item.custom.book.CustomInventoryBook;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtString;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Map;

public class CustomBookRecipe extends SpecialCraftingRecipe implements PolymerRecipe {
    private static final Ingredient BINDING = Ingredient.ofItems(Items.LEATHER);
    private static final Map<Item, Integer> CATALYSTS = Util.make(Maps.newHashMap(), hashMap -> {
        hashMap.put(Items.AMETHYST_SHARD, 2);
        hashMap.put(Items.EMERALD, 4);
        hashMap.put(Items.DIAMOND, 6);
    }); // Defines how many pages are supported by each catalyst item.
    private static final Ingredient CATALYST = Ingredient.ofItems(CATALYSTS.keySet().toArray(new Item[0]));

    public CustomBookRecipe(Identifier identifier) {
        super(identifier);
    }

    @Override
    public boolean matches(CraftingInventory craftingInventory, World world) {
        var binding = false;
        var catalyst = 0;
        var pages = new ArrayList<PageItem>();
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
            if (itemStack.getItem() instanceof PageItem page) {
                var incompatiblePages = page.incompatiblePages(itemStack);
                if (!page.bookCanHaveMultiple(itemStack) && (pages.contains(page) ||
                        pages.stream().anyMatch(i1 -> incompatiblePages.stream().anyMatch(i1::equals)))) {
                    return false;
                }
                pages.add(page);
                continue;
            }
            return false;
        }
        return binding && catalyst > 0 && pages.size() <= catalyst;
    }

    @Override
    public ItemStack craft(CraftingInventory craftingInventory) {
        var book = new ItemStack(ModItems.CUSTOM_BOOK);
        var nbt = book.getOrCreateNbt();
        var pagesList = nbt.getList(CustomInventoryBook.PAGES_TAG, NbtElement.STRING_TYPE);

        nbt.putInt(CustomInventoryBook.MAX_STACKS_TAG, ((PageItem) ModItems.STACKS_PAGE).getStacksIncrease(null));
        nbt.putInt(CustomInventoryBook.MAX_TYPES_TAG, ((PageItem) ModItems.TYPES_PAGE).getTypesIncrease(null));

        var autoIndexing = true;
        do {
            autoIndexing = !autoIndexing;
            var subTag = nbt.getCompound(autoIndexing ? CustomIndexBook.AUTO_INDEXING_TAG : CustomIndexBook.MANUAL_INDEXING_TAG);

            subTag.putInt(CustomIndexBook.MAX_RANGE_TAG, ((PageItem) ModItems.STACKS_PAGE).getRangeIncrease(null, autoIndexing));
            subTag.putInt(CustomIndexBook.MAX_LINKS_TAG, ((PageItem) ModItems.TYPES_PAGE).getLinksIncrease(null, autoIndexing));

            nbt.put(autoIndexing ? CustomIndexBook.AUTO_INDEXING_TAG : CustomIndexBook.MANUAL_INDEXING_TAG, subTag);
        } while (!autoIndexing);

        for (int i = 0; i < craftingInventory.size(); ++i) {
            var stack = craftingInventory.getStack(i);
            if (stack.getItem() instanceof PageItem pageItem) {
                pagesList.add(NbtString.of(Registry.ITEM.getId(pageItem).toString()));
                book = pageItem.onCraftToBook(stack, book);
            }
        }

        nbt.put(CustomInventoryBook.PAGES_TAG, pagesList);
        if (book.getItem() instanceof CustomIndexBook) {
            nbt.remove(CustomInventoryBook.MAX_STACKS_TAG);
            nbt.remove(CustomInventoryBook.MAX_TYPES_TAG);
        } else {
            nbt.remove(CustomIndexBook.MANUAL_INDEXING_TAG);
            nbt.remove(CustomIndexBook.AUTO_INDEXING_TAG);
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

    @Override
    public @Nullable Recipe<?> getPolymerRecipe(Recipe<?> input) {
        return null;
    }
}
