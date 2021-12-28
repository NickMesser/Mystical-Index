package net.messer.firstmod.item;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.messer.firstmod.FirstMod;
import net.messer.firstmod.item.custom.StorageBook;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ModItems {

    public static final Item STORAGE_BOOK = registerItem("storage_book",
            new StorageBook(new FabricItemSettings().group(ItemGroup.MISC).maxCount(1)));

    private static Item registerItem(String name, Item item){
        return Registry.register(Registry.ITEM, new Identifier(FirstMod.MOD_ID, name), item);
    }

    public static void registerModItems(){
        FirstMod.LOGGER.info("Registering items for " + FirstMod.MOD_ID);
    }
}
