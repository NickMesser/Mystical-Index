package net.messer.mystical_index.util;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ContentsIndex {
    private final ArrayList<BigStack> contents;

    public ContentsIndex() {
        contents = new ArrayList<>();
    }

    public ContentsIndex(ArrayList<BigStack> contents) {
        this.contents = contents;
    }

    public void merge(ContentsIndex contentsIndex) {
        contentsIndex.contents.forEach(this::add);
    }

    private void add(BigStack bigStack) {
        add(bigStack.getItemStack(), bigStack.getAmount());
    }

    public void add(ItemStack itemStack) {
        add(itemStack, itemStack.getCount());
    }

    public void add(Item item, int amount) {
        add(item.getDefaultStack(), amount);
    }

    public void add(ItemStack item, int amount) {
        for (BigStack content : contents) {
            if (ItemStack.canCombine(content.getItemStack(), item)) {
                content.increment(amount);
                return;
            }
        }
        contents.add(new BigStack(item, amount));
    }

    public List<BigStack> getAll() {
        return contents;
    }
}
