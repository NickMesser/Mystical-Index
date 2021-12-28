package net.messer.firstmod.block.entity;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.messer.firstmod.FirstMod;
import net.messer.firstmod.block.ModBlocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ModBlockEntities {
    public static BlockEntityType<LibraryBlockEntity> LIBRARY_BLOCK_ENTITY =
            Registry.register(Registry.BLOCK_ENTITY_TYPE, new Identifier(FirstMod.MOD_ID, "library"),
                    FabricBlockEntityTypeBuilder.create(LibraryBlockEntity::new,
                            ModBlocks.LIBRARY).build(null));
}
