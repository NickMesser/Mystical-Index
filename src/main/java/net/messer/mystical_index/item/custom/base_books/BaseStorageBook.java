package net.messer.mystical_index.item.custom.base_books;

import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.item.inventory.SingleItemStackingInventory;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class BaseStorageBook extends Item {
    public BaseStorageBook(Settings settings) {
        super(settings);
    }
    public SingleItemStackingInventory getInventory(ItemStack stack){
        return new SingleItemStackingInventory(stack, 64);
    }

    public void customBookTick(ItemStack stack, World world, Entity entity){
        MysticalIndex.LOGGER.info("Entity ticking a book!");
    }
    public void customBookTick(ItemStack stack, World world, BlockEntity be){
        MysticalIndex.LOGGER.info("BlockEntity ticking a book!");
    }
}
