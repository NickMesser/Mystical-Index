package net.messer.mystical_index.util.request;

import net.messer.mystical_index.util.ContentsIndex;
import net.minecraft.item.ItemStack;

import java.util.List;

public interface IIndexInteractable {
    ContentsIndex getContents();

    default List<ItemStack> countItems(ExtractionRequest request) {
        return extractItems(request, false);
    }

    default List<ItemStack> extractItems(ExtractionRequest request) {
        return extractItems(request, true);
    }

    List<ItemStack> extractItems(ExtractionRequest request, boolean apply);

//    default void insertStack(ItemStack itemStack) {
//        insertStack(itemStack, true);
//    }

    void insertStack(InsertionRequest request);
}
