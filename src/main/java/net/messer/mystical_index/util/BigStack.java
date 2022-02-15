package net.messer.mystical_index.util;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class BigStack {
    private final ItemStack itemStack;
    private int amount;

    public BigStack(ItemStack itemStack) {
        this(itemStack, itemStack.getCount());
    }

    public BigStack(ItemStack itemStack, int amount) {
        itemStack.setCount(1);
        this.itemStack = itemStack;
        this.amount = amount;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public int getAmount() {
        return amount;
    }

    public Item getItem() {
        return itemStack.getItem();
    }

    public void increment(int amount) {
        this.amount += amount;
    }
}
