package net.messer.mystical_index.item.custom;

import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.item.inventory.SingleItemStackingInventory;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;


public class StorageBook extends Item {

    public StorageBook(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if(context.getWorld().isClient)
            return super.useOnBlock(context);

        PlayerEntity player = context.getPlayer();
        Hand hand = context.getHand();
        BlockPos currentBlockPos = context.getBlockPos();
        ItemStack heldBookStack = player.getStackInHand(hand);

        SingleItemStackingInventory currentBookInventory = new SingleItemStackingInventory(player.getStackInHand(hand), 1);
        if(player.isSneaking()){
            if(currentBookInventory.isEmpty())
            {
                Item item = context.getWorld().getBlockState(currentBlockPos).getBlock().asItem();

                if(MysticalIndex.CONFIG.BookOfStorage.BlockBlacklist.contains(Registry.ITEM.getId(item).toString())){
                    player.sendMessage(Text.literal("This block is blacklisted. Sorry :("), true);
                    return super.useOnBlock(context);
                }

                currentBookInventory.setCurrentlyStoredItem(item);
                heldBookStack.setCustomName(Text.literal("Book of " + item.getName().getString()));
            }
            else{
                player.sendMessage(Text.literal("Unable to update stored item. Please empty all contents first"), true);
            }
            return super.useOnBlock(context);
        }

        if(currentBookInventory.isEmpty()){ return super.useOnBlock(context); }

        var stackInBook = currentBookInventory.getStack(0);

        if(stackInBook.getItem() instanceof BlockItem blockItem){
            var hitBlockPos = context.getBlockPos();
            var direction = context.getSide();
            var newBlockPos = hitBlockPos.offset(direction);
            var world = context.getWorld();
            if(world.canPlayerModifyAt(player, newBlockPos) && player.canPlaceOn(newBlockPos, direction, stackInBook) && world.canSetBlock(newBlockPos)){
                currentBookInventory.removeStack(0, 1);
                var soundEvent = blockItem.getBlock().getSoundGroup(null).getPlaceSound();
                context.getWorld().playSound(null, newBlockPos,soundEvent, SoundCategory.BLOCKS, 1.0f,1.0f);
                context.getWorld().setBlockState(newBlockPos, blockItem.getBlock().getDefaultState());
            }
        }

        return super.useOnBlock(context);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        var storageInventory = new SingleItemStackingInventory(stack, 1);
        return !storageInventory.isEmpty();
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if(stack.hasGlint()){
            SingleItemStackingInventory inventory = new SingleItemStackingInventory(stack, 1);
            ItemStack storedStack = inventory.getStack(0);
            tooltip.add(Text.literal("§a"+storedStack.getCount() + "x " + "§f" + storedStack.getItem().getName().getString()));
            tooltip.add(Text.literal(""));
        }

        if(Screen.hasShiftDown()){
            tooltip.add(Text.translatable("tooltip.mystical_index.storage_book_shift0"));
            tooltip.add(Text.translatable("tooltip.mystical_index.storage_book_shift1"));
        } else {
            tooltip.add(Text.translatable("tooltip.mystical_index.storage_book"));
        }
        super.appendTooltip(stack, world, tooltip, context);
    }
}
