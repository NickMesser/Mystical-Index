package net.messer.mystical_index.item.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

// One entry per stored type rather than one per slot: a tier IV Book of Holding has hundreds of
// slots, and a bundle style grid of all of them is unreadable.
public record BookContentsTooltipData(List<TypeSummary> summaries, int used, int capacity)
        implements TooltipComponent {

    public record TypeSummary(ItemStack representative, long total) {
    }

    // Groups every non-empty slot by type. For a bucketed book this gives the same answer as
    // walking buckets, since a type spread over several buckets merges either way.
    public static BookContentsTooltipData fromInventory(Container inventory) {
        return fromInventories(inventory);
    }

    /**
     * The same grouping across several containers at once, in the order given.
     *
     * <p>A Book of Farming has two distinct stores - its four output slots and, on a migrated book,
     * whatever its old single inventory still holds - and both are real, drainable contents. The
     * player should see one list, not learn that internal split.
     */
    public static BookContentsTooltipData fromInventories(Container... inventories) {
        var summaries = new ArrayList<TypeSummary>();

        for (Container inventory : inventories) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            var stack = inventory.getItem(slot);
            if (stack.isEmpty())
                continue;

            boolean merged = false;
            for (int i = 0; i < summaries.size(); i++) {
                var existing = summaries.get(i);
                if (ItemStack.isSameItemSameComponents(existing.representative(), stack)) {
                    summaries.set(i, new TypeSummary(existing.representative(), existing.total() + stack.getCount()));
                    merged = true;
                    break;
                }
            }

            if (!merged)
                summaries.add(new TypeSummary(stack, stack.getCount()));
        }
        }

        return new BookContentsTooltipData(summaries, summaries.size(), summaries.size());
    }
}
