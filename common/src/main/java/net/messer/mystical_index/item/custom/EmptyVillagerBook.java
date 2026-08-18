package net.messer.mystical_index.item.custom;

import net.messer.mystical_index.item.ModItems;
import net.messer.util.MysticalUtil;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class EmptyVillagerBook extends Item {
    public EmptyVillagerBook(Item.Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity entity, InteractionHand hand) {
// Run server-side only: doing the capture and swap on the client left a ghost villager
// and a desynced book until the next update.
        if(user.level().isClientSide())
            return super.interactLivingEntity(stack, user, entity, hand);

        if(entity instanceof Villager villagerEntity && !MysticalUtil.hasCustomData(stack)){
            if(villagerEntity.isBaby()){
                ItemStack newStack = new ItemStack(ModItems.BABY_VILLAGER_BOOK.get());
                var bookItem = (BabyVillagerBook) newStack.getItem();
                bookItem.addBabyVillagerToBook(newStack, villagerEntity);
                entity.remove(Entity.RemovalReason.DISCARDED);
// exchangeStack consumes one book and routes the filled one to the inventory when
// the player is holding more than one; setStackInHand alone destroyed the rest.
// creativeOverride must be false, otherwise a creative player gets the input book
// back unchanged and the filled book is lost.
                user.setItemInHand(hand, ItemUtils.createFilledResult(stack, user, newStack, false));
            } else{
                ItemStack newStack = new ItemStack(ModItems.VILLAGER_BOOK.get());
                var bookItem = (VillagerBook) newStack.getItem();
                bookItem.addVillagerToBook(newStack, (Villager) entity);
                entity.remove(Entity.RemovalReason.DISCARDED);
                user.setItemInHand(hand, ItemUtils.createFilledResult(stack, user, newStack, false));
            }
        }
        return super.interactLivingEntity(stack, user, entity, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag type) {
        tooltip.accept(Component.translatable("tooltip.mystical_index.empty_villager_book0"));
        tooltip.accept(Component.translatable("tooltip.mystical_index.empty_villager_book1"));
        super.appendHoverText(stack, context, display, tooltip, type);
    }
}
