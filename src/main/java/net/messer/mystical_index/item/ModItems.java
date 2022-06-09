package net.messer.mystical_index.item;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.item.custom.PageItem;
import net.messer.mystical_index.item.custom.book.*;
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

//    public static final Item FLUID_BOOK = registerItem("fluid_book",
//            new FluidBook(new FabricItemSettings().group(ItemGroup.MISC).maxCount(1)));

    public static final Item STORAGE_BOOK = registerItem("storage_book",
            new StorageBook(new FabricItemSettings().group(ItemGroup.MISC).maxCount(1)));

    public static final Item SATURATION_BOOK = registerItem("saturation_book",
            new SaturationBook(new FabricItemSettings().group(ItemGroup.MISC).maxCount(1)));

    public static final Item BUILDING_BOOK = registerItem("building_book",
            new BuildingBook(new FabricItemSettings().group(ItemGroup.MISC).maxCount(1)));

//    public static final Item MAGNETISM_BOOK = registerItem("magnetism_book",
//            new MagnetismBook(new FabricItemSettings().group(ItemGroup.MISC).maxCount(1)));

    public static final Item CUSTOM_BOOK = registerItem("custom_book",
            new CustomInventoryBook(new FabricItemSettings().group(ItemGroup.MISC).maxCount(1)));

    public static final Item CUSTOM_INDEX = registerItem("custom_index",
            new CustomIndexBook(new FabricItemSettings().group(ItemGroup.MISC).maxCount(1)));

    public static final Item STACKS_PAGE = registerItem("stacks_page",
            new PageItem() {
                @Override
                public int getStacksIncrease(ItemStack page) {
                    return 1;
                }

                @Override
                public int getRangeIncrease(ItemStack page, boolean autoIndexing) {
                    return autoIndexing ? 2 : 20;
                }

                @Override
                public int getColor() {
                    return 4825911;
                }
            });

    public static final Item TYPES_PAGE = registerItem("types_page",
            new PageItem() {
                @Override
                public int getTypesIncrease(ItemStack page) {
                    return 2;
                }

                @Override
                public int getLinksIncrease(ItemStack page, boolean autoIndexing) {
                    return autoIndexing ? 1 : 2;
                }

                @Override
                public int getColor() {
                    return 4240303;
                }
            });

    public static final Item AUTO_INDEX_PAGE = registerTickingPageItem("auto_index_page",
            new PageItem() {
                @Override
                public ItemStack onCraftToBook(ItemStack page, ItemStack book) {
                    var indexBook = new ItemStack(CUSTOM_INDEX);
                    var nbt = book.getOrCreateNbt();
//                    nbt.putString(CustomIndexBook.INDEXING_TYPE_TAG, CustomIndexBook.INDEXING_TYPE_AUTO_TAG);
                    indexBook.setNbt(nbt);
                    return indexBook;
                }

                @Override
                public int getColor() {
                    return 11745593;
                }

                @Override
                public void appendProperties(ItemStack book, List<Text> properties) {
                    properties.add(new TranslatableText("item.mystical_index.custom_index.tooltip.automatic")
                            .formatted(Formatting.BLUE));

                    if (book != null) {
                        var bookItem = (CustomIndexBook) book.getItem();

                        var linksUsed = bookItem.getLinks(book);
                        var linksMax = bookItem.getMaxLinks(book, true);
                        double linksUsedRatio = (double) linksUsed / linksMax;

                        properties.add(new TranslatableText("item.mystical_index.custom_index.tooltip.links",
                                linksUsed, linksMax)
                                .formatted(Colors.colorByRatio(linksUsedRatio)));
                        properties.add(new TranslatableText("item.mystical_index.custom_index.tooltip.link_range",
                                bookItem.getMaxRange(book, false))
                                .formatted(Colors.colorByRatio(linksUsedRatio)));
                        properties.add(new TranslatableText("item.mystical_index.custom_index.tooltip.range",
                                bookItem.getMaxRange(book, true))
                                .formatted(Formatting.YELLOW));
                    }
                }

                @Override
                public boolean bookCanHaveMultiple(ItemStack page) {
                    return false;
                }

                @Override
                public Rarity getRarity(ItemStack stack) {
                    return Rarity.RARE;
                }
            });

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
