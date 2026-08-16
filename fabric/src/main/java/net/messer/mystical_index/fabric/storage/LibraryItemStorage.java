package net.messer.mystical_index.fabric.storage;

import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.messer.mystical_index.block.entity.LibraryBlockEntity;
import net.messer.mystical_index.item.inventory.LibraryNetwork;
import net.minecraft.nbt.NbtCompound;

import java.util.ArrayList;
import java.util.List;

/**
 * The Fabric Transfer API view of a library: one {@link InventoryStorage} per stored book's mirror,
 * combined, with insertion routed through the library's own two-pass fill rules.
 */
public class LibraryItemStorage implements LibraryBlockEntity.StorageView {

    private final LibraryBlockEntity library;

    // The parts list is handed to the CombinedStorage once and then mutated in place, so the
    // storage handed out to a neighbouring pipe stays the same object across book changes.
    private final List<Storage<ItemVariant>> parts = new ArrayList<>();
    private final Storage<ItemVariant> storage;

    // insertIntoLibrary writes items straight into the stored books' custom_data, so on its own it
    // ignores the transaction: a simulated insert (StorageUtil opens a nested transaction and never
    // commits it) would leave the items written while the source kept them, duping ~20/sec through
    // pipes. Snapshotting every book's component before the write and restoring it on abort makes
    // the insert participate in the transaction.
    private final SnapshotParticipant<NbtCompound[]> insertParticipant = new SnapshotParticipant<>() {
        @Override
        protected NbtCompound[] createSnapshot() {
            return library.snapshotBooks();
        }

        @Override
        protected void readSnapshot(NbtCompound[] snapshot) {
            library.restoreBooks(snapshot);
        }
    };

    public LibraryItemStorage(LibraryBlockEntity library) {
        this.library = library;
        this.storage = new LibraryCombinedStorage(parts);
    }

    public Storage<ItemVariant> storage() {
        return storage;
    }

    @Override
    public void rebuild() {
        parts.clear();
        for (var bookInventory : library.bookInventories)
            parts.add(InventoryStorage.of(bookInventory.contents, null));
    }

    // Properly typed so the insert override reads ItemVariant/long directly instead of casting to a
    // Fabric impl class and truncating the amount. Extraction is left to the combined parts.
    private class LibraryCombinedStorage extends CombinedStorage<ItemVariant, Storage<ItemVariant>> {
        // Takes the list as a parameter rather than reading the outer field: an inner class cannot
        // touch its enclosing instance until after the super constructor has run.
        LibraryCombinedStorage(List<Storage<ItemVariant>> parts) {
            super(parts);
        }

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
            return LibraryNetwork.insertIntoLibrary(library, stack);
        }
    }
}
