package net.messer.mystical_index.block;

import eu.pb4.polymer.api.block.PolymerBlockUtils;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.block.entity.IndexLecternBlockEntity;
import net.messer.mystical_index.block.entity.LibraryBlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ModBlockEntities {

    public static BlockEntityType<IndexLecternBlockEntity> INDEX_LECTERN_BLOCK_ENTITY;
    public static BlockEntityType<LibraryBlockEntity> LIBRARY_BLOCK_ENTITY;

    public static void registerBlockEntities() {

        LIBRARY_BLOCK_ENTITY =
                Registry.register(Registry.BLOCK_ENTITY_TYPE, new Identifier(MysticalIndex.MOD_ID, "library"),
                        FabricBlockEntityTypeBuilder.create(LibraryBlockEntity::new,
                                ModBlocks.LIBRARY).build(null));

        INDEX_LECTERN_BLOCK_ENTITY =
                Registry.register(Registry.BLOCK_ENTITY_TYPE, new Identifier(MysticalIndex.MOD_ID, "index_lectern"),
                        FabricBlockEntityTypeBuilder.create(IndexLecternBlockEntity::new,
                                ModBlocks.INDEX_LECTERN).build(null));

        PolymerBlockUtils.registerBlockEntity(LIBRARY_BLOCK_ENTITY);
        PolymerBlockUtils.registerBlockEntity(INDEX_LECTERN_BLOCK_ENTITY);
    }
}
