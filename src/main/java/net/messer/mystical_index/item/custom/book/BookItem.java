package net.messer.mystical_index.item.custom.book;

import net.messer.mystical_index.item.custom.page.PageItem;
import net.messer.mystical_index.util.PageRegistry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtString;

import java.util.function.Consumer;

public abstract class BookItem extends Item {
    public static final String TYPE_PAGE_TAG = "type_page";
    public static final String ATTRIBUTE_PAGES_TAG = "attribute_pages";
    public static final String ACTION_PAGE_TAG = "action_page";

    public BookItem(Settings settings) {
        super(settings);
    }

    @Override
    public boolean canBeNested() {
        return false;
    }

    public void forEachPageType(ItemStack book, Consumer<PageItem> consumer) {
        var pages = book.getOrCreateNbt().getList(PAGES_TAG, NbtElement.STRING_TYPE);
        PageRegistry.REGISTERED_PAGES.forEach((identifier, item) -> {
            if (pages.contains(NbtString.of(identifier.toString())))
                consumer.accept(item);
        });
    }
}
