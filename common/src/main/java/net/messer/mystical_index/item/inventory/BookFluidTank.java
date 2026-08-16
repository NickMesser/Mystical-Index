package net.messer.mystical_index.item.inventory;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

/**
 * A single-fluid tank holding one fluid and an amount in droplets.
 *
 * <p>The Book of Fluid used to hold Fabric's {@code SingleVariantStorage<FluidVariant>} for this,
 * but it was only ever an internal container: it is never exposed to pipes and never joins anyone
 * else's transaction, so the only things the transfer API contributed were the capacity clamp and
 * a "write the book back when the change sticks" callback. Both are reproduced here so the class
 * can live in shared code.
 *
 * <p>Amounts stay in Fabric's droplet unit ({@value #BUCKET} per bucket) because that is what is
 * already written into existing books' NBT, and fluids normalise flowing to still exactly as
 * {@code FluidVariant.of} did, so a book filled from a flowing block still stacks with one filled
 * from a source.
 */
public class BookFluidTank {

    /** Droplets in one bucket. Same value Fabric's {@code FluidConstants.BUCKET} uses. */
    public static final long BUCKET = 81000L;

    public Fluid fluid = Fluids.EMPTY;
    public long amount = 0;

    private final long capacity;
    private final Runnable onChanged;

    public BookFluidTank(long capacity, Runnable onChanged) {
        this.capacity = capacity;
        this.onChanged = onChanged;
    }

    /**
     * Normalises a flowing fluid to its still form, matching {@code FluidVariant.of}. Without this
     * a book filled from a flowing block would hold {@code flowing_water} and refuse to stack with
     * water taken from a source.
     */
    public static Fluid normalize(Fluid fluid) {
        return fluid instanceof FlowableFluid flowable ? flowable.getStill() : fluid;
    }

    public long getCapacity() {
        return capacity;
    }

    public boolean isBlank() {
        return fluid == Fluids.EMPTY;
    }

    /** How much of {@code fluid} would be accepted right now, without changing anything. */
    public long simulateInsert(Fluid fluid, long maxAmount) {
        var inserted = normalize(fluid);
        if (inserted == Fluids.EMPTY || maxAmount <= 0)
            return 0;
        if (!isBlank() && inserted != this.fluid)
            return 0;

        return Math.max(0, Math.min(maxAmount, capacity - amount));
    }

    /**
     * Inserts up to {@code maxAmount} droplets and returns how much was actually taken. A tank
     * holding a different fluid accepts nothing.
     */
    public long insert(Fluid fluid, long maxAmount) {
        long insertedAmount = simulateInsert(fluid, maxAmount);
        if (insertedAmount <= 0)
            return 0;

        this.fluid = normalize(fluid);
        this.amount += insertedAmount;
        onChanged.run();
        return insertedAmount;
    }

    /**
     * Extracts up to {@code maxAmount} droplets of {@code fluid} and returns how much came out.
     * Draining the tank empty clears the stored fluid, so the book reads as blank again.
     */
    public long extract(Fluid fluid, long maxAmount) {
        var extracted = normalize(fluid);
        if (extracted == Fluids.EMPTY || maxAmount <= 0 || extracted != this.fluid)
            return 0;

        long extractedAmount = Math.min(maxAmount, amount);
        if (extractedAmount <= 0)
            return 0;

        this.amount -= extractedAmount;
        if (this.amount == 0)
            this.fluid = Fluids.EMPTY;

        onChanged.run();
        return extractedAmount;
    }

    /**
     * The display name of a fluid, reproducing what {@code FluidVariantAttributes.getName} resolved
     * to: the name of the fluid's block, falling back to a key built from the fluid id for fluids
     * that have no block of their own.
     */
    public static Text getName(Fluid fluid) {
        Block fluidBlock = fluid.getDefaultState().getBlockState().getBlock();

        if (fluid != Fluids.EMPTY && fluidBlock == Blocks.AIR)
            return Text.translatable(Util.createTranslationKey("block", Registries.FLUID.getId(fluid)));

        return fluidBlock.getName();
    }
}
