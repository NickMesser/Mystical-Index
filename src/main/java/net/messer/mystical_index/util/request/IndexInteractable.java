package net.messer.mystical_index.util.request;

import net.messer.mystical_index.util.ContentsIndex;
import net.minecraft.item.ItemStack;

import java.util.List;

public interface IndexInteractable {
    ContentsIndex getContents();

    /**
     * <b>Use only while processing a request! Otherwise use request.apply() instead.</b>
     */
    List<ItemStack> extractItems(ExtractionRequest request, boolean apply);

    /**
     * <b>Use only while processing a request! Otherwise use request.apply() instead.</b>
     */
    void insertStack(InsertionRequest request);
}
