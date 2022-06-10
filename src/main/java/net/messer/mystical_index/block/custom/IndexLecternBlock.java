package net.messer.mystical_index.block.custom;

import net.messer.mystical_index.block.ModBlockEntities;
import net.messer.mystical_index.block.entity.IndexLecternBlockEntity;
import net.messer.mystical_index.client.Particles;
import net.messer.mystical_index.util.LecternTracker;
import net.messer.mystical_index.util.WorldEffects;
import net.messer.mystical_index.util.request.InsertionRequest;
import net.messer.mystical_index.util.request.LibraryIndex;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LecternBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.tag.ItemTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Random;

@SuppressWarnings("deprecation")
public class IndexLecternBlock extends LecternBlock {
    private static final VoxelShape LECTERN_INSIDE_SHAPE = Block.createCuboidShape(2.0, 11.0, 2.0, 14.0, 16.0, 14.0);
    private static final VoxelShape LECTERN_ABOVE_SHAPE = Block.createCuboidShape(0.0, 16.0, 0.0, 16.0, 32.0, 16.0);
    public static final VoxelShape LECTERN_INPUT_AREA_SHAPE = VoxelShapes.union(LECTERN_INSIDE_SHAPE, LECTERN_ABOVE_SHAPE);

    public IndexLecternBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void afterBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack stack) {
        if (blockEntity instanceof IndexLecternBlockEntity lectern) {
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
                        LECTERN_INPUT_AREA_SHAPE, BooleanBiFunction.AND) &&
                world instanceof ServerWorld serverWorld &&
                serverWorld.getBlockEntity(pos) instanceof IndexLecternBlockEntity lectern) {

            ItemStack itemStack = itemEntity.getStack();

            LibraryIndex index = lectern.getLinkedLibraries();

            InsertionRequest request = new InsertionRequest(itemStack);
            request.setSourcePosition(Vec3d.ofCenter(pos));
            request.setBlockAffectedCallback(WorldEffects::insertionParticles);

            index.insertStack(request);

            if (request.hasAffected()) {
                var sourcePos = entity.getPos();

                serverWorld.playSound(null, sourcePos.getX(), sourcePos.getY(), sourcePos.getZ(),
                        SoundEvents.BLOCK_AMETHYST_BLOCK_STEP, SoundCategory.BLOCKS,
                        0.5f, 0.6f + world.getRandom().nextFloat() * 0.4f);
                serverWorld.spawnParticles(
                        ParticleTypes.SOUL_FIRE_FLAME, sourcePos.getX(), sourcePos.getY(), sourcePos.getZ(),
                        5, 0, 0, 0, 0.1);
            }
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

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        super.randomDisplayTick(state, world, pos, random);

        Particles.spawnParticles(
                world, Vec3d.ofCenter(pos).add(0, 0.7, 0), ParticleTypes.SOUL_FIRE_FLAME,
                0.3, 0.3, 0.3, UniformIntProvider.create(1, 1), 0);
    }
}
