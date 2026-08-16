package net.messer.mystical_index.item.custom;

import net.fabricmc.loader.impl.util.StringUtil;
import net.messer.config.ModConfig;
import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.block.entity.LibraryBlockEntity;
import net.messer.mystical_index.item.custom.base_books.BaseGeneratingBook;
import net.messer.mystical_index.item.inventory.SingleItemStackingInventory;
import net.messer.util.MysticalUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.gui.screen.Screen;
import net.messer.mystical_index.item.inventory.BookContentsTooltipData;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;

public class FarmingBook extends BaseGeneratingBook {
    public FarmingBook(Settings settings) {
        super(settings);
    }

    public static Map<String, Integer> BookOfFarmingCooldowns = ModConfig.BookOfFarmingCooldowns;
    public static int defaultCooldown = ModConfig.BookOfFarmingDefaultCooldown;

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if(context.getWorld().isClient)
            return super.useOnBlock(context);

        var itemStack = context.getStack();
        var world = context.getWorld();
        // Attaches the compound even when empty, same as the old getOrCreateNbt(): an untouched
        // book picking one up here is what hasGlint() and the tick guard both read.
        var compound = MysticalUtil.getOrCreateCustomData(itemStack);

        if(compound.contains("cropBlock")) // check if book has a cropBlock assigned to it
            return super.useOnBlock(context);

        Block block = context.getWorld().getBlockState(context.getBlockPos()).getBlock();
        if(block instanceof CropBlock cropBlock){
            addCrop(itemStack, cropBlock);
            var currentTime = context.getWorld().getTime() % 24000;
            updateUseTime(context.getStack(), currentTime);
            world.removeBlock(context.getBlockPos(), false);
        }

        return super.useOnBlock(context);
    }

    public void addCrop(ItemStack stack, CropBlock cropBlock){
        var cropBlockId = Registries.BLOCK.getId(cropBlock).toString();
        MysticalUtil.editCustomData(stack, compound -> compound.putString("cropBlock", cropBlockId));
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.of("Book Of Farming: " + StringUtil.capitalize(cropBlock.asItem().toString())));
    }

    public CropBlock getCrop(ItemStack stack){
        if(!MysticalUtil.hasCustomData(stack))
            return null;

        NbtCompound compound = MysticalUtil.getOrCreateCustomData(stack);
        var cropBlockId = compound.getString("cropBlock");
        var block = Registries.BLOCK.get(Identifier.of(cropBlockId));
        if (!(block instanceof CropBlock))
        {
            compound.remove("cropBlock");
            MysticalUtil.setCustomData(stack, compound);
            return null;
        }
        return (CropBlock) Registries.BLOCK.get(Identifier.of(cropBlockId));
    }

    @Override
    public SingleItemStackingInventory getInventory(ItemStack stack) {
        return new SingleItemStackingInventory(stack, ModConfig.BookOfFarmingMaxStacks);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        customBookTick(stack, world, entity);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    public void tryGenerateResources(ItemStack stack, World world){
        var compound = MysticalUtil.getCustomData(stack);
        if(compound == null)
            return;

        var currentTime = world.getTime() % 24000;
        var lastUsedTime = compound.getLong("lastUsedTime");
        var difference = currentTime - lastUsedTime;
        if(difference < 0)
            updateUseTime(stack, currentTime);

        var crop = getCrop(stack);
        if(crop == null)
            return;

        var cropId = Registries.BLOCK.getId(crop).toString();
        var cooldown = ModConfig.BookOfFarmingDefaultCooldown;
        if(BookOfFarmingCooldowns.get(cropId) != null)
            cooldown = BookOfFarmingCooldowns.get(cropId);


        if((difference) < (cooldown * 20))
            return;

        updateUseTime(stack, currentTime);

        BlockState cropGrownState = crop.getDefaultState().with(CropBlock.AGE, crop.getMaxAge());

        var bookInventory = getInventory(stack);
        var cropLoot = Block.getDroppedStacks(cropGrownState, (ServerWorld) world, new BlockPos(0,0,0), null);
        for (ItemStack itemStack : cropLoot) {
            // Non-seed drops (the actual produce) get a small random bonus.
            if(itemStack.getItem() != crop.asItem())
                itemStack.setCount(itemStack.getCount() + world.random.nextInt(4));

            if(!bookInventory.tryAddStack(itemStack, Boolean.TRUE))
                itemStack.setCount(0);
        }
    }

    @Override
    public void customBookTick(ItemStack stack, World world, BlockEntity be) {
        if (world.isClient)
            return;

        if(!MysticalUtil.hasCustomData(stack))
            return;

        if(!(be instanceof LibraryBlockEntity))
            return;

        tryGenerateResources(stack, world);
    }

    @Override
    public void customBookTick(ItemStack stack, World world, Entity entity) {
        if(world.isClient)
            return;

        if(!(entity instanceof PlayerEntity))
            return;

        if (!MysticalUtil.hasCustomData(stack))
            return;

        tryGenerateResources(stack, world);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return MysticalUtil.hasCustomData(stack);
    }

    @Override
    public Optional<TooltipData> getTooltipData(ItemStack stack) {
        var storageInventory = new SingleItemStackingInventory(stack, ModConfig.BookOfFarmingMaxStacks);
        if(storageInventory.isEmpty())
            return Optional.empty();


        return Optional.of(BookContentsTooltipData.fromInventory(storageInventory));
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        if(Screen.hasShiftDown()){
            tooltip.add(Text.translatable("tooltip.mystical_index.farming_book_shift0"));
            tooltip.add(Text.translatable("tooltip.mystical_index.farming_book_shift1"));
        } else {
            tooltip.add(Text.translatable("tooltip.mystical_index.farming_book"));
        }
        super.appendTooltip(stack, context, tooltip, type);
    }
}
