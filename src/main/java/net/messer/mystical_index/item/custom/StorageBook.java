package net.messer.mystical_index.item.custom;

import net.messer.mystical_index.item.inventory.SingleItemStackingInventory;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
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

        var player = context.getPlayer();
        var hand = context.getHand();
        var currentBlockPos = context.getBlockPos();
        var heldBookStack = player.getStackInHand(hand);

        var currentBookInventory = new SingleItemStackingInventory(player.getStackInHand(hand), 1);
        if(Screen.hasShiftDown()){
            if(currentBookInventory.isEmpty())
            {
                var item = context.getWorld().getBlockState(currentBlockPos).getBlock().asItem();
                currentBookInventory.setCurrentlyStoredItem(item);
                heldBookStack.setCustomName(new LiteralText("Book of " + item.getName().getString()));
                return super.useOnBlock(context);
            }
            else{
                player.sendMessage(new LiteralText("Unable to update stored item. Please empty all contents first"), true);
                return super.useOnBlock(context);
            }
        }

        if(currentBookInventory.isEmpty()){ return super.useOnBlock(context); }

        var sideOfBlockClicked = context.getSide();
        var newBlockPos = new BlockPos.Mutable(0,0,0);

        switch (sideOfBlockClicked.name()){
            case "UP":
                newBlockPos.set(currentBlockPos.getX(), currentBlockPos.getY() + 1, currentBlockPos.getZ());
                break;
            case "DOWN":
                newBlockPos.set(currentBlockPos.getX(), currentBlockPos.getY() - 1, currentBlockPos.getZ());
                break;
            case "NORTH":
                newBlockPos.set(currentBlockPos.getX(), currentBlockPos.getY(), currentBlockPos.getZ() - 1);
                break;
            case "SOUTH":
                newBlockPos.set(currentBlockPos.getX(), currentBlockPos.getY(), currentBlockPos.getZ() + 1);
                break;
            case "EAST":
                newBlockPos.set(currentBlockPos.getX() + 1, currentBlockPos.getY(), currentBlockPos.getZ());
                break;
            case "WEST":
                newBlockPos.set(currentBlockPos.getX() - 1, currentBlockPos.getY(), currentBlockPos.getZ());
                break;
        }


        if(currentBookInventory.getStack(0).getItem() instanceof BlockItem blockItem){
            currentBookInventory.removeStack(0, 1);
            var soundEvent = blockItem.getBlock().getSoundGroup(null).getPlaceSound();
            context.getWorld().playSound(null, newBlockPos,soundEvent, SoundCategory.BLOCKS, 1.0f,1.0f);
            context.getWorld().setBlockState(newBlockPos, blockItem.getBlock().getDefaultState());
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
        if(Screen.hasShiftDown()){
            tooltip.add(new TranslatableText("tooltip.mystical_index.storage_book_shift0"));
            tooltip.add(new TranslatableText("tooltip.mystical_index.storage_book_shift1"));
        } else {
            tooltip.add(new TranslatableText("tooltip.mystical_index.storage_book"));
        }

        super.appendTooltip(stack, world, tooltip, context);
    }
}
