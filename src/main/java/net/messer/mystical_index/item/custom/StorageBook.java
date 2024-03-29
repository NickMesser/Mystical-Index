package net.messer.mystical_index.item.custom;

import net.messer.config.ModConfig;
import net.messer.mystical_index.item.custom.base_books.BaseStorageBook;
import net.messer.mystical_index.item.inventory.SingleItemStackingInventory;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.BundleTooltipData;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.item.TooltipData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;


public class StorageBook extends BaseStorageBook {

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

        SingleItemStackingInventory currentBookInventory = new SingleItemStackingInventory(player.getStackInHand(hand), ModConfig.StorageBookMaxStacks);
        if(player.isSneaking()){
            if(currentBookInventory.isEmpty())
            {
                Item item = context.getWorld().getBlockState(currentBlockPos).getBlock().asItem();

                if(ModConfig.StorageBookBlockBlacklist.contains(Registries.ITEM.getId(item).toString())){
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



        if(currentBookInventory.currentlyStoredItem instanceof BlockItem blockItem && currentBookInventory.tryRemoveOneItem()){
            var hitBlockPos = context.getBlockPos();
            var direction = context.getSide();
            var newBlockPos = hitBlockPos.offset(direction);
            var world = context.getWorld();
            if(world.canPlayerModifyAt(player, newBlockPos) && player.canPlaceOn(newBlockPos, direction, heldBookStack) && world.canSetBlock(newBlockPos)){
                var soundEvent = blockItem.getBlock().getSoundGroup(null).getPlaceSound();
                context.getWorld().playSound(null, newBlockPos,soundEvent, SoundCategory.BLOCKS, 1.0f,1.0f);
                context.getWorld().setBlockState(newBlockPos, blockItem.getBlock().getDefaultState());
            }
        }

        return super.useOnBlock(context);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        var storageInventory = new SingleItemStackingInventory(stack, ModConfig.StorageBookMaxStacks);
        return !storageInventory.isEmpty();
    }

    @Override
    public Optional<TooltipData> getTooltipData(ItemStack stack) {
        var storageInventory = new SingleItemStackingInventory(stack, ModConfig.StorageBookMaxStacks);
        if(storageInventory.isEmpty())
            return Optional.empty();


        return Optional.of(new BundleTooltipData(storageInventory.storedItems, ModConfig.StorageBookMaxStacks * 64));
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if(stack.hasGlint()){
            SingleItemStackingInventory inventory = new SingleItemStackingInventory(stack, ModConfig.StorageBookMaxStacks);
            int currentAmount = 0;
            for (ItemStack inventoryStack : inventory.storedItems) {
                if(inventoryStack.getItem() == Items.AIR)
                    continue;
                currentAmount += inventoryStack.getCount();
            }

            Item storedItem = inventory.currentlyStoredItem;
            tooltip.add(Text.literal("§a"+currentAmount + "x " + "§f" + storedItem.getName().getString()));
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

    @Override
    public SingleItemStackingInventory getInventory(ItemStack stack) {
        return new SingleItemStackingInventory(stack, ModConfig.StorageBookMaxStacks);
    }
}
