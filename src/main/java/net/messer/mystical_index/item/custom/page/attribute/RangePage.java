package net.messer.mystical_index.item.custom.page.attribute;

import net.messer.mystical_index.item.custom.page.type.IndexingTypePage;
import net.minecraft.item.ItemStack;

public class RangePage extends IndexingTypePage.IndexingAttributePage {
    @Override
    public int getRangeIncrease(ItemStack page, boolean linked) {
        return linked ? 20 : 2;
    }

    @Override
    public int getColor() {
        return 0xff0000;
    }
}
