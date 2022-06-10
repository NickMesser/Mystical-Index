package net.messer.mystical_index.item.custom.page;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

import java.util.List;

public abstract class AttributePageItem extends PageItem implements TypeDependentPage {
    public static void increaseIntAttribute(NbtCompound nbt, String attribute, int amount) {
        var attributeValue = nbt.getInt(attribute);
        nbt.putInt(attribute, attributeValue + amount);
    }

    public List<AttributePageItem> getIncompatibleAttributes(ItemStack page) {
        return List.of();
    }

    public boolean bookCanHaveMultiple(ItemStack page) {
        return true;
    }

    public abstract void appendAttributes(ItemStack page, NbtCompound nbt);

    @Override
    public void onCraftToBook(ItemStack page, ItemStack book) {
        super.onCraftToBook(page, book);

        appendAttributes(page, book.getOrCreateSubNbt("attributes"));
    }
}
