package net.messer.mystical_index.item.custom;

import net.fabricmc.loader.impl.util.StringUtil;
import net.messer.config.ModConfig;
import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.block.entity.LibraryBlockEntity;
import net.messer.mystical_index.item.custom.base_books.BaseGeneratingBook;
import net.messer.mystical_index.item.inventory.SingleItemStackingInventory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
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
        var compound = itemStack.getOrCreateNbt();

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
        NbtCompound compound = stack.getOrCreateNbt();
        var cropBlockId = Registries.BLOCK.getId(cropBlock).toString();
        compound.putString("cropBlock", cropBlockId);
        stack.setCustomName(Text.of("Book Of Farming: " + StringUtil.capitalize(cropBlock.asItem().toString())));
    }

    public CropBlock getCrop(ItemStack stack){
        if(!stack.hasNbt())
            return null;

        NbtCompound compound = stack.getOrCreateNbt();
        var cropBlockId = compound.getString("cropBlock");
        var block = Registries.BLOCK.get(new Identifier(cropBlockId));
        if (!(block instanceof CropBlock))
        {
            compound.remove("cropBlock");
            return null;
        }
        return (CropBlock) Registries.BLOCK.get(new Identifier(cropBlockId));
    }

    @Override
    public SingleItemStackingInventory getInventory(ItemStack stack) {
        return new SingleItemStackingInventory(stack, 5);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        customBookTick(stack, world, entity);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    public void tryGenerateResources(ItemStack stack, World world){
        var compound = stack.getNbt();
        if(compound == null)
            return;

        var currentTime = world.getTime() % 24000;
        var lastUsedTime = compound.getLong("lastUsedTime");
        var difference = currentTime - lastUsedTime;
        if(difference < 0)
            updateUseTime(stack, currentTime);

        var crop = getCrop(stack);
        var cropId = Registries.BLOCK.getId(crop).toString();
        var cooldown = ModConfig.BookOfFarmingDefaultCooldown;
        if(BookOfFarmingCooldowns.get(cropId) != null)
            cooldown = BookOfFarmingCooldowns.get(cropId);


        if((difference) < (cooldown * 20))
            return;

        updateUseTime(stack, currentTime);
        if(crop == null)
            return;

        BlockState cropGrownState = crop.getDefaultState().with(CropBlock.AGE, crop.getMaxAge());

        var bookInventory = getInventory(stack);
        var cropLoot = Block.getDroppedStacks(cropGrownState, (ServerWorld) world, new BlockPos(0,0,0), null);
        for (ItemStack itemStack : cropLoot) {
            if(itemStack.getItem() != crop.asItem()){
                itemStack.setCount(itemStack.getCount() + new Random().nextInt(0, 4));
                if(!bookInventory.tryAddStack(itemStack, true))
                    itemStack.setCount(0);
            }
            if(!bookInventory.tryAddStack(itemStack, true))
                itemStack.setCount(0);
        }
    }

    @Override
    public void customBookTick(ItemStack stack, World world, BlockEntity be) {
        if (world.isClient)
            return;

        if(!stack.hasNbt())
            return;

        if(!stack.getNbt().contains("indexed"))
            return;

        if(!(be instanceof LibraryBlockEntity))
            return;

        tryGenerateResources(stack, world);
    }

    @Override
    public void customBookTick(ItemStack stack, World world, Entity entity) {
        if(world.isClient)
            return;

        if(!(entity instanceof PlayerEntity player))
            return;

        if (!stack.hasNbt())
            return;

        tryGenerateResources(stack, world);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return stack.hasNbt();
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        SingleItemStackingInventory inventory = this.getInventory(stack);


        List<String> itemNames = new ArrayList<>();
        for (ItemStack inventoryStack : inventory.storedItems) {
            var itemName = inventoryStack.getItem().getName().getString();
            if(itemNames.contains(itemName) || Objects.equals(itemName, Items.AIR.getName().getString())) {
            }
            else{
                itemNames.add(itemName);
            }
        }

        for(String itemName : itemNames){
            var currentAmount = 0;
            for(ItemStack inventoryStack : inventory.storedItems) {
                if(inventoryStack.getItem().getName().getString().equals(itemName))
                    currentAmount += inventoryStack.getCount();
            }

            tooltip.add(Text.literal("§a"+currentAmount + "x " + "§f" + itemName));
        }

        if(stack.getNbt() != null){
            var compound = stack.getNbt();

            if(compound.contains("indexed"))
                tooltip.add(Text.literal("§aIndexed"));
        }
        if(Screen.hasShiftDown()){
            tooltip.add(Text.translatable("tooltip.mystical_index.farming_book_shift0"));
            tooltip.add(Text.translatable("tooltip.mystical_index.farming_book_shift1"));
        } else {
            tooltip.add(Text.translatable("tooltip.mystical_index.farming_book"));
        }
        super.appendTooltip(stack, world, tooltip, context);
    }
}
