package net.messer.mystical_index.block;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.block.custom.LibraryInventoryBlock;
import net.messer.mystical_index.block.custom.TestInventoryBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlocks {

    public static final Block LIBRARY = registerBlock("library",
            new LibraryInventoryBlock(FabricBlockSettings.create().strength(1.5f)));

    public static final Block TEST = registerBlock("test",
            new TestInventoryBlock(FabricBlockSettings.create().strength(1.5f)));

    private static Block registerBlock(String name, Block block){
        registerBlockItem(name, block);
        return Registry.register(Registries.BLOCK, new Identifier(MysticalIndex.MOD_ID, name), block);
    }

    private static void registerBlockItem(String name, Block block){
        Registry.register(Registries.ITEM, new Identifier(MysticalIndex.MOD_ID, name),
                new BlockItem(block, new FabricItemSettings().maxCount(64)));
    }

    public static void registerModBlocks(){
        MysticalIndex.LOGGER.info("Registering Mod Blocks");
    }

}
