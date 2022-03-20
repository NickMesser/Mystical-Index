package net.messer.mystical_index.block.custom;


import eu.pb4.polymer.api.block.PolymerBlock;
import net.messer.mystical_index.block.ModProperties;
import net.messer.mystical_index.block.entity.LibraryBlockEntity;
import net.messer.mystical_index.util.LecternTracker;
import net.messer.mystical_index.util.request.IIndexInteractable;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("deprecation")
public class LibraryBlock extends BlockWithEntity implements BlockEntityProvider, PolymerBlock {
    public static final IntProperty BOOKS = ModProperties.BOOKS;

    public LibraryBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(BOOKS, 0));
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof Inventory) {
                ItemScatterer.spawn(world, pos, (Inventory)blockEntity);
                world.updateComparators(pos, this);
            }

            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new LibraryBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        } else {
            NamedScreenHandlerFactory screenHandlerFactory = state.createScreenHandlerFactory(world, pos);

            if (screenHandlerFactory != null){
                player.openHandledScreen(screenHandlerFactory);
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        LecternTracker.tryRegisterToLectern((IIndexInteractable) world.getBlockEntity(pos));
    }

    @Override
    public void afterBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack stack) {
        LecternTracker.unRegisterFromLectern((IIndexInteractable) blockEntity);
    }

    @Override
    public Block getPolymerBlock(BlockState state) {
        return Blocks.BOOKSHELF;
    } // TODO use alternative block for empty shelf, loom mb? otherwise think of alternative to shearing
}
