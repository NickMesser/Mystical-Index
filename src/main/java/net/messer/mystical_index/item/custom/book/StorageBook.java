package net.messer.mystical_index.item.custom.book;

import net.minecraft.item.ItemStack;


public class StorageBook extends InventoryBookItem {

    public StorageBook(Settings settings) {
        super(settings);
    }

    @Override
    public int getMaxTypes(ItemStack book) {
        return 32;
    }

    @Override
    public int getMaxStack(ItemStack book) {
        return 8;
    }
}
