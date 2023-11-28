package net.messer.mystical_index.item.custom;

import net.messer.config.ModConfig;
import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.item.ModItems;
import net.messer.mystical_index.item.inventory.SingleItemStackingInventory;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.BundleTooltipData;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.item.TooltipData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class SaturationBook extends Item {
    public SaturationBook(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        if(world.isClient)
            return super.use(world, player, hand);

        ItemStack stack = player.getStackInHand(hand);
        if(player.isSneaking()){
            var hitResult = player.raycast(10, 0, false);
            if (hitResult.getType() == HitResult.Type.MISS)
                return TypedActionResult.pass(stack);

            var inventory = new SingleItemStackingInventory(stack, ModConfig.SaturationBookMaxStacks);
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
                player.sendMessage(Text.literal("Unable to update stored item. Please empty all contents first"), true);
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

            var inventory = new SingleItemStackingInventory(stack,ModConfig.SaturationBookMaxStacks);
            var foodStack = inventory.getFirstItemStack();
            if(!foodStack.isEmpty() && foodStack.isFood()){
                player.eatFood(world, foodStack);
                inventory.markDirty();
                player.getItemCooldownManager().set(this, ModConfig.SaturationBookTimeBetweenFeedings * 20);
            }
        }

        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        var storageInventory = new SingleItemStackingInventory(stack, ModConfig.SaturationBookMaxStacks);
        return !storageInventory.isEmpty();
    }

    @Override
    public Optional<TooltipData> getTooltipData(ItemStack stack) {
        var storageInventory = new SingleItemStackingInventory(stack, ModConfig.SaturationBookMaxStacks);
        if(storageInventory.isEmpty())
            return Optional.empty();


        return Optional.of(new BundleTooltipData(storageInventory.storedItems, ModConfig.SaturationBookMaxStacks * 64));
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if(stack.hasGlint()){
            SingleItemStackingInventory inventory = new SingleItemStackingInventory(stack, ModConfig.SaturationBookMaxStacks);
            tooltip.add(Text.literal("§a"+ inventory.getCountOfStoredItem() + "x " + "§f" + inventory.currentlyStoredItem.getName().getString()));
            tooltip.add(Text.literal(""));
        }

        if(Screen.hasShiftDown()){
            tooltip.add(Text.translatable("tooltip.mystical_index.saturation_book_shift0"));
            tooltip.add(Text.translatable("tooltip.mystical_index.saturation_book_shift1"));
        } else {
            tooltip.add(Text.translatable("tooltip.mystical_index.saturation_book"));
        }

        super.appendTooltip(stack, world, tooltip, context);
    }
}
