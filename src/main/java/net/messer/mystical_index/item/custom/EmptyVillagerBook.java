package net.messer.mystical_index.item.custom;

import net.messer.mystical_index.item.ModItems;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
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
        // Run server-side only: doing the capture and swap on the client left a ghost villager
        // and a desynced book until the next update.
        if(user.getWorld().isClient)
            return super.useOnEntity(stack, user, entity, hand);

        if(entity instanceof VillagerEntity villagerEntity && !stack.hasNbt()){
            if(villagerEntity.isBaby()){
                ItemStack newStack = new ItemStack(ModItems.BABY_VILLAGER_BOOK);
                var bookItem = (BabyVillagerBook) newStack.getItem();
                bookItem.addBabyVillagerToBook(newStack, villagerEntity);
                entity.remove(Entity.RemovalReason.DISCARDED);
                // exchangeStack consumes one book and routes the filled one to the inventory when
                // the player is holding more than one; setStackInHand alone destroyed the rest.
                // creativeOverride must be false, otherwise a creative player gets the input book
                // back unchanged and the filled book is lost.
                user.setStackInHand(hand, ItemUsage.exchangeStack(stack, user, newStack, false));
            } else{
                ItemStack newStack = new ItemStack(ModItems.VILLAGER_BOOK);
                var bookItem = (VillagerBook) newStack.getItem();
                bookItem.addVillagerToBook(newStack, (VillagerEntity) entity);
                entity.remove(Entity.RemovalReason.DISCARDED);
                user.setStackInHand(hand, ItemUsage.exchangeStack(stack, user, newStack, false));
            }
        }
        return super.useOnEntity(stack, user, entity, hand);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.mystical_index.empty_villager_book0"));
        tooltip.add(Text.translatable("tooltip.mystical_index.empty_villager_book1"));
        super.appendTooltip(stack, world, tooltip, context);
    }
}
