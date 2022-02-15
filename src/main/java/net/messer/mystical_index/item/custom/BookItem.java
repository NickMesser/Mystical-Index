package net.messer.mystical_index.item.custom;

import eu.pb4.polymer.api.item.PolymerItem;
import net.minecraft.item.Item;

public abstract class BookItem extends Item implements PolymerItem {
    public BookItem(Settings settings) {
        super(settings);
    }

    @Override
    public boolean canBeNested() {
        return false;
    }
}
