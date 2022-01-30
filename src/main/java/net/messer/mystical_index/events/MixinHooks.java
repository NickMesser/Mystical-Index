package net.messer.mystical_index.events;

import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.item.ModItems;
import net.messer.mystical_index.item.inventory.SingleItemStackingInventory;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.registry.Registry;

public class MixinHooks {
    public static boolean interceptPickup(PlayerInventory playerInventory, ItemStack itemPickedUp) {
        var player = playerInventory.player;

        if(MysticalIndex.CONFIG.BookOfStorage.BlockBlacklist.contains(Registry.ITEM.getId(itemPickedUp.getItem())) || player.world.isClient()){
            return false;
        }

        if(itemPickedUp.isFood()){
            for (int i = 0; i < playerInventory.size(); i++) {
                var potentialBook = playerInventory.getStack(i);
                if (potentialBook.getItem() == ModItems.SATURATION_BOOK) {
                    var bookInventory = new SingleItemStackingInventory(potentialBook, 1);
                    if (bookInventory.currentlyStoredItem == itemPickedUp.getItem()) {
                        var itemToAdd = bookInventory.addStack(itemPickedUp);
                        if (itemToAdd.isEmpty()) {
                            return true;
                        } else {
                            itemPickedUp.setCount(itemToAdd.getCount());
                        }
                    }
                }
            }
            return false;
        }

        if(itemPickedUp.getItem() instanceof BlockItem){
            for (int i = 0; i < playerInventory.size(); i++) {
                var potentialBook = playerInventory.getStack(i);
                if (potentialBook.getItem() == ModItems.STORAGE_BOOK) {
                    var bookInventory = new SingleItemStackingInventory(potentialBook, 1);
                    if (bookInventory.currentlyStoredItem == itemPickedUp.getItem()) {
                        var itemToAdd = bookInventory.addStack(itemPickedUp);
                        if (itemToAdd.isEmpty()) {
                            return true;
                        } else {
                            itemPickedUp.setCount(itemToAdd.getCount());
                        }
                    }
                }
            }
        }
        return false;
    }
}
