package net.messer.mystical_index.item.custom.page.type;

import net.minecraft.item.ItemStack;

public class FoodStorageTypePage extends ItemStorageTypePage{
    public FoodStorageTypePage(String id) {
        super(id);
    }

    @Override
    protected boolean canInsert(ItemStack book, ItemStack itemStack) {
        return (itemStack.getItem().isFood());
    }
}
