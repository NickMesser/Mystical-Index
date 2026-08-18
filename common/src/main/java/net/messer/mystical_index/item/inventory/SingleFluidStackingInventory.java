package net.messer.mystical_index.item.inventory;

import net.minecraft.core.registries.Registries;
import net.messer.config.ModConfig;
import net.messer.util.MysticalUtil;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

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
            new BookFluidTank(ModConfig.FluidBookMaxBuckets * BookFluidTank.BUCKET, this::setChanged);

    public boolean hasFluid(){
        return fluidStorage.amount != 0;
    }

    public SingleFluidStackingInventory(ItemStack stack){
        this.stack = stack;
        if(MysticalUtil.hasCustomData(stack))
            readNbt(stack);
    }

    public void setChanged(){
        writeNbt();
    }

    public void writeNbt(){
        // Merge into the existing compound: replacing it wholesale would drop the custom name
        // and everything else already on the stack.
        CompoundTag tag = MysticalUtil.copyCustomData(stack);

        CompoundTag fluid = new CompoundTag();
        fluid.putString(FLUID_ID_KEY, BuiltInRegistries.FLUID.getKey(fluidStorage.fluid).toString());
        tag.put(FLUID_KEY, fluid);
        tag.putLong(AMOUNT_KEY, fluidStorage.amount);

        MysticalUtil.setCustomData(stack, tag);
    }

    public void readNbt(ItemStack stack){
        CompoundTag tag = MysticalUtil.getCustomData(stack);
        if(tag == null)
            return;

        // Read only. The custom name is updated from FluidBook's fill/empty paths instead of
        // here: setting it during a read ran every render frame, reverted anvil renames and
        // never cleared when the book was drained.
        var id = Identifier.tryParse(tag.getCompoundOrEmpty(FLUID_KEY).getStringOr(FLUID_ID_KEY, ""));
        // An unknown id (a fluid from a removed mod) degrades to empty rather than throwing, the
        // same way the codec parse used to fall back to a blank variant.
        var fluid = id == null ? null : BuiltInRegistries.FLUID.getValue(id);
        fluidStorage.fluid = fluid == null ? Fluids.EMPTY : BookFluidTank.normalize(fluid);
        fluidStorage.amount = tag.getLongOr(AMOUNT_KEY, 0L);

        if (fluidStorage.fluid == Fluids.EMPTY)
            fluidStorage.amount = 0;
    }
}
