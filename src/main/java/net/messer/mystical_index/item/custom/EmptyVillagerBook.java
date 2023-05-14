package net.messer.mystical_index.item.custom;

import net.messer.mystical_index.item.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

public class EmptyVillagerBook extends Item {
    public EmptyVillagerBook(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if(entity instanceof VillagerEntity && !stack.hasNbt()){
            ItemStack newStack = new ItemStack(ModItems.VILLAGER_BOOK);
            NbtCompound newStackNbt = newStack.getOrCreateNbt();

            NbtCompound entityNbt = new NbtCompound();
            entity.saveSelfNbt(entityNbt);
            newStackNbt.put("Entity", entityNbt);
            entity.remove(Entity.RemovalReason.DISCARDED);

            stack.decrement(1);
            user.setStackInHand(hand, newStack);
            super.useOnEntity(stack, user, entity, hand);
        }
        return super.useOnEntity(stack, user, entity, hand);
    }
}
