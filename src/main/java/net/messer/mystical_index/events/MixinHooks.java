package net.messer.mystical_index.events;

import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.item.custom.Index;
import net.messer.mystical_index.util.LecternTracker;
import net.messer.mystical_index.util.ParticleSystem;
import net.minecraft.block.BlockState;
import net.minecraft.block.LecternBlock;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

public class MixinHooks {
    private static final int LECTERN_CIRCLE_PERIOD = 200;
    private static final int LECTERN_CIRCLE_INTERVAL = 2;

    public static boolean interceptPickup(PlayerInventory playerInventory, ItemStack itemPickedUp) {
        var player = playerInventory.player;

        if(MysticalIndex.CONFIG.BookOfStorage.BlockBlacklist.contains(Registry.ITEM.getId(itemPickedUp.getItem())) || player.world.isClient()){
            return false;
        }

        //TODO
//        if(itemPickedUp.isFood()){
//            for (int i = 0; i < playerInventory.size(); i++) {
//                var potentialBook = playerInventory.getStack(i);
//                if (potentialBook.getItem() == ModItems.SATURATION_BOOK) {
//                    var bookInventory = new SingleItemStackingInventory(potentialBook, 1);
//                    if (bookInventory.currentlyStoredItem == itemPickedUp.getItem()) {
//                        var itemToAdd = bookInventory.addStack(itemPickedUp);
//                        if (itemToAdd.isEmpty()) {
//                            return true;
//                        } else {
//                            itemPickedUp.setCount(itemToAdd.getCount());
//                        }
//                    }
//                }
//            }
//            return false;
//        }
//
//        if(itemPickedUp.getItem() instanceof BlockItem){
//            for (int i = 0; i < playerInventory.size(); i++) {
//                var potentialBook = playerInventory.getStack(i);
//                if (potentialBook.getItem() == ModItems.STORAGE_BOOK) {
//                    var bookInventory = new SingleItemStackingInventory(potentialBook, 1);
//                    if (bookInventory.currentlyStoredItem == itemPickedUp.getItem()) {
//                        var itemToAdd = bookInventory.addStack(itemPickedUp);
//                        if (itemToAdd.isEmpty()) {
//                            return true;
//                        } else {
//                            itemPickedUp.setCount(itemToAdd.getCount());
//                        }
//                    }
//                }
//            }
//        }
        return false;
    }

    private static boolean lecternContainsIndex(LecternBlockEntity blockEntity) {
        return blockEntity.getBook().getOrCreateNbt().contains(Index.LECTERN_TAG_NAME);
    }

    public static void lecternTick(World world, BlockPos pos, BlockState state, LecternBlockEntity be) {
        if (world instanceof ServerWorld serverWorld) {
            if (state.get(LecternBlock.HAS_BOOK) && lecternContainsIndex(be)) {
                if (
                        serverWorld.getServer().getTicks() % LECTERN_CIRCLE_INTERVAL == 0 &&
                        serverWorld.getClosestPlayer(
                                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                                Index.LECTERN_PICKUP_RADIUS, false
                        ) != null
                ) {
                    Vec3d centerPos = Vec3d.ofCenter(pos, 0.5);
                    ParticleSystem.drawParticleCircle(
                            serverWorld, centerPos, LECTERN_CIRCLE_PERIOD,
                            0, Index.LECTERN_PICKUP_RADIUS
                    );
                    ParticleSystem.drawParticleCircle(
                            serverWorld, centerPos, -LECTERN_CIRCLE_PERIOD,
                            0, Index.LECTERN_PICKUP_RADIUS
                    );
                    ParticleSystem.drawParticleCircle(
                            serverWorld, centerPos, LECTERN_CIRCLE_PERIOD,
                            LECTERN_CIRCLE_PERIOD / 2, Index.LECTERN_PICKUP_RADIUS
                    );
                    ParticleSystem.drawParticleCircle(
                            serverWorld, centerPos, -LECTERN_CIRCLE_PERIOD,
                            LECTERN_CIRCLE_PERIOD / 2, Index.LECTERN_PICKUP_RADIUS
                    );
                }

                LecternTracker.addIndexLectern(be);
            } else {
                LecternTracker.removeIndexLectern(be);
            }
        }
    }
}
