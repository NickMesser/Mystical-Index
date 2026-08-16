package net.messer.mystical_index.item.custom;

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
import net.messer.mystical_index.item.inventory.BookContentsTooltipData;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.item.TooltipData;
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

import java.util.*;

public class FarmingBook extends BaseGeneratingBook {
    public FarmingBook(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if(context.getWorld().isClient)
            return super.useOnBlock(context);

        var itemStack = context.getStack();
        var world = context.getWorld();
        var player = context.getPlayer();
        var pos = context.getBlockPos();
        var compound = itemStack.getOrCreateNbt();

        if(compound.contains("cropBlock")) { // check if book has a cropBlock assigned to it
            // Sneak-right-click a block while bound to unbind the book (mirrors MagnetismBook's reset).
            if(player != null && player.isSneaking()){
                compound.remove("cropBlock");
                itemStack.removeCustomName();
                player.sendMessage(Text.translatable("message.mystical_index.farming_unbound"), true);
            }
            return super.useOnBlock(context);
        }

        Block block = world.getBlockState(pos).getBlock();
        if(block instanceof CropBlock cropBlock){
            // Do not let the book grief crops the player cannot modify (protected/adventure).
            if(player == null || !world.canPlayerModifyAt(player, pos))
                return super.useOnBlock(context);

            addCrop(itemStack, cropBlock);
            updateUseTime(itemStack, world.getTime());
            world.breakBlock(pos, true, player);
        }

        return super.useOnBlock(context);
    }

    public void addCrop(ItemStack stack, CropBlock cropBlock){
        NbtCompound compound = stack.getOrCreateNbt();
        var cropBlockId = Registries.BLOCK.getId(cropBlock).toString();
        compound.putString("cropBlock", cropBlockId);
        stack.setCustomName(Text.translatable("item.mystical_index.farming_book.named", cropBlock.asItem().getName()));
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
        return new SingleItemStackingInventory(stack, ModConfig.BookOfFarmingMaxStacks);
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

        var currentTime = world.getTime();
        var lastUsedTime = compound.getLong("lastUsedTime");
        var difference = currentTime - lastUsedTime;
        if(difference < 0){
            updateUseTime(stack, currentTime);
            return;
        }

        var crop = getCrop(stack);
        if(crop == null)
            return;

        var cropId = Registries.BLOCK.getId(crop).toString();
        var cooldown = ModConfig.BookOfFarmingDefaultCooldown;
        if(ModConfig.BookOfFarmingCooldowns.get(cropId) != null)
            cooldown = ModConfig.BookOfFarmingCooldowns.get(cropId);


        if(difference < (cooldown * 20L))
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

        if(!stack.hasNbt())
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

        if (!stack.hasNbt())
            return;

        tryGenerateResources(stack, world);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return stack.hasNbt();
    }

    @Override
    public Optional<TooltipData> getTooltipData(ItemStack stack) {
        var storageInventory = new SingleItemStackingInventory(stack, ModConfig.BookOfFarmingMaxStacks);
        if(storageInventory.isEmpty())
            return Optional.empty();


        return Optional.of(BookContentsTooltipData.fromInventory(storageInventory));
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if(Screen.hasShiftDown()){
            tooltip.add(Text.translatable("tooltip.mystical_index.farming_book_shift0"));
            tooltip.add(Text.translatable("tooltip.mystical_index.farming_book_shift1"));
        } else {
            tooltip.add(Text.translatable("tooltip.mystical_index.farming_book"));
        }
        super.appendTooltip(stack, world, tooltip, context);
    }
}
