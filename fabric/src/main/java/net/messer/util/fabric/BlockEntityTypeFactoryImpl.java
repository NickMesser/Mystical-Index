package net.messer.util.fabric;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.messer.util.BlockEntityTypeFactory;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class BlockEntityTypeFactoryImpl {

    public static <T extends BlockEntity> BlockEntityType<T> create(BlockEntityTypeFactory.Factory<T> factory, Block block) {
        return FabricBlockEntityTypeBuilder.<T>create(factory::create, block).build();
    }
}
