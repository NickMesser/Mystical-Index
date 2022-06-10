package net.messer.mystical_index.item.custom.page;

import net.messer.mystical_index.item.custom.PageItem;
import net.minecraft.item.ItemStack;

public class StacksPage extends PageItem {
    @Override
    public int getStacksIncrease(ItemStack page) {
        return 1;
    }

    @Override
    public int getRangeIncrease(ItemStack page, boolean autoIndexing) {
        return autoIndexing ? 2 : 20;
    }

    @Override
    public int getColor() {
        return 0x49a337;
    }
}
