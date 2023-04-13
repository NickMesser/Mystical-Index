package net.messer.mystical_index.events;

import net.messer.config.ModConfig;
import net.messer.mystical_index.block.entity.LibraryBlockEntity;
import net.messer.mystical_index.item.ModItems;
import net.messer.mystical_index.item.custom.BaseStorageBook;
import net.messer.mystical_index.item.inventory.SingleItemStackingInventory;
import net.minecraft.block.entity.Hopper;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

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

    public static Inventory getInputInventory(World world, Hopper hopper, Inventory inventory) {
//        var blockPos = new BlockPos(hopper.getHopperX(), hopper.getHopperY() + 1, hopper.getHopperZ());
//        var blockEntity = world.getBlockEntity(blockPos);
//        world.getPlayers().forEach(player -> player.sendMessage(Text.of("I made it here!"), false));
//        if(blockEntity instanceof LibraryBlockEntity entity)
//        {
//            var stack = entity.getItems().get(0);
//            var item = stack.getItem();
//            if(item instanceof BaseStorageBook book){
//                return book.getInventory(stack);
//            }
//
//            return entity;
//        }

        return inventory;
    }
}
