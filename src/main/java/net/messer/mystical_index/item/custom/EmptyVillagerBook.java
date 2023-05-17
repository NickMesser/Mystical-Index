package net.messer.mystical_index.item.custom;

import net.messer.mystical_index.item.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

public class EmptyVillagerBook extends Item {
    public EmptyVillagerBook(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if(entity instanceof VillagerEntity villagerEntity && !stack.hasNbt()){
            if(villagerEntity.isBaby()){
                ItemStack newStack = new ItemStack(ModItems.BABY_VILLAGER_BOOK);
                var bookItem = (BabyVillagerBook) newStack.getItem();
                bookItem.addBabyVillagerToBook(newStack, villagerEntity);
                entity.remove(Entity.RemovalReason.DISCARDED);
                stack.decrement(1);
                user.setStackInHand(hand, newStack);
            } else{
                ItemStack newStack = new ItemStack(ModItems.VILLAGER_BOOK);
                var bookItem = (VillagerBook) newStack.getItem();
                bookItem.addVillagerToBook(newStack, (VillagerEntity) entity);
                entity.remove(Entity.RemovalReason.DISCARDED);
                stack.decrement(1);
                user.setStackInHand(hand, newStack);
            }

            super.useOnEntity(stack, user, entity, hand);
        }
        return super.useOnEntity(stack, user, entity, hand);
    }
}
