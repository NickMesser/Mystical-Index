package net.messer.mystical_index.event;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.messer.mystical_index.block.ModBlocks;
import net.messer.mystical_index.block.entity.MysticalLecternBlockEntity;
import net.messer.mystical_index.block.entity.LibraryBlockEntity;
import net.messer.mystical_index.util.LecternTracker;
import net.messer.mystical_index.util.WorldEffects;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class EventListeners {
    public static void register() {
        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register(EventListeners::onLoadBlockEntity);
        ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register(EventListeners::onUnloadBlockEntity);
        UseBlockCallback.EVENT.register(EventListeners::onBlockInteract);
    }

    private static void onLoadBlockEntity(BlockEntity blockEntity, ServerWorld world) {
        if (blockEntity instanceof LibraryBlockEntity library) {
            LecternTracker.tryRegisterToLectern(library, false);
        }
    }

    private static void onUnloadBlockEntity(BlockEntity blockEntity, ServerWorld world) {
        if (blockEntity instanceof MysticalLecternBlockEntity lectern) {
            LecternTracker.removeIndexLectern(lectern);
        }
    }

    private static ActionResult onBlockInteract(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {
        BlockPos blockPos = hitResult.getBlockPos();
        BlockState blockState = world.getBlockState(blockPos);
        ItemStack itemStack = player.getStackInHand(hand);

        if (blockState.isOf(Blocks.BOOKSHELF) && itemStack.getItem().equals(Items.AMETHYST_SHARD)) {
            world.setBlockState(blockPos, ModBlocks.LIBRARY.getDefaultState());
            itemStack.decrement(1);
            WorldEffects.blockParticles(world, blockPos, ParticleTypes.FLAME);

            return ActionResult.success(true);
        }

        return ActionResult.PASS;
    }
}
