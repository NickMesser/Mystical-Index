package net.messer.mystical_index.item.custom;

import net.messer.util.SelfUpdatingBook;
import net.minecraft.core.registries.Registries;
import net.messer.config.ModConfig;
import net.messer.mystical_index.item.custom.base_books.BaseStorageBook;
import net.messer.mystical_index.item.inventory.SingleItemStackingInventory;
import net.minecraft.core.component.DataComponents;
import net.minecraft.client.gui.screens.Screen;
import net.messer.mystical_index.item.inventory.BookContentsTooltipData;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;


import net.minecraft.client.Minecraft;

import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

import net.messer.util.GlintingBook;

public class StorageBook extends BaseStorageBook implements GlintingBook, SelfUpdatingBook {

    public StorageBook(Item.Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if(context.getLevel().isClientSide())
            return super.useOn(context);

        Player player = context.getPlayer();
        if(player == null)
            return super.useOn(context);

        InteractionHand hand = context.getHand();
        BlockPos currentBlockPos = context.getClickedPos();
        ItemStack heldBookStack = context.getItemInHand();

        SingleItemStackingInventory currentBookInventory = new SingleItemStackingInventory(context.getItemInHand(), ModConfig.StorageBookMaxStacks);
        if(player.isShiftKeyDown()){
            if(currentBookInventory.isEmpty())
            {
                Item item = context.getLevel().getBlockState(currentBlockPos).getBlock().asItem();

                if(ModConfig.StorageBookBlockBlacklist.contains(BuiltInRegistries.ITEM.getKey(item).toString())){
                    player.sendOverlayMessage(Component.translatable("message.mystical_index.block_blacklisted"));
                    return super.useOn(context);
                }

                currentBookInventory.setCurrentlyStoredItem(item);
                heldBookStack.set(DataComponents.CUSTOM_NAME, Component.translatable("item.mystical_index.storage_book.named", item.getName(item.getDefaultInstance())));
            }
            else{
                player.sendOverlayMessage(Component.translatable("message.mystical_index.empty_first"));
            }
            return super.useOn(context);
        }

        if(currentBookInventory.isEmpty()){ return super.useOn(context); }



        if(currentBookInventory.currentlyStoredItem instanceof BlockItem blockItem){
            // Place through the item pipeline with a throwaway block stack so replaceability,
            // canPlaceAt, the proper placement state and the place sound are all handled by
            // vanilla instead of overwriting the target block with a default state. Only draw
            // one item from the book once a block was actually placed.
            BlockHitResult hitResult = new BlockHitResult(context.getClickLocation(), context.getClickedFace(), context.getClickedPos(), context.isInside());
            BlockPlaceContext placementContext = new BlockPlaceContext(player, hand, new ItemStack(blockItem), hitResult);
            if(blockItem.place(placementContext).consumesAction())
                currentBookInventory.tryRemoveOneItem();
        }

        return super.useOn(context);
    }
    @Override
    public boolean shouldGlint(ItemStack stack) {
        var storageInventory = new SingleItemStackingInventory(stack, ModConfig.StorageBookMaxStacks);
        return !storageInventory.isEmpty();
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        var storageInventory = new SingleItemStackingInventory(stack, ModConfig.StorageBookMaxStacks);
        if(storageInventory.isEmpty())
            return Optional.empty();


        return Optional.of(BookContentsTooltipData.fromInventory(storageInventory));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag type) {
        if(Minecraft.getInstance().hasShiftDown()){
            tooltip.accept(Component.translatable("tooltip.mystical_index.storage_book_shift0"));
            tooltip.accept(Component.translatable("tooltip.mystical_index.storage_book_shift1"));
        } else {
            tooltip.accept(Component.translatable("tooltip.mystical_index.storage_book"));
        }
        super.appendHoverText(stack, context, display, tooltip, type);
    }

    @Override
    public SingleItemStackingInventory getInventory(ItemStack stack) {
        return new SingleItemStackingInventory(stack, ModConfig.StorageBookMaxStacks);
    }
}
