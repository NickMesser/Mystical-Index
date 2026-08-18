package net.messer.mystical_index.block.entity;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.block.ModBlocks;
import net.messer.util.BlockEntityTypeFactory;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.registries.Registries;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(MysticalIndex.MOD_ID, Registries.BLOCK_ENTITY_TYPE);

    // Vanilla has no public way to build a block entity type any more, so this goes through the
    // per-loader shim. The type has to stay deferred because the block entity reads it in its own
    // constructor, which cannot run before registration has happened.
    public static final RegistrySupplier<BlockEntityType<LibraryBlockEntity>> LIBRARY_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("library", () ->
                    BlockEntityTypeFactory.create(LibraryBlockEntity::new, ModBlocks.LIBRARY.get()));

    public static final RegistrySupplier<BlockEntityType<ScriptoriumBlockEntity>> SCRIPTORIUM_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("scriptorium", () ->
                    BlockEntityTypeFactory.create(ScriptoriumBlockEntity::new, ModBlocks.SCRIPTORIUM.get()));

    public static void registerBlockEntities() {
        BLOCK_ENTITIES.register();
    }
}
