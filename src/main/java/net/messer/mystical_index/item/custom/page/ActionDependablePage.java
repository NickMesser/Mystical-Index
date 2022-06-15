package net.messer.mystical_index.item.custom.page;

import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface ActionDependablePage {
    /**
     * List of action pages that this page can be used with.
     * If null is returned, the page can be used with any action page.
     */
    @Nullable
    default List<ActionPageItem> getCompatibleActions(ItemStack page) {
        return null;
    }
}
