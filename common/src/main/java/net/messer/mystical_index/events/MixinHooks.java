package net.messer.mystical_index.events;

import net.minecraft.core.registries.Registries;
import net.messer.config.ModConfig;
import net.messer.mystical_index.item.ModItems;
import net.messer.mystical_index.item.custom.TieredStorageBook;
import net.messer.mystical_index.item.custom.VillagerBook;
import net.messer.mystical_index.item.inventory.SingleItemStackingInventory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.nbt.CompoundTag;
import net.messer.util.MysticalUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;



public class MixinHooks {
    public static boolean interceptPickup(Inventory playerInventory, ItemStack itemPickedUp) {
        var player = playerInventory.player;

        // The blacklist holds strings; comparing it against an Identifier never matched.
        if(ModConfig.StorageBookBlockBlacklist.contains(BuiltInRegistries.ITEM.getKey(itemPickedUp.getItem()).toString()) || player.level().isClientSide()){
            return false;
        }

        // The raw inventory scans these used to run are REPLACED by the visitor, not supplemented:
        // it yields each loose book once and each slung book once, so nothing can absorb twice.
        var absorbed = new boolean[]{false};

        if(itemPickedUp.has(DataComponents.FOOD)){
            MysticalUtil.forEachEffectiveBook(player, potentialBook -> {
                if (absorbed[0] || potentialBook.getItem() != ModItems.SATURATION_BOOK.get())
                    return;

                var bookInventory = new SingleItemStackingInventory(potentialBook, ModConfig.SaturationBookMaxStacks);
                if(bookInventory.tryAddStack(itemPickedUp, Boolean.FALSE))
                    absorbed[0] = true;
            });
            if (absorbed[0])
                return true;
        }

        if(itemPickedUp.getItem() instanceof BlockItem){
            MysticalUtil.forEachEffectiveBook(player, potentialBook -> {
                if (absorbed[0] || potentialBook.getItem() != ModItems.STORAGE_BOOK.get())
                    return;

                var bookInventory = new SingleItemStackingInventory(potentialBook, ModConfig.StorageBookMaxStacks);
                if(bookInventory.tryAddStack(itemPickedUp, Boolean.FALSE))
                    absorbed[0] = true;
            });
            if (absorbed[0])
                return true;
        }

        // Books of Holding get a pass for every pickup the books above did not consume, but only
        // for types they already hold: a pocket book must not claim a type slot on its own. A
        // partial absorb leaves a remainder for the next book, or for the player's own inventory.
        MysticalUtil.forEachEffectiveBook(player, potentialBook -> {
            if (absorbed[0] || !(potentialBook.getItem() instanceof TieredStorageBook tiered))
                return;

            if(tiered.getInventory(potentialBook).tryAddStack(itemPickedUp, false) || itemPickedUp.isEmpty())
                absorbed[0] = true;
        });

        return absorbed[0];
    }

    public static void interactWithItem(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir, ItemStack itemStack, LivingEntity entity) {
        if(itemStack.is(ModItems.EMPTY_VILLAGER_BOOK.get())) {
            InteractionResult actionResult = itemStack.interactLivingEntity(player, entity, hand);
            if (actionResult.consumesAction()) {
                cir.setReturnValue(actionResult);
            }
        }
    }
    public static void afterUsing(MerchantOffer offer, CallbackInfo ci, Player player, Villager entity) {
        if(player == null)
            return;

        var stack = player.getMainHandItem();
        if(!MysticalUtil.hasCustomData(stack))
            return;

        if(!(stack.getItem() instanceof VillagerBook))
            return;

        // Only persist trades actually driven by the book. VillagerBook.use() spins up a throwaway
        // villager that is never registered in the world, so getEntityById can never resolve back
        // to it. A real world villager the player traded with while merely holding a book IS
        // registered; writing it back would overwrite the book's stored villager with a copy of
        // the world one.
        if(entity.level().getEntity(entity.getId()) == entity && !entity.isRemoved())
            return;

        final CompoundTag entityNbt = MysticalUtil.saveEntityWithId(entity);
        if (entityNbt == null)
            return;

        MysticalUtil.editCustomData(stack, compound -> {
            compound.remove("Entity");
            compound.put("Entity", entityNbt);
        });
    }
}
