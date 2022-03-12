package net.messer.mystical_index.block.custom;

import eu.pb4.polymer.api.block.PolymerBlock;
import net.messer.mystical_index.block.ModBlockEntities;
import net.messer.mystical_index.block.entity.IndexLecternBlockEntity;
import net.messer.mystical_index.events.MixinHooks;
import net.messer.mystical_index.item.custom.book.CustomIndexBook;
import net.messer.mystical_index.util.LecternTracker;
import net.messer.mystical_index.util.ParticleSystem;
import net.messer.mystical_index.util.request.InsertionRequest;
import net.messer.mystical_index.util.request.LibraryIndex;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tag.ItemTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@SuppressWarnings("deprecation")
public class IndexLecternBlock extends LecternBlock implements PolymerBlock {
    public IndexLecternBlock(Settings settings) {
        super(settings);
    }

    @Override
    public Block getPolymerBlock(BlockState state) {
        return Blocks.LECTERN;
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state) {
        return getPolymerBlock(state).getStateWithProperties(state);
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
                !Objects.equals(itemEntity.getThrower(), CustomIndexBook.EXTRACTED_DROP_UUID) &&
                VoxelShapes.matchesAnywhere(VoxelShapes.cuboid(
                                entity.getBoundingBox().offset(-pos.getX(), -pos.getY(), -pos.getZ())),
                        MixinHooks.LECTERN_INPUT_AREA_SHAPE, BooleanBiFunction.AND)) {

            ItemStack itemStack = itemEntity.getStack();

            LibraryIndex index = LibraryIndex.get(world, pos, LibraryIndex.LECTERN_SEARCH_RANGE); // TODO changes here once index can store library positions

            InsertionRequest request = new InsertionRequest(itemStack);
            request.setSourcePosition(Vec3d.ofCenter(pos));
            request.setBlockAffectedCallback(ParticleSystem::insertionParticles);

            index.insertStack(request);
        }
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (state.get(HAS_BOOK)) {
            var blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof IndexLecternBlockEntity lecternBlockEntity) {
                var book = lecternBlockEntity.getBook();
                player.getInventory().offerOrDrop(book);

                world.setBlockState(pos, Blocks.LECTERN.getStateWithProperties(state).with(LecternBlock.HAS_BOOK, false));

                return ActionResult.success(world.isClient);
            }
            return ActionResult.CONSUME;
        }
        ItemStack itemStack = player.getStackInHand(hand);
        if (itemStack.isEmpty() || itemStack.isIn(ItemTags.LECTERN_BOOKS)) {
            return ActionResult.PASS;
        }
        return ActionResult.CONSUME;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new IndexLecternBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return checkType(type, ModBlockEntities.INDEX_LECTERN_BLOCK_ENTITY, IndexLecternBlockEntity::serverTick);
    }
}
