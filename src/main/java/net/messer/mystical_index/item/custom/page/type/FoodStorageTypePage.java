package net.messer.mystical_index.item.custom.page.type;

import net.minecraft.item.ItemStack;

public class FoodStorageTypePage extends ItemStorageTypePage{
    @Override
    public int getColor() {
        return 0x50a99b;
    }

    public FoodStorageTypePage(String id) {
        super(id);
    }

    @Override
    protected boolean canInsert(ItemStack book, ItemStack itemStack) {
        if(itemStack.isFood()) return true;

        return super.canInsert(book, itemStack);
    }
}
