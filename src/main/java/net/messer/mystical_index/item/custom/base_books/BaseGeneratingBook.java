package net.messer.mystical_index.item.custom.base_books;

import net.messer.config.ModConfig;
import net.messer.mystical_index.item.inventory.SingleItemStackingInventory;
import net.minecraft.client.item.BundleTooltipData;
import net.minecraft.client.item.TooltipData;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

import java.util.Optional;

public class BaseGeneratingBook extends BaseStorageBook{
    public BaseGeneratingBook(Settings settings) {
        super(settings);
    }

    public void updateUseTime(ItemStack stack, long time){
        NbtCompound compound = stack.getNbt();

        if(compound == null)
            compound = new NbtCompound();

        compound.putLong("lastUsedTime", time);
    }
}
