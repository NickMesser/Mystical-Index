package net.messer.mystical_index.util.request;

import net.messer.mystical_index.item.custom.book.MysticalBookItem;
import net.messer.mystical_index.item.custom.page.type.ItemStorageTypePage;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class InsertionRequest extends Request {
    private final ItemStack itemStack;
    // TODO priority?

    public InsertionRequest(ItemStack itemStack) {
        super(itemStack.getCount());
        this.itemStack = itemStack;
    }

    @Override
    public void apply(LibraryIndex index, boolean apply) {
        var sources = new ArrayList<>(index.getSources());

        // Sort sources by priority.
        sources.sort(Comparator.comparingInt((source) -> {
            var book = source.getBook();
            if (book.getItem() instanceof MysticalBookItem bookItem) {
                if (bookItem.getTypePage(book) instanceof ItemStorageTypePage page) {
                    return page.getInsertPriority(book, getItemStack());
                }
            }
            return 0;
        }));
        Collections.reverse(sources);

        // Insert into sources in order of priority.
        for (IndexSource source : sources) {
            if (isSatisfied()) break;

            var book = source.getBook();
            if (book.getItem() instanceof MysticalBookItem bookItem) {
                if (bookItem.getTypePage(book) instanceof ItemStorageTypePage page) {
                    int amountInserted = page.tryAddItem(book, getItemStack());
                    satisfy(amountInserted);

                    runBlockAffectedCallback(source.getBlockEntity());
                }
            }
        }
    }

    @Override
    public void satisfy(int amount) {
        itemStack.decrement(amount);
        super.satisfy(amount);
    }

    public ItemStack getItemStack() {
        return itemStack;
    }
}
