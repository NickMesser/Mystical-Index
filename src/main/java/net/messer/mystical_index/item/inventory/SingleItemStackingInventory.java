package net.messer.mystical_index.item.inventory;
import net.messer.config.ModConfig;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;

import java.util.Iterator;

public class SingleItemStackingInventory implements Inventory {
    public final ItemStack stack;
    public final int inventorySize;
    public final DefaultedList<ItemStack> storedItems;
    public Item currentlyStoredItem;

    public int maxStacks = 1;

    public SingleItemStackingInventory(ItemStack stack , int size){
        this.stack = stack;
        this.inventorySize = size;
        this.storedItems = DefaultedList.ofSize(size,ItemStack.EMPTY);
        this.currentlyStoredItem = Items.AIR;
        this.maxStacks = ModConfig.StorageBookMaxStacks;
        if(stack.hasNbt()){
            readNbt();
        }
    }

    public void setCurrentlyStoredItem(Item item){
        this.currentlyStoredItem = item;
        this.markDirty();
    }

    public boolean tryRemoveOneItem(){
        for (int i = 0; i < storedItems.size(); i++) {
            if(storedItems.get(i).getItem() != Items.AIR){
                storedItems.get(i).decrement(1);
                if(storedItems.get(i).getCount() == 0){
                    storedItems.set(i, ItemStack.EMPTY);
                }
                this.markDirty();
                return true;
            }
        }
        return false;
    }

    public int getCountOfStoredItem(){
        int count = 0;
        for (ItemStack item: storedItems) {
            if(item.getItem() != Items.AIR)
                count += item.getCount();
        }
        return count;
    }

    public ItemStack getFirstItemStack(){
        for (int i = 0; i < storedItems.size(); i++) {
            if(storedItems.get(i).getItem() != Items.AIR){
                return storedItems.get(i);
            }
        }
        return ItemStack.EMPTY;
    }


    public boolean tryAddStack(ItemStack stack, Boolean bypassItemCheck){
        if(stack.getItem() != currentlyStoredItem && !bypassItemCheck)
            return false;

        ItemStack stackToAdd = stack.copy();

        for (ItemStack item: storedItems) {
            if (ItemStack.canCombine(item, stack)) {
                int combinedCount = item.getCount() + stack.getCount();
                if (combinedCount > this.getMaxCountPerStack() && item.getCount() < this.getMaxCountPerStack()) {
                    var remainder = this.getMaxCountPerStack() - item.getCount();
                    item.increment(remainder);
                    stack.decrement(remainder);
                    this.markDirty();
                    if(stack.getCount() > 0)
                        return tryAddStack(stack, true);
                    else
                        return true;
                }

                if(combinedCount <= this.getMaxCountPerStack()){
                    item.increment(stack.getCount());
                    stack.setCount(0);
                    this.markDirty();
                    return true;
                }
            }
        }

        for (int i = 0; i < storedItems.size(); i++) {
            if(storedItems.get(i).getItem() == Items.AIR){
                storedItems.set(i, stackToAdd);
                stack.setCount(0);
                this.markDirty();
                return true;
            }
        }

        return false;
    }

    public void writeNbt(){
        NbtCompound nbtData = stack.getNbt();
        if(nbtData == null)
            nbtData = new NbtCompound();

        nbtData.putString("storedItem", this.currentlyStoredItem.toString());
        Inventories.writeNbt(nbtData, storedItems);
    }

    public void readNbt(){
        NbtCompound compound = stack.getNbt();
        if (compound == null) {
            return;
        }

        Inventories.readNbt(compound, storedItems);
        var itemName = compound.getString("storedItem");
        currentlyStoredItem = Registries.ITEM.get(Identifier.tryParse(itemName));
    }


    @Override
    public int getMaxCountPerStack() {
        return 64;
    }

    @Override
    public int size() {
        return inventorySize;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : storedItems){
            if(stack.getItem() != Items.AIR || !stack.isEmpty()){
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        return storedItems.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack stack = Inventories.splitStack(storedItems, slot, amount);
        this.markDirty();
        return stack;
    }

    @Override
    public ItemStack removeStack(int slot) {
        return Inventories.removeStack(storedItems, slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        this.storedItems.set(slot, stack);
        this.currentlyStoredItem = stack.getItem();
        if (!stack.isEmpty() && stack.getCount() > this.getMaxCountPerStack()) {
            stack.setCount(this.getMaxCountPerStack());
        }

        this.markDirty();
    }

    @Override
    public void markDirty() {
        writeNbt();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void clear() {
        storedItems.clear();
    }

}
