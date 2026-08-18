package net.messer.mystical_index.item.inventory;

import net.messer.config.ModConfig;
import net.messer.mystical_index.item.custom.base_books.BaseStorageBook;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class SimpleBookInventory {
    ItemStack bookStack;

    // book.markDirty() writes into contents, and contents.markDirty() writes back into the book.
    // SimpleInventory.setStack() calls markDirty() on every slot write, so without this guard each
    // side re-entrantly triggers the other and the copy loops overwrite the book with a
    // half-cleared inventory. Only the outermost sync is allowed to run.
    private boolean syncing = false;

    public SimpleBookInventory(ItemStack stack){
        bookStack = stack;
        book.addItem(bookStack);
    }

    public void clearInventory(SimpleContainer inventory){
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            inventory.setItem(i, ItemStack.EMPTY);
        }
    }

    public SimpleContainer book = new SimpleContainer(1){
        @Override
        public void setChanged() {
            if(syncing)
                return;

            if(bookStack.getItem() instanceof BaseStorageBook storageBook){
                syncing = true;
                try {
                    var content = storageBook.getInventory(bookStack);
                    clearInventory(contents);
                    for(int i = 0; i < content.getContainerSize() && i < contents.getContainerSize(); i++){
                        contents.setItem(i, content.getItem(i));
                    }
                } finally {
                    syncing = false;
                }
            }
        }
    };

    public SimpleContainer contents = new SimpleContainer(ModConfig.StorageBookMaxStacks * 5){
        @Override
        public void setChanged() {
            if(syncing)
                return;

            if(bookStack.getItem() instanceof BaseStorageBook storageBook){
                syncing = true;
                try {
                    var content = storageBook.getInventory(bookStack);
                    // Only the mirrored range may be cleared. A book with more slots than this
                    // mirror (a high tier Book of Holding) would otherwise lose everything past
                    // the end of the mirror, since the restore loop below cannot reach it.
                    for (int i = 0; i < content.getContainerSize() && i < contents.getContainerSize(); i++) {
                        content.setItem(i, ItemStack.EMPTY);
                    }
                    for (int i = 0; i < contents.getContainerSize(); i++) {
                        if (i >= content.getContainerSize())
                            break;
                        var stack = contents.getItem(i);
                        if(stack.isEmpty() || stack.getItem() == Items.AIR)
                            continue;
                        content.setItem(i, contents.getItem(i));
                    }
                    content.setChanged();
                } finally {
                    syncing = false;
                }
            }
        }

        @Override
        public void clearContent() {
            this.getItems().clear();
        }
    };

}
