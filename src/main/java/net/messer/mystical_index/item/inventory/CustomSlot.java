package net.messer.mystical_index.item.inventory;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

public class CustomSlot extends Slot {
    public CustomSlot(Inventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
    }

    @Override
    public int getMaxItemCount() {
        return 256;
    }

    @Override
    public int getMaxItemCount(ItemStack stack) {
        return 256;
    }
}
