package net.messer.mystical_index.item.custom;

import net.messer.mystical_index.item.ModItems;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.of("§eRIGHT CLICK§r a villager"));
        tooltip.add(Text.of("to store them in this book"));
        super.appendTooltip(stack, world, tooltip, context);
    }
}
