package net.messer.mystical_index.item.custom;

import net.messer.mystical_index.block.entity.LibraryBlockEntity;
import net.messer.mystical_index.item.inventory.SingleItemStackingInventory;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.SimpleInventory;
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
}
