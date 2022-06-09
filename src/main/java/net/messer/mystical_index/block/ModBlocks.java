package net.messer.mystical_index.block;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.block.custom.IndexLecternBlock;
import net.messer.mystical_index.block.custom.LibraryBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ModBlocks {

    public static final Block LIBRARY = registerBlock("library",
            new LibraryBlock(FabricBlockSettings.of(Material.WOOD).strength(1.5f)), true);

    public static final Block INDEX_LECTERN = registerBlock("index_lectern",
            new IndexLecternBlock(FabricBlockSettings.of(Material.WOOD).strength(1.5f)), false);


    private static Block registerBlock(String name, Block block, boolean hasItem) {
        if (hasItem) {
            registerBlockItem(name, block);
        }
        return Registry.register(Registry.BLOCK, new Identifier(MysticalIndex.MOD_ID, name), block);
    }

    private static BlockItem registerBlockItem(String name, Block block) {
        return Registry.register(Registry.ITEM, new Identifier(MysticalIndex.MOD_ID, name),
                new BlockItem(block, new FabricItemSettings().group(ItemGroup.MISC).maxCount(64)));
    }

    public static void registerModBlocks(){
        MysticalIndex.LOGGER.info("Registering blocks for " + MysticalIndex.MOD_ID);
    }
}
