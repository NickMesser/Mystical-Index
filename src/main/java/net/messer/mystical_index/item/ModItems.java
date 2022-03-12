package net.messer.mystical_index.item;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.item.custom.PageItem;
import net.messer.mystical_index.item.custom.book.*;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

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

    public static final Item INDEX = registerItem("index",
            new Index(new FabricItemSettings().group(ItemGroup.MISC).maxCount(1)));

    public static final Item STACKS_PAGE = registerItem("stacks_page",
            new PageItem() {
                @Override
                public int getStacksIncrease(ItemStack page) {
                    return 1;
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
                public int getColor() {
                    return 4240303;
                }
            });

    public static final Item INDEX_PAGE = registerItem("index_page",
            new PageItem() {
                @Override
                public int getColor() {
                    return 11745593;
                }

                @Override
                public void appendProperties(ItemStack stack, List<Text> properties) {
                    properties.add(new TranslatableText("item.mystical_index.page.tooltip.properties.index")
                            .formatted(Formatting.DARK_RED));
                    super.appendProperties(stack, properties);
                }
            });

    private static Item registerItem(String name, Item item){
        return Registry.register(Registry.ITEM, new Identifier(MysticalIndex.MOD_ID, name), item);
    }

    public static void registerModItems(){
        MysticalIndex.LOGGER.info("Registering items for " + MysticalIndex.MOD_ID);
    }
}
