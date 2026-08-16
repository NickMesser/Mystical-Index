package net.messer.mystical_index.item.inventory;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.messer.config.ModConfig;
import net.messer.mystical_index.MysticalIndex;
import net.messer.util.MysticalUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
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
        var ops = MysticalUtil.registryLookup().getOps(NbtOps.INSTANCE);
        FluidVariant.CODEC.encodeStart(ops, fluidStorage.variant)
                .result()
                .ifPresent(encoded -> tag.put("fluidVariant", encoded));
        tag.putLong("amount", fluidStorage.amount);
        MysticalUtil.setCustomData(stack, tag);
    }

    public void readNbt(ItemStack stack){
        NbtCompound tag = MysticalUtil.getCustomData(stack);
        if(tag == null)
            return;

        // Same "fluid" field the old variant NBT used, so books written before the component
        // rewrite still resolve; only the optional extra data moved key.
        var ops = MysticalUtil.registryLookup().getOps(NbtOps.INSTANCE);
        fluidStorage.variant = FluidVariant.CODEC.parse(ops, tag.getCompound("fluidVariant"))
                .result()
                .orElse(FluidVariant.blank());
        fluidStorage.amount = tag.getLong("amount");
        if(fluidStorage.variant.getFluid() != Fluids.EMPTY)
            stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Book of " +
                    Registries.FLUID.getId(fluidStorage.variant.getFluid()).getPath().substring(0,1).toUpperCase() +
                    Registries.FLUID.getId(fluidStorage.variant.getFluid()).getPath().substring(1)));

    }
}
