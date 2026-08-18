package net.messer.mystical_index.block.custom;

import net.minecraft.util.RandomSource;
import net.minecraft.core.particles.ParticleTypes;
import com.mojang.serialization.MapCodec;
import net.messer.mystical_index.screen.MysticalLecternScreenHandler;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import net.minecraft.world.level.block.state.BlockBehaviour;

import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class MysticalLecternBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<MysticalLecternBlock> CODEC = simpleCodec(MysticalLecternBlock::new);

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 0, 0, 16, 2, 16),
            Block.box(4, 2, 4, 12, 14, 12),
            Block.box(1, 12, 1, 15, 15, 15));

    public MysticalLecternBlock(BlockBehaviour.Properties settings) {
        super(settings);
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends MysticalLecternBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (world.isClientSide())
            return InteractionResult.SUCCESS;

        player.openMenu(state.getMenuProvider(world, pos));
        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level world, BlockPos pos) {
        return new SimpleMenuProvider(
                (syncId, inventory, player) -> new MysticalLecternScreenHandler(syncId, inventory, ContainerLevelAccess.create(world, pos)),
                Component.translatable("container.mystical_index.mystical_lectern"));
    }

    /**
     * Ambient enchanting glyphs drifting in toward the lectern.
     *
     * <p>Client-only decoration - {@code animateTick} runs on the client for blocks near the
     * camera and the server never hears about it, so nothing here can desync or cost tick time.
     *
     * <p>Built on the enchanting table's idiom, which is what gives the glyphs their inward drift:
     * the particle is spawned AT the block and handed an OUTWARD offset as its velocity, and
     * {@code ENCHANT} reads that as where to start from, so it flies from out there back to here.
     * The rate is deliberately far below vanilla's - an enchanting table rolls this once per
     * surrounding bookshelf every tick, which would be a blizzard on a single block.
     */
    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        super.animateTick(state, world, pos, random);

        // Roughly one visit in three produces anything at all; the rest stay quiet so the effect
        // reads as an occasional shimmer rather than a constant stream.
        if (random.nextInt(3) != 0)
            return;

        int count = 1 + random.nextInt(3);
        for (int i = 0; i < count; i++) {
            // Where the glyph starts, relative to the lectern: a ring roughly a block and a half
            // out, biased to appear at or above the reading surface rather than inside the base.
            double outX = (random.nextDouble() - 0.5) * 3.0;
            double outY = random.nextDouble() * 1.5;
            double outZ = (random.nextDouble() - 0.5) * 3.0;

            world.addParticle(ParticleTypes.ENCHANT,
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    outX, outY, outZ);
        }
    }

}
