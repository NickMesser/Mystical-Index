package net.messer.firstmod.item.custom;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

public class ItemInventory implements Inventory {
    public final ItemStack stack;
    public final int inventorySize;
    public final DefaultedList<ItemStack> items;

    public ItemInventory(ItemStack stack , int size){
        this.stack = stack;
        this.inventorySize = size;
        this.items = DefaultedList.ofSize(size,ItemStack.EMPTY);
        if(stack.hasNbt()){
            Inventories.readNbt(stack.getOrCreateNbt(), items);
        }
    }

    @Override
    public int size() {
        return inventorySize;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items){
            if(stack.isEmpty()) continue;
            return false;
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack stack = Inventories.splitStack(items, slot, amount);
        if (!stack.isEmpty()) this.markDirty();
        return stack;
    }

    @Override
    public ItemStack removeStack(int slot) {
        return Inventories.removeStack(items, slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > this.getMaxCountPerStack()) {
            stack.setCount(this.getMaxCountPerStack());
        }

        this.markDirty();
    }

    @Override
    public void markDirty() {
        Inventories.writeNbt(stack.getOrCreateNbt(),items);
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void clear() {
        items.clear();
    }

    public ItemStack addStack(ItemStack stackToAdd) {
        ItemStack itemStack = stackToAdd.copy();
        this.addToExistingSlot(itemStack);
        if (itemStack.isEmpty()) {
            return ItemStack.EMPTY;
        } else {
            this.addToNewSlot(itemStack);
            return itemStack.isEmpty() ? ItemStack.EMPTY : itemStack;
        }
    }

    private void addToNewSlot(ItemStack stack) {
        for(int i = 0; i < this.inventorySize; ++i) {
            ItemStack itemStack = this.getStack(i);
            if (itemStack.isEmpty()) {
                this.setStack(i, stack.copy());
                stack.setCount(0);
                return;
            }
        }

    }

    private void addToExistingSlot(ItemStack stack) {
        for(int i = 0; i < this.inventorySize; ++i) {
            ItemStack itemStack = this.getStack(i);
            if (ItemStack.canCombine(itemStack, stack)) {
                if((itemStack.getCount() + stack.getCount()) > itemStack.getMaxCount())
                {
                    stack.decrement(itemStack.getMaxCount() - itemStack.getCount());
                    itemStack.setCount(itemStack.getMaxCount());
                    this.markDirty();
                    continue;
                }
                else{
                    itemStack.increment(stack.getCount());
                    stack.decrement(stack.getCount());
                    this.markDirty();
                }
                if (stack.isEmpty()) {
                    return;
                }
            }
        }

    }
}
