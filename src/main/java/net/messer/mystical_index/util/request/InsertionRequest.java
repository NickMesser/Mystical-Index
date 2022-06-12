package net.messer.mystical_index.util.request;

import net.minecraft.item.ItemStack;

public class InsertionRequest extends Request {
    private final ItemStack itemStack;
    // TODO priority?

    public InsertionRequest(ItemStack itemStack) {
        super(itemStack.getCount());
        this.itemStack = itemStack;
    }

    @Override
    public void apply(LibraryIndex index, boolean apply) {
        index.insertStack(this); // TODO apply
    }

    @Override
    public void satisfy(int amount) {
        itemStack.decrement(amount);
        super.satisfy(amount);
    }

    public ItemStack getItemStack() {
        return itemStack;
    }
}
