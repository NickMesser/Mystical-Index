package net.messer.mystical_index.item.custom.base_books;

import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.item.inventory.BookInventory;
import net.messer.mystical_index.item.inventory.SingleItemStackingInventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class BaseStorageBook extends Item {
    public BaseStorageBook(Item.Properties settings) {
        super(settings);
    }
    public BookInventory getInventory(ItemStack stack){
        return new SingleItemStackingInventory(stack, 64);
    }

    public void customBookTick(ItemStack stack, Level world, Entity entity){
    }
    public void customBookTick(ItemStack stack, Level world, BlockEntity be){
    }
}
