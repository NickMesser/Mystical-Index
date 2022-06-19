package net.messer.mystical_index.item;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.item.custom.book.MysticalBookItem;
import net.messer.mystical_index.item.custom.page.PageItem;
import net.messer.mystical_index.item.custom.page.action.FeedingActionPage;
import net.messer.mystical_index.item.custom.page.attribute.LinksPage;
import net.messer.mystical_index.item.custom.page.attribute.RangePage;
import net.messer.mystical_index.item.custom.page.attribute.StacksPage;
import net.messer.mystical_index.item.custom.page.attribute.TypesPage;
import net.messer.mystical_index.item.custom.page.type.BlockStorageTypePage;
import net.messer.mystical_index.item.custom.page.type.FoodStorageTypePage;
import net.messer.mystical_index.item.custom.page.type.IndexingTypePage;
import net.messer.mystical_index.item.custom.page.type.ItemStorageTypePage;
import net.messer.mystical_index.util.PageRegistry;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ModItems {
    public static final MysticalBookItem MYSTICAL_BOOK = registerItem("mystical_book", new MysticalBookItem(new FabricItemSettings().maxCount(1)));
    public static final Item EMPTY_PAGE = registerItem("empty_page", new Item(new FabricItemSettings()));

    public static final ItemStorageTypePage ITEM_STORAGE_TYPE_PAGE = registerPageItem("item_storage_type_page", new ItemStorageTypePage("item_storage"));
    public static final StacksPage STACKS_PAGE = registerPageItem("stacks_page", new StacksPage("stacks"));
    public static final TypesPage TYPES_PAGE = registerPageItem("types_page", new TypesPage("types"));

    public static final IndexingTypePage INDEXING_TYPE_PAGE = registerPageItem("indexing_type_page", new IndexingTypePage("indexing"));
    public static final RangePage RANGE_PAGE = registerPageItem("range_page", new RangePage("range"));
    public static final LinksPage LINKS_PAGE = registerPageItem("links_page", new LinksPage("links"));


    public static final FoodStorageTypePage FOOD_STORAGE_TYPE_PAGE = registerPageItem("food_storage_type_page", new FoodStorageTypePage("food_storage"));
    public static final FeedingActionPage FEEDING_ACTION_PAGE = registerPageItem("feeding_action_page", new FeedingActionPage("feeding"));
    public static final BlockStorageTypePage BLOCK_STORAGE_TYPE_PAGE = registerPageItem("block_storage_type_page", new BlockStorageTypePage("block_storage"));

    private static <T extends PageItem> T registerPageItem(String name, T item) {
        var id = MysticalIndex.id(name);
        Registry.register(Registry.ITEM, id, item);
        PageRegistry.registerPage(id, item);
        return item;
    }

    private static <T extends Item> T registerItem(String name, T item){
        return Registry.register(Registry.ITEM, new Identifier(MysticalIndex.MOD_ID, name), item);
    }

    public static void registerModItems(){
        MysticalIndex.LOGGER.info("Registering items for " + MysticalIndex.MOD_ID);
    }
}
