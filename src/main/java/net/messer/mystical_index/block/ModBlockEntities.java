package net.messer.mystical_index.block;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.block.entity.MysticalLecternBlockEntity;
import net.messer.mystical_index.block.entity.LibraryBlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.registry.Registry;

public class ModBlockEntities {

    public static BlockEntityType<MysticalLecternBlockEntity> MYSTICAL_LECTERN_BLOCK_ENTITY;
    public static BlockEntityType<LibraryBlockEntity> LIBRARY_BLOCK_ENTITY;

    public static void registerBlockEntities() {
        LIBRARY_BLOCK_ENTITY =
                Registry.register(Registry.BLOCK_ENTITY_TYPE, MysticalIndex.id("library"),
                        FabricBlockEntityTypeBuilder.create(LibraryBlockEntity::new,
                                ModBlocks.LIBRARY).build(null));

        MYSTICAL_LECTERN_BLOCK_ENTITY =
                Registry.register(Registry.BLOCK_ENTITY_TYPE, MysticalIndex.id("mystical_lectern"),
                        FabricBlockEntityTypeBuilder.create(MysticalLecternBlockEntity::new,
                                ModBlocks.MYSTICAL_LECTERN).build(null));
    }
}
