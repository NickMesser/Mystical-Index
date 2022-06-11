package net.messer.mystical_index.item.inventory;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

public interface LibraryInventory extends Inventory {

    DefaultedList<ItemStack> getItems();

    void updateBlockState(int books);

    @Override
    default int size() {
        return getItems().size();
    }

    default int slotsNotEmpty() {
        int i = 0;
        for (int j = 0; j < size(); ++j) {
            ItemStack itemStack = getStack(j);
            if (itemStack.equals(ItemStack.EMPTY)) continue;
            i += itemStack.getCount();
        }
        return i;
    }

    @Override
    default boolean isEmpty() {
        for (int i = 0; i < size(); i++) {
            ItemStack stack = getStack(i);
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    default ItemStack getStack(int slot) {
        return getItems().get(slot);
    }

    @Override
    default ItemStack removeStack(int slot, int amount) {
        ItemStack result = Inventories.splitStack(getItems(), slot, amount);
        if (!result.isEmpty()) {
            updateBlockState(slotsNotEmpty());
            markDirty();
        }
        return result;
    }

    @Override
    default ItemStack removeStack(int slot) {
        var result = Inventories.removeStack(getItems(), slot);
        updateBlockState(slotsNotEmpty());
        return result;
    }

    @Override
    default void setStack(int slot, ItemStack stack) {
        getItems().set(slot, stack);
        updateBlockState(slotsNotEmpty());
        if (stack.getCount() > getMaxCountPerStack()) {
            stack.setCount(getMaxCountPerStack());
        }
    }

    @Override
    default void markDirty() {
    }

    @Override
    default boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    @Override
    default void clear() {
        getItems().clear();
        updateBlockState(slotsNotEmpty());
    }
}
