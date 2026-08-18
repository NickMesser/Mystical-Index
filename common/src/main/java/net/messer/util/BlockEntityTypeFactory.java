package net.messer.util;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Builds a {@link BlockEntityType} for a mod block entity.
 *
 * <p>Vanilla no longer offers any way to do this: {@code BlockEntityType.Builder} was removed, the
 * constructor is private, and the vanilla types are all created by a private static. Both loaders
 * reopen it, but by different routes - Fabric through its object-builder API, NeoForge by an access
 * transformer that makes the constructor public - so the one call is made once per loader.
 *
 * <p>{@code BlockEntityType.BlockEntitySupplier} is itself package-private in unmodified Minecraft,
 * which is why the factory below is declared here rather than reusing vanilla's: shared code has no
 * legal way to name that interface. Each loader adapts this one to whatever it needs, and the method
 * reference conversion is exact - the shapes are identical.
 */
public class BlockEntityTypeFactory {

    public interface Factory<T extends BlockEntity> {
        T create(BlockPos pos, BlockState state);
    }

    @ExpectPlatform
    public static <T extends BlockEntity> BlockEntityType<T> create(Factory<T> factory, Block block) {
        throw new AssertionError();
    }
}
