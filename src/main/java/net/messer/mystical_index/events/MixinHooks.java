package net.messer.mystical_index.events;

import net.messer.config.ModConfig;
import net.messer.mystical_index.item.ModItems;
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

        var compound = stack.getNbt();
        compound.remove("Entity");

        NbtCompound entityNbt = new NbtCompound();
        entity.saveSelfNbt(entityNbt);
        compound.remove("Entity");
        compound.put("Entity", entityNbt);
    }
}
