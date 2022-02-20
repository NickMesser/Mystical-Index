package net.messer.mystical_index.block;

import eu.pb4.polymer.api.item.PolymerBlockItem;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.block.custom.LibraryInventoryBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ModBlocks {

    public static final Block LIBRARY = registerBlock("library",
            new LibraryInventoryBlock(FabricBlockSettings.of(Material.WOOD).strength(1.5f)), Items.BOOKSHELF);

    private static Block registerBlock(String name, Block block, Item polymerItem){
        registerBlockItem(name, block, polymerItem);
        return Registry.register(Registry.BLOCK, new Identifier(MysticalIndex.MOD_ID, name), block);
    }

    private static BlockItem registerBlockItem(String name, Block block, Item polymerItem){
        return Registry.register(Registry.ITEM, new Identifier(MysticalIndex.MOD_ID, name),
                new PolymerBlockItem(block, new FabricItemSettings().group(ItemGroup.MISC).maxCount(64), polymerItem));
    }

    public static void registerModBlocks(){
        MysticalIndex.LOGGER.info("Registering Mod Blocks");
    }

}
