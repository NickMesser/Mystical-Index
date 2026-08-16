package net.messer.mystical_index.item.custom.base_books;


import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;


public class BaseGeneratingBook extends BaseStorageBook{
    public BaseGeneratingBook(Settings settings) {
        super(settings);
    }

    public void updateUseTime(ItemStack stack, long time){
        // getOrCreateNbt() attaches the compound to the stack; a bare `new NbtCompound()` would
        // be discarded and the write silently lost.
        NbtCompound compound = stack.getOrCreateNbt();

        compound.putLong("lastUsedTime", time);
    }
}
