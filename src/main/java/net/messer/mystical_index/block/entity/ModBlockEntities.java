package net.messer.mystical_index.block.entity;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.block.ModBlocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntities {

    public static BlockEntityType<LibraryBlockEntity> LIBRARY_BLOCK_ENTITY;

    public static void registerBlockEntities() {

        LIBRARY_BLOCK_ENTITY =
                Registry.register(Registries.BLOCK_ENTITY_TYPE, new Identifier(MysticalIndex.MOD_ID, "library"),
                        FabricBlockEntityTypeBuilder.create(LibraryBlockEntity::new,
                                ModBlocks.LIBRARY).build(null));
    }
}
