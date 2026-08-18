package net.messer.util;

import net.minecraft.world.item.ItemStack;

/**
 * A book whose enchantment glint reflects what it currently holds.
 *
 * <p>Items used to answer this per render frame through {@code Item.hasGlint}, which no longer
 * exists: the glint is a data component now, so it has to be written when the contents change
 * rather than computed when they are drawn. Each book keeps its own rule here, and
 * {@link MysticalUtil#setCustomData} applies it on every write, which is the single point every
 * book's data passes through - so the component can never go stale.
 */
public interface GlintingBook {
    boolean shouldGlint(ItemStack stack);
}
