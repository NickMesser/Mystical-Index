package net.messer.mystical_index.item.custom.page.attribute;

import net.messer.mystical_index.item.custom.page.type.IndexingTypePage;
import net.minecraft.item.ItemStack;

public class RangePage extends IndexingTypePage.IndexingAttributePage {
    @Override
    public int getRangeMultiplier(ItemStack page, boolean linked) {
        return 2;
    }

    @Override
    public int getColor() {
        return 0xff0000;
    }
}
