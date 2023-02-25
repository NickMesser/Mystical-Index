package net.messer.mystical_index.item;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.item.custom.*;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {

    public static final Item FLUID_BOOK = registerItem("fluid_book",
            new FluidBook(new FabricItemSettings().maxCount(1)));

    public static final Item STORAGE_BOOK = registerItem("storage_book",
            new StorageBook(new FabricItemSettings().maxCount(1)));

    public static final Item HUSBANDRY_BOOK = registerItem("husbandry_book",
            new HusbandryBook(new FabricItemSettings().maxCount(1)));

    public static final Item SATURATION_BOOK = registerItem("saturation_book",
            new SaturationBook(new FabricItemSettings().maxCount(1)));

    public static final Item MAGNETISM_BOOK = registerItem("magnetism_book",
            new MagnetismBook(new FabricItemSettings().maxCount(1)));

    private static Item registerItem(String name, Item item){
        return Registry.register(Registries.ITEM, new Identifier(MysticalIndex.MOD_ID, name), item);
    }

    public static void registerModItems(){
        MysticalIndex.LOGGER.info("Registering items for " + MysticalIndex.MOD_ID);
    }
}
