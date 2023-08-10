package net.messer.mystical_index.block.custom;

import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;

public class SimpleBlockInventory extends SimpleInventory {
    public void setStack(int slot, ItemStack stack, int index, Inventory blockInventory) {
        super.setStack(0, stack);
    }
}
