package net.messer.mystical_index.events;

import net.messer.config.ModConfig;
import net.messer.mystical_index.item.ModItems;
import net.messer.mystical_index.item.inventory.SingleItemStackingInventory;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

public class MixinHooks {
    public static boolean interceptPickup(PlayerInventory playerInventory, ItemStack itemPickedUp) {
        var player = playerInventory.player;

        if(ModConfig.StorageBookBlockBlacklist.contains(Registries.ITEM.getId(itemPickedUp.getItem())) || player.world.isClient()){
            return false;
        }

        if(itemPickedUp.isFood()){
            for (int i = 0; i < playerInventory.size(); i++) {
                var potentialBook = playerInventory.getStack(i);
                if (potentialBook.getItem() == ModItems.SATURATION_BOOK) {
                    SingleItemStackingInventory bookInventory = new SingleItemStackingInventory(potentialBook, ModConfig.SaturationBookMaxStacks);
                    return bookInventory.tryAddStack(itemPickedUp, Boolean.FALSE);
                }
            }
            return false;
        }

        if(itemPickedUp.getItem() instanceof BlockItem){
            for (int i = 0; i < playerInventory.size(); i++) {
                var potentialBook = playerInventory.getStack(i);
                if (potentialBook.getItem() == ModItems.STORAGE_BOOK) {
                    var bookInventory = new SingleItemStackingInventory(potentialBook, ModConfig.StorageBookMaxStacks);
                    return bookInventory.tryAddStack(itemPickedUp, Boolean.FALSE);
                }
            }
        }
        return false;
    }
}
