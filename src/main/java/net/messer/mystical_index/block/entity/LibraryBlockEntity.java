package net.messer.mystical_index.block.entity;

import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
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
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;


public class LibraryBlockEntity extends BlockEntity implements NamedScreenHandlerFactory {

    List<Storage<ItemVariant>> combinedContents = new ArrayList<>();
    Storage<ItemVariant> combinedStorage = new LibraryCombinedStorage(combinedContents);

    // insertIntoLibrary writes items straight into the stored books' NBT, so on its own it ignores
    // the transaction: a simulated insert (StorageUtil opens a nested transaction and never commits
    // it) would leave the items written while the source kept them, duping ~20/sec through pipes.
    // Snapshotting every book's NBT before the write and restoring it on abort makes the insert
    // participate in the transaction.
    private final SnapshotParticipant<NbtCompound[]> insertParticipant = new SnapshotParticipant<>() {
        @Override
        protected NbtCompound[] createSnapshot() {
            var stacks = storedBooks.stacks;
            var snapshot = new NbtCompound[stacks.size()];
            for (int i = 0; i < stacks.size(); i++) {
                var nbt = stacks.get(i).getNbt();
                snapshot[i] = nbt == null ? null : nbt.copy();
            }
            return snapshot;
        }

        @Override
        protected void readSnapshot(NbtCompound[] snapshot) {
            var stacks = storedBooks.stacks;
            for (int i = 0; i < snapshot.length && i < stacks.size(); i++) {
                stacks.get(i).setNbt(snapshot[i] == null ? null : snapshot[i].copy());
            }
        }
    };

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
                    public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
                        if (resource.isBlank() || maxAmount <= 0)
                            return 0;

                        // Snapshot the books before mutating so an aborted or simulated insert
                        // rolls them back. maxAmount is a long; clamp instead of casting so a
                        // huge request never truncates to a zero-count stack.
                        insertParticipant.updateSnapshots(transaction);
                        var stack = resource.toStack((int) Math.min(maxAmount, Integer.MAX_VALUE));
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
        return new LibraryInventoryScreenHandler(syncId, inv, storedBooks,
                ScreenHandlerContext.create(getWorld(), getPos()));
    }

    public static void tick(World world, BlockPos pos, BlockState state, LibraryBlockEntity be) {
        var storedBooks = be.storedBooks;
        for (var book : storedBooks.stacks) {
            if (book.getItem() instanceof BaseStorageBook storageBook) {
                storageBook.customBookTick(book, world, be);
            }
        }
        storedBooks.markDirty();
        return;
    }
}
