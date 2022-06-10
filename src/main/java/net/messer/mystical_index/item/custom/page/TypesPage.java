package net.messer.mystical_index.item.custom.page;

import net.messer.mystical_index.item.custom.PageItem;
import net.minecraft.item.ItemStack;

public class TypesPage extends PageItem {
    @Override
    public int getTypesIncrease(ItemStack page) {
        return 2;
    }

    @Override
    public int getLinksIncrease(ItemStack page, boolean autoIndexing) {
        return autoIndexing ? 1 : 2;
    }

    @Override
    public int getColor() {
        return 0x40b3af;
    }
}
