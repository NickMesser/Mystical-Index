package net.messer.mystical_index.events;

import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.item.ModItems;
import net.messer.mystical_index.item.inventory.SingleItemStackingInventory;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

public class MixinHooks {
    public static boolean interceptPickup(PlayerInventory playerInventory, ItemStack itemPickedUp){
        var player = playerInventory.player;

        if(player.world.isClient())
            return false;

        for(int i = 0; i < playerInventory.size(); i++){
            var potentialBook = playerInventory.getStack(i);
            if(potentialBook.getItem() == ModItems.STORAGE_BOOK){
                var bookInventory = new SingleItemStackingInventory(potentialBook, 1);
                if(bookInventory.currentlyStoredItem == itemPickedUp.getItem()){
                    bookInventory.addStack(itemPickedUp);
                    itemPickedUp.setCount(0);
                    MysticalIndex.LOGGER.info(bookInventory.getStack(0).getCount());
                    return true;
                }
            }
        }

        return false;
    }
}
