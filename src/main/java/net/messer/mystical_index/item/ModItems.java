package net.messer.mystical_index.item;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.item.custom.book.*;
import net.messer.mystical_index.item.custom.page.PageItem;
import net.messer.mystical_index.item.custom.page.type.IndexingTypePage;
import net.messer.mystical_index.item.custom.page.attribute.StacksPage;
import net.messer.mystical_index.item.custom.page.attribute.TypesPage;
import net.messer.mystical_index.item.custom.page.type.ItemStorageTypePage;
import net.messer.mystical_index.util.PageRegistry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ModItems {
    public static final Item CUSTOM_BOOK = registerItem("custom_book",
            new CustomInventoryBook(new FabricItemSettings().group(ItemGroup.MISC).maxCount(1)));
    public static final Item CUSTOM_INDEX = registerItem("custom_index",
            new CustomIndexBook(new FabricItemSettings().group(ItemGroup.MISC).maxCount(1)));

    public static final PageItem ITEM_STORAGE_TYPE_PAGE = registerPageItem("item_storage_type_page", new ItemStorageTypePage());
    public static final PageItem INDEXING_TYPE_PAGE = registerPageItem("indexing_type_page", new IndexingTypePage());

    public static final PageItem STACKS_PAGE = registerPageItem("stacks_page", new StacksPage());
    public static final PageItem TYPES_PAGE = registerPageItem("types_page", new TypesPage());

    private static PageItem registerPageItem(String name, PageItem item) {
        var id = new Identifier(MysticalIndex.MOD_ID, name);
        Registry.register(Registry.ITEM, id, item);
        PageRegistry.registerPage(id, item);
        return item;
    }

    private static Item registerItem(String name, Item item){
        return Registry.register(Registry.ITEM, new Identifier(MysticalIndex.MOD_ID, name), item);
    }

    public static void registerModItems(){
        MysticalIndex.LOGGER.info("Registering items for " + MysticalIndex.MOD_ID);
    }
}
