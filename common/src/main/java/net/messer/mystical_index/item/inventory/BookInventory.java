package net.messer.mystical_index.item.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public interface BookInventory extends Container {

    /**
     * Inserts as much of {@code stack} as fits, mutating it downward by the amount taken.
     * With {@code allowNewTypes} false only types the book already holds are topped up.
     *
     * @return true only when the whole stack was absorbed.
     */
    boolean tryAddStack(ItemStack stack, boolean allowNewTypes);
}
