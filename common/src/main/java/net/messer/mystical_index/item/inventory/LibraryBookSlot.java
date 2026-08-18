package net.messer.mystical_index.item.inventory;

import net.messer.mystical_index.screen.LibraryInventoryScreenHandler;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;

public class LibraryBookSlot extends Slot {
    private final LibraryInventoryScreenHandler handler;
    public LibraryBookSlot(LibraryInventoryScreenHandler handler, Container inventory, int index, int x, int y) {
        super(inventory, index, x, y);
        this.handler = handler;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return this.handler.isStorageBook(stack);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return 1;
    }
}
