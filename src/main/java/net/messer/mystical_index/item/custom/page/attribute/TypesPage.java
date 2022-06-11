package net.messer.mystical_index.item.custom.page.attribute;

import net.messer.mystical_index.item.custom.page.type.ItemStorageTypePage;
import net.minecraft.item.ItemStack;

public class TypesPage extends ItemStorageTypePage.ItemStorageAttributePage {
    @Override
    public int getTypesMultiplier(ItemStack page) {
        return 2;
    }

    @Override
    public int getColor() {
        return 0x00ffff;
    }
}
