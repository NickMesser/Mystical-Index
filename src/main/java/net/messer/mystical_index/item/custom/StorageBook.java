package net.messer.mystical_index.item.custom;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;


public class StorageBook extends InventoryBookItem {

    public StorageBook(Settings settings) {
        super(settings);
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        return Items.BOOK;
    }

    @Override
    public int getMaxTypes() {
        return 32;
    }

    @Override
    public int getMaxStack() {
        return 8;
    }
}
