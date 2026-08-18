package net.messer.mystical_index.block.custom;

import com.mojang.serialization.MapCodec;
import net.messer.mystical_index.block.entity.ModBlockEntities;
import net.messer.mystical_index.block.entity.ScriptoriumBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.messer.mystical_index.screen.ScriptoriumScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.jetbrains.annotations.Nullable;

/**
 * A workbench made of books: the Scriptorium holds effect books and runs them at the block.
 *
 * <p>This sub-slice is the block itself only - it places, faces the player, drops itself, and hosts
 * an empty block entity. The book slots, the screen and the ticking arrive in the sub-slices after
 * this one, so nothing here is half-wired: there is simply nothing inside it yet.
 */
public class ScriptoriumBlock extends BaseEntityBlock implements EntityBlock {

    public static final MapCodec<ScriptoriumBlock> CODEC = simpleCodec(ScriptoriumBlock::new);

    // Reuse vanilla's own property object rather than declaring a second one - the blockstate json
    // keys on the property NAME, and two separate instances would silently not match.
    public static final net.minecraft.world.level.block.state.properties.EnumProperty<Direction> FACING =
            HorizontalDirectionalBlock.FACING;

    public ScriptoriumBlock(Properties settings) {
        super(settings);
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends ScriptoriumBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /**
     * Spills the stored books when the block goes. The block entity holds a container rather than
     * being one, so the container is what gets dropped - testing the block entity itself drops
     * nothing, which is how the Library once ate books on break.
     */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel world, BlockPos pos, boolean moved) {
        if (world.getBlockEntity(pos) instanceof ScriptoriumBlockEntity scriptorium) {
            Containers.dropContents(world, pos, scriptorium.storedBooks);
            world.updateNeighbourForOutputSignal(pos, this);
        }

        super.affectNeighborsAfterRemoval(state, world, pos, moved);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state,
                                                                 BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.SCRIPTORIUM_BLOCK_ENTITY.get(),
                ScriptoriumBlockEntity::tick);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ScriptoriumBlockEntity(pos, state);
    }

    @Nullable
    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level world, BlockPos pos) {
        // ContainerLevelAccess rather than a position payload: it is what stillValid tests against,
        // and it is the shape every other block menu in the mod already uses.
        return world.getBlockEntity(pos) instanceof ScriptoriumBlockEntity scriptorium
                ? new SimpleMenuProvider(
                        (syncId, inventory, player) -> new ScriptoriumScreenHandler(
                                syncId, inventory, scriptorium.storedBooks,
                                ContainerLevelAccess.create(world, pos)),
                        Component.translatable("container.mystical_index.scriptorium"))
                : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (world.isClientSide())
            return InteractionResult.SUCCESS;

        var provider = state.getMenuProvider(world, pos);
        if (provider != null)
            player.openMenu(provider);

        return InteractionResult.SUCCESS;
    }

    // A normal block model rather than a block entity renderer - the books live in the screen, not
    // on the surface.
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
