package net.messer.mystical_index.item.custom.page.type;

import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;

public class FoodStorageTypePage extends ItemStorageTypePage{
    public FoodStorageTypePage(String id) {
        super(id);
    }

    @Override
    public int getColor() {
        return 0x008811;
    }

    @Override
    public MutableText getTypeDisplayName() {
        return super.getTypeDisplayName().formatted(Formatting.DARK_GREEN);
    }

    @Override
    protected boolean canInsert(ItemStack book, ItemStack itemStack) {
        if (!itemStack.isFood()) return false;

        return super.canInsert(book, itemStack);
    }
}
