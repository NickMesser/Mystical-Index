package net.messer.mystical_index.item.inventory;

import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.messer.config.ModConfig;
import net.messer.mystical_index.item.custom.base_books.BaseStorageBook;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class SimpleBookInventory {
    ItemStack bookStack;

    // book.markDirty() writes into contents, and contents.markDirty() writes back into the book.
    // SimpleInventory.setStack() calls markDirty() on every slot write, so without this guard each
    // side re-entrantly triggers the other and the copy loops overwrite the book with a
    // half-cleared inventory. Only the outermost sync is allowed to run.
    private boolean syncing = false;

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
            if(syncing)
                return;

            if(bookStack.getItem() instanceof BaseStorageBook storageBook){
                syncing = true;
                try {
                    var content = storageBook.getInventory(bookStack);
                    clearInventory(contents);
                    for(int i = 0; i < content.size() && i < contents.size(); i++){
                        contents.setStack(i, content.getStack(i));
                    }
                } finally {
                    syncing = false;
                }
            }
        }
    };

    public SimpleInventory contents = new SimpleInventory(ModConfig.StorageBookMaxStacks * 5){
        @Override
        public void markDirty() {
            if(syncing)
                return;

            if(bookStack.getItem() instanceof BaseStorageBook storageBook){
                syncing = true;
                try {
                    var content = storageBook.getInventory(bookStack);
                    // Only the mirrored range may be cleared. A book with more slots than this
                    // mirror (a high tier Book of Holding) would otherwise lose everything past
                    // the end of the mirror, since the restore loop below cannot reach it.
                    for (int i = 0; i < content.size() && i < contents.size(); i++) {
                        content.setStack(i, ItemStack.EMPTY);
                    }
                    for (int i = 0; i < contents.size(); i++) {
                        if (i >= content.size())
                            break;
                        var stack = contents.getStack(i);
                        if(stack.isEmpty() || stack.getItem() == Items.AIR)
                            continue;
                        content.setStack(i, contents.getStack(i));
                    }
                    content.markDirty();
                } finally {
                    syncing = false;
                }
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
