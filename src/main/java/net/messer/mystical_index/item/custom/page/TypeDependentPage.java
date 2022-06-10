package net.messer.mystical_index.item.custom.page;

import net.minecraft.item.ItemStack;

import java.util.List;

public interface TypeDependentPage {
    List<TypePageItem> getCompatibleTypes(ItemStack page);
}
