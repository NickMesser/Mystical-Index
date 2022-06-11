package net.messer.mystical_index.item.custom.page.attribute;

import net.messer.mystical_index.item.custom.page.type.IndexingTypePage;
import net.minecraft.item.ItemStack;

public class LinksPage extends IndexingTypePage.IndexingAttributePage {
    @Override
    public int getLinksIncrease(ItemStack page) {
        return 2;
    }

    @Override
    public int getColor() {
        return 0x00ff00;
    }
}
