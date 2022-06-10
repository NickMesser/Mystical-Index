package net.messer.mystical_index.item;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.item.custom.PageItem;
import net.messer.mystical_index.item.custom.book.*;
import net.messer.mystical_index.item.custom.page.AutoIndexPage;
import net.messer.mystical_index.item.custom.page.StacksPage;
import net.messer.mystical_index.item.custom.page.TypesPage;
import net.messer.mystical_index.util.Colors;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.registry.Registry;

import java.util.List;

public class ModItems {
    public static final Item CUSTOM_BOOK = registerItem("custom_book",
            new CustomInventoryBook(new FabricItemSettings().group(ItemGroup.MISC).maxCount(1)));
    public static final Item CUSTOM_INDEX = registerItem("custom_index",
            new CustomIndexBook(new FabricItemSettings().group(ItemGroup.MISC).maxCount(1)));

    public static final Item STACKS_PAGE = registerItem("stacks_page", new StacksPage());
    public static final Item TYPES_PAGE = registerItem("types_page", new TypesPage());
    public static final Item AUTO_INDEX_PAGE = registerTickingPageItem("auto_index_page", new AutoIndexPage());

    private static Item registerTickingPageItem(String name, Item item) {
        var id = new Identifier(MysticalIndex.MOD_ID, name);
        Registry.register(Registry.ITEM, id, item);
        CustomInventoryBook.registerPage(id, item);
        return item;
    }

    private static Item registerItem(String name, Item item){
        return Registry.register(Registry.ITEM, new Identifier(MysticalIndex.MOD_ID, name), item);
    }

    public static void registerModItems(){
        MysticalIndex.LOGGER.info("Registering items for " + MysticalIndex.MOD_ID);
    }
}
