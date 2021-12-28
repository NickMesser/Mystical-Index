package net.messer.firstmod.block;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.messer.firstmod.FirstMod;
import net.messer.firstmod.block.custom.CardCatalogBlock;
import net.messer.firstmod.block.entity.LibraryBlockEntity;
import net.messer.firstmod.block.custom.LibraryInventoryBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ModBlocks {

    public static final Block LIBRARY = registerBlock("library",
            new LibraryInventoryBlock(FabricBlockSettings.of(Material.WOOD)));

    public static final Block CARD_CATALOG = registerBlock("card_catalog",
            new CardCatalogBlock(FabricBlockSettings.of(Material.WOOD)));

    private static Block registerBlock(String name, Block block){
        registerBlockItem(name, block);
        return Registry.register(Registry.BLOCK, new Identifier(FirstMod.MOD_ID, name), block);
    }

    private static BlockItem registerBlockItem(String name, Block block){
        return Registry.register(Registry.ITEM, new Identifier(FirstMod.MOD_ID, name),
                new BlockItem(block, new FabricItemSettings().group(ItemGroup.MISC)));
    }

    public static void registerModBlocks(){
        FirstMod.LOGGER.info("Registering Mod Blocks");
    }

}
