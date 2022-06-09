package net.messer.mystical_index.item.custom.book;

import net.minecraft.item.Item;

public abstract class BookItem extends Item {
    public BookItem(Settings settings) {
        super(settings);
    }

    @Override
    public boolean canBeNested() {
        return false;
    }
}
