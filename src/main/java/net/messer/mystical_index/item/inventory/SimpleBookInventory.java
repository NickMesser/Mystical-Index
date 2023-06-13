package net.messer.mystical_index.item.inventory;

import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.messer.config.ModConfig;
import net.messer.mystical_index.item.custom.BaseStorageBook;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.collection.DefaultedList;

public class SimpleBookInventory {
    ItemStack bookStack;

    public SimpleBookInventory(ItemStack stack){
        bookStack = stack;
        book.addStack(bookStack);
    }

    public void clearInventory(SimpleInventory inventory){
        for (int i = 0; i < inventory.size(); i++) {
            inventory.setStack(i, ItemStack.EMPTY);
        }
    }

    public SimpleInventory book = new SimpleInventory(1){
        @Override
        public void markDirty() {
            if(bookStack.getItem() instanceof BaseStorageBook storageBook){
                var content = storageBook.getInventory(bookStack);
                clearInventory(contents);
                for(int i = 0; i < content.size(); i++){
                    contents.setStack(i, content.getStack(i));
                }
                contents.markDirty();
            }
        }
    };

    public SimpleInventory contents = new SimpleInventory(ModConfig.StorageBookMaxStacks * 5){
        @Override
        public void markDirty() {
            if(bookStack.getItem() instanceof BaseStorageBook storageBook){
                var content = storageBook.getInventory(bookStack);
                content.clear();
                for (int i = 0; i < contents.size(); i++) {
                    if (i >= content.size())
                        break;
                    var stack = contents.getStack(i);
                    if(stack.isEmpty() || stack.getItem() == Items.AIR)
                        continue;
                    content.setStack(i, contents.getStack(i));
                }
                content.markDirty();
            }
        }

        @Override
        public void setStack(int slot, ItemStack stack) {
            this.stacks.set(slot, stack);
            if (!stack.isEmpty() && stack.getCount() > this.getMaxCountPerStack()) {
                stack.setCount(this.getMaxCountPerStack());
            }
        }

        @Override
        public void clear() {
            this.stacks.clear();
        }
    };

    public final InventoryStorage bookWrapper = InventoryStorage.of(book, null);
    public final InventoryStorage contentsWrapper = InventoryStorage.of(contents, null);

}
