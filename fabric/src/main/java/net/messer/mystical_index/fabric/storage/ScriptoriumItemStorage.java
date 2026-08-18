package net.messer.mystical_index.fabric.storage;

import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.messer.mystical_index.block.entity.ScriptoriumBlockEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The Fabric Transfer API view of a Scriptorium.
 *
 * <p>Mirror of {@link LibraryItemStorage} - a deliberate parallel copy rather than a shared class.
 * The two block entities expose their mirrors as a public field and a nested interface, so sharing
 * would have meant editing the Library's proven transactional code or coupling this block to a type
 * named after another one. Keep the two in step: a fix here almost certainly belongs there too.
 *
 * <p>Two differences from the Library, both deliberate:
 * <ul>
 *   <li>insertion routes through {@code offerToBooks}, the same method ground absorption uses, so
 *       hopper and world insertion cannot drift apart;</li>
 *   <li>farming OUTPUTS are served alongside the book mirrors. A farming book's {@code getInventory}
 *       is its legacy store, so the mirrors never see the outputs. The Library deliberately keeps
 *       serving legacy contents only - the Scriptorium is the automation block, so here the produce
 *       comes out.</li>
 * </ul>
 */
public class ScriptoriumItemStorage implements ScriptoriumBlockEntity.StorageView {

    private final ScriptoriumBlockEntity scriptorium;

    // The parts list is handed to the CombinedStorage once and then mutated in place, so the
    // storage handed out to a neighbouring pipe stays the same object across book changes.
    private final List<Storage<ItemVariant>> parts = new ArrayList<>();
    private final Storage<ItemVariant> storage;

    // offerToBooks writes items straight into the stored books' custom_data, so on its own it
    // ignores the transaction: a simulated insert (StorageUtil opens a nested transaction and never
    // commits it) would leave the items written while the source kept them, duping through pipes.
    // Snapshotting every book before the write and restoring it on abort makes the insert
    // participate in the transaction.
    private final SnapshotParticipant<ItemStack[]> insertParticipant = new SnapshotParticipant<>() {
        @Override
        protected ItemStack[] createSnapshot() {
            return scriptorium.snapshotBooks();
        }

        @Override
        protected void readSnapshot(ItemStack[] snapshot) {
            scriptorium.restoreBooks(snapshot);
        }
    };

    public ScriptoriumItemStorage(ScriptoriumBlockEntity scriptorium) {
        this.scriptorium = scriptorium;
        this.storage = new ScriptoriumCombinedStorage(parts);
    }

    public Storage<ItemVariant> storage() {
        return storage;
    }

    @Override
    public void rebuild() {
        parts.clear();
        for (var bookInventory : scriptorium.bookInventories)
            parts.add(ContainerStorage.of(bookInventory.contents, null));

        for (var outputs : scriptorium.farmingOutputs)
            parts.add(ContainerStorage.of(outputs, null));
    }

    private class ScriptoriumCombinedStorage extends CombinedStorage<ItemVariant, Storage<ItemVariant>> {
        // Takes the list as a parameter rather than reading the outer field: an inner class cannot
        // touch its enclosing instance until after the super constructor has run.
        ScriptoriumCombinedStorage(List<Storage<ItemVariant>> parts) {
            super(parts);
        }

        @Override
        public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            if (resource.isBlank() || maxAmount <= 0)
                return 0;

            insertParticipant.updateSnapshots(transaction);

            // maxAmount is a long; clamp instead of casting so a huge request never truncates to a
            // zero-count stack. offerToBooks drains what it takes, so the delta is the real amount.
            var stack = resource.toStack((int) Math.min(maxAmount, Integer.MAX_VALUE));
            int before = stack.getCount();
            scriptorium.offerToBooks(stack);

            // offerToBooks writes inside the book stacks, which never notifies the container - so
            // the mirrors would not rebuild until the next tick and an extract in the same tick
            // could read pre-insert contents. Same reason LibraryNetwork needs markLibraryChanged.
            if (before != stack.getCount())
                scriptorium.storedBooks.setChanged();

            return before - stack.getCount();
        }
    }
}
