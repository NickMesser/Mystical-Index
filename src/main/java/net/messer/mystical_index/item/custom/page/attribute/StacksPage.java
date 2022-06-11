package net.messer.mystical_index.item.custom.page.attribute;

import net.messer.mystical_index.item.custom.page.type.ItemStorageTypePage;
import net.minecraft.item.ItemStack;

public class StacksPage extends ItemStorageTypePage.ItemStorageAttributePage {
    @Override
    public int getStacksIncrease(ItemStack page) {
        return 1;
    }

    @Override
    public int getColor() {
        return 0xffff00;
    }
}
