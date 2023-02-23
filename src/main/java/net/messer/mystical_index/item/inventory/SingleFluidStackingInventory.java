package net.messer.mystical_index.item.inventory;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.messer.config.ModConfig;
import net.messer.mystical_index.MysticalIndex;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

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

    public boolean IsFluidEmpty(){
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
        NbtCompound tag = new NbtCompound();
        tag.put("fluidVariant", fluidStorage.variant.toNbt());
        tag.putLong("amount", fluidStorage.amount);
        stack.setNbt(tag);
    }

    public void readNbt(ItemStack stack){
        fluidStorage.variant = FluidVariant.fromNbt(stack.getNbt().getCompound("fluidVariant"));
        fluidStorage.amount = stack.getNbt().getLong("amount");
        if(fluidStorage.variant.getFluid() != Fluids.EMPTY)
            stack.setCustomName(Text.literal("Book of " +
                    Registries.FLUID.getId(fluidStorage.variant.getFluid()).getPath().substring(0,1).toUpperCase() +
                    Registries.FLUID.getId(fluidStorage.variant.getFluid()).getPath().substring(1)));

    }
}
