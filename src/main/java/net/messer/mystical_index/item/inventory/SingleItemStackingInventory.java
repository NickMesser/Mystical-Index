package net.messer.mystical_index.item.inventory;

import net.messer.config.ModConfig;
import net.messer.mystical_index.item.ModItems;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;

public class SingleItemStackingInventory implements Inventory {
    public final ItemStack stack;
    public final int inventorySize;
    public final DefaultedList<ItemStack> items;
    public Item currentlyStoredItem;

    public SingleItemStackingInventory(ItemStack stack , int size){
        this.stack = stack;
        this.inventorySize = size;
        this.items = DefaultedList.ofSize(size,ItemStack.EMPTY);
        this.currentlyStoredItem = Items.AIR;
        if(stack.hasNbt()){
            readNbt(stack);
        }
    }

    public void setCurrentlyStoredItem(Item item){
        this.currentlyStoredItem = item;
        this.markDirty();
    }

    public void writeNbt(){
        NbtCompound nbtData = new NbtCompound();
        nbtData.putString("storedItem", this.currentlyStoredItem.toString());
        Inventories.writeNbt(nbtData, items);
        stack.setNbt(nbtData);
    }

    public void readNbt(ItemStack stack){
        Inventories.readNbt(stack.getOrCreateNbt(), items);
        var itemName = stack.getNbt().get("storedItem").asString();
        currentlyStoredItem = Registries.ITEM.get(Identifier.tryParse(itemName));
        if(!itemName.isBlank()){
            this.stack.setCustomName(Text.literal("Book of " + currentlyStoredItem.getName().getString()));
        }
    }


    @Override
    public int getMaxCountPerStack() {

        if(this.stack.getItem() == ModItems.STORAGE_BOOK)
            return ModConfig.StorageBookMaxStacks * 64;

        if(this.stack.getItem() == ModItems.SATURATION_BOOK)
            return ModConfig.StorageBookMaxStacks * 64;

        return 64;
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
        this.markDirty();
        return stack;
    }

    public ItemStack decrementStack(int amount){
        getStack(0).decrement(amount);
        this.markDirty();
        return getStack(0);
    }

    @Override
    public ItemStack removeStack(int slot) {
        return Inventories.removeStack(items, slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        this.items.set(slot, stack);
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
        items.clear();
    }

    public ItemStack addStack(ItemStack stackToAdd) {
        var currentStack = this.getStack(0);
        if (ItemStack.canCombine(currentStack, stackToAdd)) {
            if ((currentStack.getCount() + stackToAdd.getCount()) > this.getMaxCountPerStack()) {
                var amountToMax = getMaxCountPerStack() - currentStack.getCount();
                currentStack.increment(amountToMax);
                stackToAdd.decrement(amountToMax);
                this.markDirty();
                return stackToAdd;
            } else {
                currentStack.increment(stackToAdd.getCount());
                stackToAdd.setCount(0);
                this.markDirty();
                return ItemStack.EMPTY;
            }
        } else {
            var newStack = stackToAdd.copy();
            if(newStack.getCount() > getMaxCountPerStack()){
                setStack(0, newStack);
                newStack.setCount(getMaxCountPerStack());
                stackToAdd.decrement(getMaxCountPerStack());
                this.markDirty();
                return stackToAdd;
            } else {
                setStack(0, newStack);
                stackToAdd.setCount(0);
            }
        }
        this.markDirty();
        return stackToAdd;
    }

    public ItemStack firstAvailableStack(){
        for (ItemStack stack : items){
            if(stack.isEmpty()) continue;
            return stack;
        }
        return ItemStack.EMPTY;
    }

    public int firstSlotWithStack(){
        for (int i = 0; i < items.size(); i++){
            if(items.get(i).isEmpty()) continue;
            return i;
        }
        return -1;
    }
}
