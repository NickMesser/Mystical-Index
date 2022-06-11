package net.messer.mystical_index.item.custom.page.attribute;

import net.messer.mystical_index.item.custom.page.type.ItemStorageTypePage;
import net.minecraft.item.ItemStack;

public class StacksPage extends ItemStorageTypePage.ItemStorageAttributePage {
    @Override
    public int getStacksMultiplier(ItemStack page) {
        return 2;
    }

    @Override
    public int getColor() {
        return 0xffff00;
    }
}
