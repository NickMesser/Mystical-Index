package net.messer.mystical_index.item.inventory;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

public interface ILibraryInventory extends Inventory {

    DefaultedList<ItemStack> getItems();

    static ILibraryInventory of(DefaultedList<ItemStack> items) {
        return () -> items;
    }

    static ILibraryInventory ofSize(int size) {
        return of(DefaultedList.ofSize(size, ItemStack.EMPTY));
    }

    @Override
    public default int size() {
        return getItems().size();
    }

    @Override
    public default boolean isEmpty() {
        for (int i = 0; i < size(); i++) {
            ItemStack stack = getStack(i);
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public default ItemStack getStack(int slot) {
        return getItems().get(slot);
    }

    @Override
    public default ItemStack removeStack(int slot, int amount) {
        ItemStack result = Inventories.splitStack(getItems(), slot, amount);
        if (!result.isEmpty()) {
            markDirty();
        }
        return result;
    }

    @Override
    public default ItemStack removeStack(int slot) {
        return Inventories.removeStack(getItems(), slot);
    }

    @Override
    public default void setStack(int slot, ItemStack stack) {
        getItems().set(slot, stack);
        if (stack.getCount() > getMaxCountPerStack()) {
            stack.setCount(getMaxCountPerStack());
        }
    }

    @Override
    public default void markDirty() {
    }

    @Override
    public default boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    @Override
    public default void clear() {
        getItems().clear();
    }
}
