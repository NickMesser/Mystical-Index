package net.messer.mystical_index.block.entity;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.block.ModBlocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.List;

public class ModBlockEntities {

    public static BlockEntityType<LibraryBlockEntity> LIBRARY_BLOCK_ENTITY;

    public static void registerBlockEntities() {

        LIBRARY_BLOCK_ENTITY =
                Registry.register(Registries.BLOCK_ENTITY_TYPE, new Identifier(MysticalIndex.MOD_ID, "library"),
                        FabricBlockEntityTypeBuilder.create((pos, state) -> new LibraryBlockEntity(pos, state),
                                ModBlocks.LIBRARY).build(null));

        ItemStorage.SIDED.registerForBlockEntity((block, direction) -> switch (direction){
            default -> block.storedContents();
        }, LIBRARY_BLOCK_ENTITY);

    }
}
