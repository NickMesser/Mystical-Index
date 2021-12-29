package net.messer.mystical_index.block.entity;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.block.ModBlocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ModBlockEntities {

    public static BlockEntityType<LibraryBlockEntity> LIBRARY_BLOCK_ENTITY;
    public static BlockEntityType<MagicalIndexBlockEntity> MAGICAL_INDEX_BLOCK_ENTITY;

    public static void registerBlockEntities(){

        MAGICAL_INDEX_BLOCK_ENTITY =
                Registry.register(Registry.BLOCK_ENTITY_TYPE, new Identifier(MysticalIndex.MOD_ID, "magical_index"),
                        FabricBlockEntityTypeBuilder.create(MagicalIndexBlockEntity::new,
                                ModBlocks.MAGICAL_INDEX).build(null));

        LIBRARY_BLOCK_ENTITY =
                Registry.register(Registry.BLOCK_ENTITY_TYPE, new Identifier(MysticalIndex.MOD_ID, "library"),
                        FabricBlockEntityTypeBuilder.create(LibraryBlockEntity::new,
                                ModBlocks.LIBRARY).build(null));
    }
}
