package net.messer.mystical_index.block.entity;

import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.impl.transfer.item.ItemVariantImpl;
import net.messer.mystical_index.item.custom.base_books.BaseStorageBook;
import net.messer.mystical_index.item.inventory.LibraryCombinedStorage;
import net.messer.mystical_index.item.inventory.SimpleBookInventory;
import net.messer.mystical_index.screen.LibraryInventoryScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;


public class LibraryBlockEntity extends BlockEntity implements NamedScreenHandlerFactory {

    List<InventoryStorage> combinedContents = new ArrayList<>();
    Storage<ItemVariant> combinedStorage = new LibraryCombinedStorage(combinedContents);

    public SimpleInventory storedBooks = new SimpleInventory(5) {
        @Override
        public void markDirty() {
            combinedContents.clear();
            for (var itemStack : storedBooks.stacks) {
                if(itemStack.getItem() instanceof BaseStorageBook storageBook){
                    if(storageBook.getInventory(itemStack).isEmpty())
                        continue;
                }
                SimpleBookInventory bookInventory = new SimpleBookInventory(itemStack);
                combinedContents.add(InventoryStorage.of(bookInventory.contents, null));
                combinedStorage = new LibraryCombinedStorage(combinedContents){
                    @Override
                    public long insert(Object resource, long maxAmount, TransactionContext transaction) {
                        var stack = ((ItemVariantImpl) resource).toStack((int)maxAmount);
                        var stackCopy = stack.copy();
                        for(var book: storedBooks.stacks){
                            if(book.getItem() instanceof BaseStorageBook storageBook){
                                var storage = storageBook.getInventory(book);
                                if(storage.tryAddStack(stack, false)){
                                    return stackCopy.getCount();
                                }
                            }
                        }
                        return 0;
                    }
                };
                LibraryBlockEntity.this.markDirty();
            }
        }
    };
    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        storedBooks.clear();
        Inventories.readNbt(nbt, storedBooks.stacks);
        storedBooks.markDirty();
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        Inventories.writeNbt(nbt, storedBooks.stacks);
    }

    public LibraryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LIBRARY_BLOCK_ENTITY,pos, state);
    }

    @Override
    public Text getDisplayName() {
        return Text.literal("Library");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new LibraryInventoryScreenHandler(syncId,inv, storedBooks);
    }
}
