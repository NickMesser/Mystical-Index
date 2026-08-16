package net.messer.mystical_index.block.entity;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.block.ModBlocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.RegistryKeys;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(MysticalIndex.MOD_ID, RegistryKeys.BLOCK_ENTITY_TYPE);

    // Vanilla's builder rather than Fabric's: the Fabric one only existed to allow a null block
    // list, which this never used. The type has to stay deferred because the block entity reads it
    // in its own constructor, which cannot run before registration has happened.
    public static final RegistrySupplier<BlockEntityType<LibraryBlockEntity>> LIBRARY_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("library", () ->
                    BlockEntityType.Builder.create(LibraryBlockEntity::new, ModBlocks.LIBRARY.get()).build(null));

    public static void registerBlockEntities() {
        BLOCK_ENTITIES.register();
    }
}
