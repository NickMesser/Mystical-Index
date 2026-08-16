package net.messer.mystical_index.item.custom;

import net.messer.config.ModConfig;
import net.messer.mystical_index.item.custom.base_books.BaseStorageBook;
import net.messer.mystical_index.item.inventory.SingleItemStackingInventory;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.client.gui.screen.Screen;
import net.messer.mystical_index.item.inventory.BookContentsTooltipData;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
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
        if(player == null)
            return super.useOnBlock(context);

        Hand hand = context.getHand();
        BlockPos currentBlockPos = context.getBlockPos();
        ItemStack heldBookStack = context.getStack();

        SingleItemStackingInventory currentBookInventory = new SingleItemStackingInventory(context.getStack(), ModConfig.StorageBookMaxStacks);
        if(player.isSneaking()){
            if(currentBookInventory.isEmpty())
            {
                Item item = context.getWorld().getBlockState(currentBlockPos).getBlock().asItem();

                if(ModConfig.StorageBookBlockBlacklist.contains(Registries.ITEM.getId(item).toString())){
                    player.sendMessage(Text.translatable("message.mystical_index.block_blacklisted"), true);
                    return super.useOnBlock(context);
                }

                currentBookInventory.setCurrentlyStoredItem(item);
                heldBookStack.set(DataComponentTypes.CUSTOM_NAME, Text.translatable("item.mystical_index.storage_book.named", item.getName()));
            }
            else{
                player.sendMessage(Text.translatable("message.mystical_index.empty_first"), true);
            }
            return super.useOnBlock(context);
        }

        if(currentBookInventory.isEmpty()){ return super.useOnBlock(context); }



        if(currentBookInventory.currentlyStoredItem instanceof BlockItem blockItem){
            // Place through the item pipeline with a throwaway block stack so replaceability,
            // canPlaceAt, the proper placement state and the place sound are all handled by
            // vanilla instead of overwriting the target block with a default state. Only draw
            // one item from the book once a block was actually placed.
            BlockHitResult hitResult = new BlockHitResult(context.getHitPos(), context.getSide(), context.getBlockPos(), context.hitsInsideBlock());
            ItemPlacementContext placementContext = new ItemPlacementContext(player, hand, new ItemStack(blockItem), hitResult);
            if(blockItem.place(placementContext).isAccepted())
                currentBookInventory.tryRemoveOneItem();
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


        return Optional.of(BookContentsTooltipData.fromInventory(storageInventory));
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        if(Screen.hasShiftDown()){
            tooltip.add(Text.translatable("tooltip.mystical_index.storage_book_shift0"));
            tooltip.add(Text.translatable("tooltip.mystical_index.storage_book_shift1"));
        } else {
            tooltip.add(Text.translatable("tooltip.mystical_index.storage_book"));
        }
        super.appendTooltip(stack, context, tooltip, type);
    }

    @Override
    public SingleItemStackingInventory getInventory(ItemStack stack) {
        return new SingleItemStackingInventory(stack, ModConfig.StorageBookMaxStacks);
    }
}
