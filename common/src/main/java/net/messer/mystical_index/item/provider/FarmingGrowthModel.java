package net.messer.mystical_index.item.provider;

import net.messer.config.ModConfig;
import net.messer.mystical_index.item.custom.FarmingBook;
import net.messer.mystical_index.item.inventory.FarmingBookInventory;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;

import java.util.List;

/**
 * Publishes a Book of Farming's growth as a number the item model can dispatch on.
 *
 * <p>Custom item-model properties are not open to mods in this version - the registry behind them,
 * {@code RangeSelectItemModelProperties.ID_MAPPER}, is private, and reaching it would need an
 * access widener on Fabric plus an access transformer on NeoForge. Vanilla's own
 * {@code minecraft:custom_model_data} property is the way through: it reads a float straight off
 * the stack, so writing progress there lets a plain {@code range_dispatch} in the item definition
 * pick the model with no loader-specific code at all.
 *
 * <p>Written from the shared custom-data path rather than computed at render time, for the same
 * reason the enchantment glint is: the value has to already be on the stack when the client draws
 * it, and hanging it off the single write path every mutation goes through means no future code
 * path can forget to update it.
 */
public class FarmingGrowthModel {

    /** Index into the component's float list; the item definition dispatches on the same index. */
    private static final int GROWTH_INDEX = 0;

    public static void applyStage(ItemStack stack) {
        if (!(stack.getItem() instanceof FarmingBook))
            return;

        float progress = progressOf(stack);
        var existing = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        if (existing != null && !existing.floats().isEmpty()
                && Math.abs(existing.floats().get(GROWTH_INDEX) - progress) < 0.001F)
            return;

        stack.set(DataComponents.CUSTOM_MODEL_DATA,
                new CustomModelData(List.of(progress), List.of(), List.of(), List.of()));
    }

    /**
     * How far through a cycle the book is, 0.0 to 1.0.
     *
     * <p>An unplanted book reads 0 so it falls back to the closed model. A book whose outputs are
     * full sits at 1.0 rather than rolling over, which is what makes the "waiting for space" state
     * look fully grown instead of resetting to a sprout.
     */
    public static float progressOf(ItemStack stack) {
        var inventory = new FarmingBookInventory(stack);
        if (!inventory.isPlanted())
            return 0.0F;

        long planted = inventory.plantedAt();
        if (planted <= 0L)
            return 0.0F;

        long total = Math.max(1L, ModConfig.BookOfFarmingGrowthTicks);
        long elapsed = FarmingBook.lastSeenTime(stack) - planted;
        if (elapsed <= 0L)
            return 0.0F;

        return (float) Math.min(1.0, elapsed / (double) total);
    }
}
