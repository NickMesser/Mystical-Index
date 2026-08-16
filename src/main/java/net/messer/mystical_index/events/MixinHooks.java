package net.messer.mystical_index.events;

import net.messer.config.ModConfig;
import net.messer.mystical_index.item.ModItems;
import net.messer.mystical_index.item.custom.TieredStorageBook;
import net.messer.mystical_index.item.custom.VillagerBook;
import net.messer.mystical_index.item.inventory.SingleItemStackingInventory;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.village.TradeOffer;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class MixinHooks {
    public static boolean interceptPickup(PlayerInventory playerInventory, ItemStack itemPickedUp) {
        var player = playerInventory.player;

        // The blacklist holds strings; comparing it against an Identifier never matched.
        if(ModConfig.StorageBookBlockBlacklist.contains(Registries.ITEM.getId(itemPickedUp.getItem()).toString()) || player.getWorld().isClient()){
            return false;
        }

        if(itemPickedUp.isFood()){
            for (int i = 0; i < playerInventory.size(); i++) {
                var potentialBook = playerInventory.getStack(i);
                if (potentialBook.getItem() == ModItems.SATURATION_BOOK) {
                    SingleItemStackingInventory bookInventory = new SingleItemStackingInventory(potentialBook, ModConfig.SaturationBookMaxStacks);
                    if(bookInventory.tryAddStack(itemPickedUp, Boolean.FALSE))
                        return true;
                }
            }
        }

        if(itemPickedUp.getItem() instanceof BlockItem){
            for (int i = 0; i < playerInventory.size(); i++) {
                var potentialBook = playerInventory.getStack(i);
                if (potentialBook.getItem() == ModItems.STORAGE_BOOK) {
                    var bookInventory = new SingleItemStackingInventory(potentialBook, ModConfig.StorageBookMaxStacks);
                    if(bookInventory.tryAddStack(itemPickedUp, Boolean.FALSE))
                        return true;
                }
            }
        }

        // Books of Holding get a pass for every pickup the books above did not consume, but only
        // for types they already hold: a pocket book must not claim a type slot on its own. A
        // partial absorb leaves a remainder for the next book, or for the player's own inventory.
        for (int i = 0; i < playerInventory.size(); i++) {
            var potentialBook = playerInventory.getStack(i);
            if (potentialBook.getItem() instanceof TieredStorageBook tiered) {
                if(tiered.getInventory(potentialBook).tryAddStack(itemPickedUp, false))
                    return true;

                if(itemPickedUp.isEmpty())
                    return true;
            }
        }

        return false;
    }

    public static void interactWithItem(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir, ItemStack itemStack, LivingEntity entity) {
        if(itemStack.isOf(ModItems.EMPTY_VILLAGER_BOOK)) {
            ActionResult actionResult = itemStack.useOnEntity(player, entity, hand);
            if (actionResult.isAccepted()) {
                cir.setReturnValue(actionResult);
            }
        }
    }
    public static void afterUsing(TradeOffer offer, CallbackInfo ci, PlayerEntity player, VillagerEntity entity) {
        if(player == null)
            return;

        var stack = player.getMainHandStack();
        if(!stack.hasNbt())
            return;

        if(!(stack.getItem() instanceof VillagerBook))
            return;

        // Only persist trades actually driven by the book. VillagerBook.use() spins up a throwaway
        // villager that is never registered in the world, so getEntityById can never resolve back
        // to it. A real world villager the player traded with while merely holding a book IS
        // registered; writing it back would overwrite the book's stored villager with a copy of
        // the world one.
        if(entity.getWorld().getEntityById(entity.getId()) == entity && !entity.isRemoved())
            return;

        var compound = stack.getNbt();
        NbtCompound entityNbt = new NbtCompound();
        entity.saveSelfNbt(entityNbt);
        compound.remove("Entity");
        compound.put("Entity", entityNbt);
    }
}
