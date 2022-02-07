package net.messer.mystical_index.item.custom;

import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.item.inventory.SingleItemStackingInventory;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SaturationBook extends Item {
    public SaturationBook(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        if(world.isClient)
            return super.use(world, player, hand);

        var stack = player.getStackInHand(hand);
        if(player.isSneaking()){
            var hitResult = player.raycast(10, 0, false);
            if (hitResult.getType() == HitResult.Type.MISS)
                return TypedActionResult.pass(stack);

            var inventory = new SingleItemStackingInventory(stack, 1);
            if(inventory.isEmpty()){
                var box = Box.from(hitResult.getPos()).expand(.5);

                for(Entity entity : world.getNonSpectatingEntities(ItemEntity.class, box)){
                    ItemEntity item = (ItemEntity) entity;
                    var hitStack = item.getStack();
                    if(hitStack.isFood()){
                        inventory.setCurrentlyStoredItem(hitStack.getItem());
                        return TypedActionResult.pass(stack);
                    }
                }
            }
            else{
                player.sendMessage(new LiteralText("Unable to update stored item. Please empty all contents first"), true);
            }
        }

        return super.use(world, player, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if(entity instanceof PlayerEntity player && player.canConsume(false) &&
                !player.getItemCooldownManager().isCoolingDown(this) &&
                !player.isCreative() &&
                !world.isClient){

            var inventory = new SingleItemStackingInventory(stack,1);
            var foodStack = inventory.getStack(0);
            if(!foodStack.isEmpty() && foodStack.isFood()){
                player.eatFood(world, foodStack);
                inventory.markDirty();
                player.getItemCooldownManager().set(this, MysticalIndex.CONFIG.BookOfSaturation.TimeBetweenFeedings * 20);
            }
        }

        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        var storageInventory = new SingleItemStackingInventory(stack, 1);
        return !storageInventory.isEmpty();
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if(Screen.hasShiftDown()){
            tooltip.add(new TranslatableText("tooltip.mystical_index.saturation_book_shift0"));
            tooltip.add(new TranslatableText("tooltip.mystical_index.saturation_book_shift1"));
        } else {
            tooltip.add(new TranslatableText("tooltip.mystical_index.saturation_book"));
        }

        super.appendTooltip(stack, world, tooltip, context);
    }
}
