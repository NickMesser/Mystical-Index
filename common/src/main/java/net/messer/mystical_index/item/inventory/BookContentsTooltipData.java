package net.messer.mystical_index.item.inventory;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

// One entry per stored type rather than one per slot: a tier IV Book of Holding has hundreds of
// slots, and a bundle style grid of all of them is unreadable.
public record BookContentsTooltipData(List<TypeSummary> summaries, int used, int capacity)
        implements TooltipData {

    public record TypeSummary(ItemStack representative, long total) {
    }

    // Groups every non-empty slot by type. For a bucketed book this gives the same answer as
    // walking buckets, since a type spread over several buckets merges either way.
    public static BookContentsTooltipData fromInventory(Inventory inventory) {
        var summaries = new ArrayList<TypeSummary>();

        for (int slot = 0; slot < inventory.size(); slot++) {
            var stack = inventory.getStack(slot);
            if (stack.isEmpty())
                continue;

            boolean merged = false;
            for (int i = 0; i < summaries.size(); i++) {
                var existing = summaries.get(i);
                if (ItemStack.areItemsAndComponentsEqual(existing.representative(), stack)) {
                    summaries.set(i, new TypeSummary(existing.representative(), existing.total() + stack.getCount()));
                    merged = true;
                    break;
                }
            }

            if (!merged)
                summaries.add(new TypeSummary(stack, stack.getCount()));
        }

        return new BookContentsTooltipData(summaries, summaries.size(), summaries.size());
    }
}
