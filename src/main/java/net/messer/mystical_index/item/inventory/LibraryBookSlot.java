package net.messer.mystical_index.item.inventory;

import net.messer.mystical_index.screen.LibraryInventoryScreenHandler;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

public class LibraryBookSlot extends Slot {
    private final LibraryInventoryScreenHandler handler;
    public LibraryBookSlot(LibraryInventoryScreenHandler handler, Inventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
        this.handler = handler;
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        return this.handler.isStorageBook(stack);
    }

    @Override
    public int getMaxItemCount() {
        return 1;
    }

    @Override
    public int getMaxItemCount(ItemStack stack) {
        return 1;
    }
}
