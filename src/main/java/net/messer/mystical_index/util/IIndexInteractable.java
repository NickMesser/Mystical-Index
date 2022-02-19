package net.messer.mystical_index.util;

import net.minecraft.item.ItemStack;

import java.util.List;

public interface IIndexInteractable {
    default List<ItemStack> countItems(Request request) {
        return extractItems(request, false);
    }

    default List<ItemStack> extractItems(Request request) {
        return extractItems(request, true);
    }

    List<ItemStack> extractItems(Request request, boolean apply);
}
