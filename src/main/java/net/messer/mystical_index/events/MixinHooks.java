package net.messer.mystical_index.events;

import net.messer.mystical_index.MysticalIndex;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

public class MixinHooks {

    private static final VoxelShape LECTERN_INSIDE_SHAPE = Block.createCuboidShape(2.0, 11.0, 2.0, 14.0, 16.0, 14.0);
    private static final VoxelShape LECTERN_ABOVE_SHAPE = Block.createCuboidShape(0.0, 16.0, 0.0, 16.0, 32.0, 16.0);
    public static final VoxelShape LECTERN_INPUT_AREA_SHAPE = VoxelShapes.union(LECTERN_INSIDE_SHAPE, LECTERN_ABOVE_SHAPE);

    public static boolean interceptPickup(PlayerInventory playerInventory, ItemStack itemPickedUp) {
        var player = playerInventory.player;

        if(MysticalIndex.CONFIG.BookOfStorage.BlockBlacklist.contains(Registry.ITEM.getId(itemPickedUp.getItem())) || player.world.isClient()){
            return false;
        }

        //TODO once filters work
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

}
