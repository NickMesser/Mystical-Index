package net.messer.mystical_index.block;

import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.block.custom.LibraryInventoryBlock;
import net.messer.mystical_index.block.custom.MysticalLecternBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlocks {

    public static final Block LIBRARY = registerBlock("library",
            new LibraryInventoryBlock(AbstractBlock.Settings.create().strength(1.5f)));

    public static final Block MYSTICAL_LECTERN = registerBlock("mystical_lectern",
            new MysticalLecternBlock(AbstractBlock.Settings.create().strength(2.5f).nonOpaque()));

    private static Block registerBlock(String name, Block block){
        registerBlockItem(name, block);
        return Registry.register(Registries.BLOCK, Identifier.of(MysticalIndex.MOD_ID, name), block);
    }

    private static BlockItem registerBlockItem(String name, Block block){
        return Registry.register(Registries.ITEM, Identifier.of(MysticalIndex.MOD_ID, name),
                new BlockItem(block, new Item.Settings().maxCount(64)));
    }

    public static void registerModBlocks(){
        MysticalIndex.LOGGER.info("Registering Mod Blocks");
    }

}
