package net.messer.util.neoforge;

import net.messer.util.BlockEntityTypeFactory;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class BlockEntityTypeFactoryImpl {

    // NeoForge's access transformer makes the constructor public, so no builder is needed at all.
    public static <T extends BlockEntity> BlockEntityType<T> create(BlockEntityTypeFactory.Factory<T> factory, Block block) {
        return new BlockEntityType<>(factory::create, block);
    }
}
