package net.messer.mystical_index.block.entity;

import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.impl.transfer.item.ItemVariantImpl;
import net.messer.mystical_index.item.custom.base_books.BaseStorageBook;
import net.messer.mystical_index.item.inventory.LibraryCombinedStorage;
import net.messer.mystical_index.item.inventory.LibraryNetwork;
import net.messer.mystical_index.item.inventory.SimpleBookInventory;
import net.messer.mystical_index.screen.LibraryInventoryScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
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
            for (var itemStack : storedBooks.heldStacks) {
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
                        // Books already bound to this item fill first, then an unbound Book of
                        // Holding claims the rest. The return value is the real amount taken;
                        // reporting the full requested amount made the caller void the remainder.
                        return LibraryNetwork.insertIntoLibrary(LibraryBlockEntity.this, stack);
                    }
                };
                LibraryBlockEntity.this.markDirty();
            }
        }
    };
    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        storedBooks.clear();
        Inventories.readNbt(nbt, storedBooks.heldStacks, registryLookup);
        storedBooks.markDirty();
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        Inventories.writeNbt(nbt, storedBooks.heldStacks, registryLookup);
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

    public static void tick(World world, BlockPos pos, BlockState state, LibraryBlockEntity be) {
        var storedBooks = be.storedBooks;
        for (var book : storedBooks.heldStacks) {
            if (book.getItem() instanceof BaseStorageBook storageBook) {
                storageBook.customBookTick(book, world, be);
            }
        }
        storedBooks.markDirty();
        return;
    }
}
