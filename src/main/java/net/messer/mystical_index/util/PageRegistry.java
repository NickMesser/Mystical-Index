package net.messer.mystical_index.util;

import net.messer.mystical_index.item.custom.page.PageItem;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;

import java.util.HashMap;

public class PageRegistry {
    public static final HashMap<Identifier, PageItem> REGISTERED_PAGES = new HashMap<>();

    public static void registerPage(Identifier itemId, Item item) {
        if (item instanceof PageItem pageItem)
            REGISTERED_PAGES.put(itemId, pageItem);
    }

    public static PageItem getPage(Identifier itemId) {
        return REGISTERED_PAGES.get(itemId);
    }
}
