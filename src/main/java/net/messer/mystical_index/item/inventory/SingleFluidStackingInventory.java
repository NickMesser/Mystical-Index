package net.messer.mystical_index.item.inventory;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.messer.config.ModConfig;
import net.messer.mystical_index.MysticalIndex;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

public class SingleFluidStackingInventory {
    public final ItemStack stack;

    public final SingleVariantStorage<FluidVariant> fluidStorage = new SingleVariantStorage<FluidVariant>() {
        @Override
        protected FluidVariant getBlankVariant() {
            return FluidVariant.blank();
        }

        @Override
        protected long getCapacity(FluidVariant variant) {
            return ModConfig.FluidBookMaxBuckets * FluidConstants.BUCKET;
        }

        @Override
        protected void onFinalCommit() {
            markDirty();
        }
    };

    public boolean hasFluid(){
        return fluidStorage.amount != 0;
    }

    public SingleFluidStackingInventory(ItemStack stack){
        this.stack = stack;
        if(stack.hasNbt())
            readNbt(stack);
    }

    public void markDirty(){
        writeNbt();
    }

    public void writeNbt(){
        // Merge into the existing compound: replacing it wholesale would drop the custom name
        // and everything else already on the stack.
        NbtCompound tag = stack.getOrCreateNbt();
        tag.put("fluidVariant", fluidStorage.variant.toNbt());
        tag.putLong("amount", fluidStorage.amount);
    }

    public void readNbt(ItemStack stack){
        NbtCompound tag = stack.getNbt();
        if(tag == null)
            return;

        // Read only. The custom name is updated from FluidBook's fill/empty paths instead of
        // here: setting it during a read ran every render frame, reverted anvil renames and
        // never cleared when the book was drained.
        fluidStorage.variant = FluidVariant.fromNbt(tag.getCompound("fluidVariant"));
        fluidStorage.amount = tag.getLong("amount");
    }
}
