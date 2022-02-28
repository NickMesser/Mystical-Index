package net.messer.mixin;

import net.messer.mystical_index.events.MixinHooks;
import net.messer.mystical_index.item.custom.Index;
import net.messer.mystical_index.util.LecternTracker;
import net.messer.mystical_index.util.ParticleSystem;
import net.messer.mystical_index.util.request.InsertionRequest;
import net.messer.mystical_index.util.request.LibraryIndex;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.LecternBlock;
import net.minecraft.block.entity.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Objects;

@Mixin(LecternBlock.class)
public abstract class LecternBlockMixin extends BlockWithEntity {
    protected LecternBlockMixin(Settings settings) {
        super(settings);
    }

    @ModifyVariable(
            method = "dropBook",
            at = @At(value = "STORE")
    )
    public ItemStack modifyDroppedItem(ItemStack i) {
        return Index.getFromLecternBook(i);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return checkType(type, BlockEntityType.LECTERN, MixinHooks::lecternTick);
    }

    @Override
    public void afterBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack stack) {
        if (blockEntity instanceof LecternBlockEntity lectern) {
            LecternTracker.removeIndexLectern(lectern);
        }
        super.afterBreak(world, player, pos, state, blockEntity, stack);
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (entity instanceof ItemEntity itemEntity &&
                !Objects.equals(itemEntity.getThrower(), Index.EXTRACTED_DROP_UUID) &&
                VoxelShapes.matchesAnywhere(VoxelShapes.cuboid(
                        entity.getBoundingBox().offset(-pos.getX(), -pos.getY(), -pos.getZ())),
                        MixinHooks.LECTERN_INPUT_AREA_SHAPE, BooleanBiFunction.AND)) {

            ItemStack itemStack = itemEntity.getStack();

            LibraryIndex index = LibraryIndex.get(world, pos, LibraryIndex.LECTERN_SEARCH_RANGE);

            InsertionRequest request = new InsertionRequest(itemStack);
            request.setSourcePosition(Vec3d.ofCenter(pos));
            request.setBlockAffectedCallback(ParticleSystem::insertionParticles);

            index.insertStack(request);
        }
    }
}
