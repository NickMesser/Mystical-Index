package net.messer.mystical_index.item.inventory;

import net.messer.config.ModConfig;
import net.messer.util.MysticalUtil;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class SingleFluidStackingInventory {

    // The stored fluid used to be written by FluidVariant's codec, which lays it down as
    // {fluidVariant: {fluid: "<id>"}, amount: <long>}. The same two keys are read and written by
    // hand here so books written before the multi-loader split still resolve, and so decoding no
    // longer has to reach for a registry lookup it may not have.
    private static final String FLUID_KEY = "fluidVariant";
    private static final String FLUID_ID_KEY = "fluid";
    private static final String AMOUNT_KEY = "amount";

    public final ItemStack stack;

    public final BookFluidTank fluidStorage =
            new BookFluidTank(ModConfig.FluidBookMaxBuckets * BookFluidTank.BUCKET, this::markDirty);

    public boolean hasFluid(){
        return fluidStorage.amount != 0;
    }

    public SingleFluidStackingInventory(ItemStack stack){
        this.stack = stack;
        if(MysticalUtil.hasCustomData(stack))
            readNbt(stack);
    }

    public void markDirty(){
        writeNbt();
    }

    public void writeNbt(){
        // Merge into the existing compound: replacing it wholesale would drop the custom name
        // and everything else already on the stack.
        NbtCompound tag = MysticalUtil.copyCustomData(stack);

        NbtCompound fluid = new NbtCompound();
        fluid.putString(FLUID_ID_KEY, Registries.FLUID.getId(fluidStorage.fluid).toString());
        tag.put(FLUID_KEY, fluid);
        tag.putLong(AMOUNT_KEY, fluidStorage.amount);

        MysticalUtil.setCustomData(stack, tag);
    }

    public void readNbt(ItemStack stack){
        NbtCompound tag = MysticalUtil.getCustomData(stack);
        if(tag == null)
            return;

        // Read only. The custom name is updated from FluidBook's fill/empty paths instead of
        // here: setting it during a read ran every render frame, reverted anvil renames and
        // never cleared when the book was drained.
        var id = Identifier.tryParse(tag.getCompound(FLUID_KEY).getString(FLUID_ID_KEY));
        // An unknown id (a fluid from a removed mod) degrades to empty rather than throwing, the
        // same way the codec parse used to fall back to a blank variant.
        var fluid = id == null ? null : Registries.FLUID.get(id);
        fluidStorage.fluid = fluid == null ? Fluids.EMPTY : BookFluidTank.normalize(fluid);
        fluidStorage.amount = tag.getLong(AMOUNT_KEY);

        if (fluidStorage.fluid == Fluids.EMPTY)
            fluidStorage.amount = 0;
    }
}
