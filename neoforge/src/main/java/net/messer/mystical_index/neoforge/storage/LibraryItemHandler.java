package net.messer.mystical_index.neoforge.storage;

import net.messer.mystical_index.block.entity.LibraryBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.ArrayList;
import java.util.List;

/**
 * The NeoForge capability view of a library, mirroring what the Fabric module exposes through the
 * Transfer API: extraction and the slot listing come from the stored books' mirrors, while every
 * insertion is routed through the library's own two-pass fill rules instead of landing in whichever
 * slot the caller happened to name.
 *
 * <p>NeoForge replaced {@code IItemHandler} with {@link ResourceHandler}, which is transactional in
 * the same way Fabric's Transfer API always was: a caller opens a transaction, asks for the move,
 * and either commits it or drops it. Writes here land straight in the stored books' components, so
 * on its own an insert would ignore that, and a dropped transaction would leave the items written
 * while the source still had them - a dupe worth tens of items a second through a pipe. Extending
 * {@link SnapshotJournal} and calling {@link SnapshotJournal#updateSnapshots} before every mutation
 * is what makes both directions actually participate.
 *
 * <p>Because a never-committed transaction is by definition a complete dry run, the separate
 * simulate path the old {@code IItemHandler} needed is gone rather than kept alongside this one:
 * the snapshot restores whole book stacks and rebuilds the mirrors, so an aborted insert leaves
 * nothing behind to tell it apart from one that never happened.
 */
public class LibraryItemHandler extends SnapshotJournal<ItemStack[]>
        implements ResourceHandler<ItemResource>, LibraryBlockEntity.StorageView {

    private final LibraryBlockEntity library;

    // Flattened view of the stored books' mirrors: global slot i lives in containers[k] at
    // i - slotOffsets[k]. Rebuilt whenever the books change so a query never reconstructs it.
    private final List<Container> containers = new ArrayList<>();
    private final List<Integer> slotOffsets = new ArrayList<>();
    private int totalSlots;

    public LibraryItemHandler(LibraryBlockEntity library) {
        this.library = library;
    }

    @Override
    public void rebuild() {
        containers.clear();
        slotOffsets.clear();
        totalSlots = 0;

        for (var bookInventory : library.bookInventories) {
            containers.add(bookInventory.contents);
            slotOffsets.add(totalSlots);
            totalSlots += bookInventory.contents.getContainerSize();
        }
    }

    /**
     * A library whose books are all empty contributes no mirrors, and a handler reporting zero
     * slots is never even offered anything by a hopper. One insert-only slot is always advertised
     * so such a library still accepts items, which is what it does on Fabric, where insertion never
     * went through slots in the first place. The extra slot reads as permanently empty, so nothing
     * can be pulled out of it.
     */
    @Override
    public int size() {
        return Math.max(totalSlots, 1);
    }

    private int handlerIndex(int slot) {
        if (slot < 0 || slot >= totalSlots)
            return -1;

        for (int i = containers.size() - 1; i >= 0; i--) {
            if (slot >= slotOffsets.get(i))
                return i;
        }
        return -1;
    }

    private ItemStack stackAt(int slot) {
        int index = handlerIndex(slot);
        return index < 0 ? ItemStack.EMPTY : containers.get(index).getItem(slot - slotOffsets.get(index));
    }

    @Override
    public ItemResource getResource(int slot) {
        return ItemResource.of(stackAt(slot));
    }

    @Override
    public long getAmountAsLong(int slot) {
        return stackAt(slot).getCount();
    }

    @Override
    public long getCapacityAsLong(int slot, ItemResource resource) {
        int index = handlerIndex(slot);
        // The always-advertised insert-only slot has no container behind it; it exists to be
        // offered items, so it reports the resource's own stack limit rather than nothing.
        int containerLimit = index < 0 ? Item.ABSOLUTE_MAX_STACK_SIZE : containers.get(index).getMaxStackSize();
        if (resource.isEmpty())
            return containerLimit;

        return Math.min(containerLimit, resource.toStack().getMaxStackSize());
    }

    /**
     * Whether a book will take this is decided by the fill rules, not by the slot, so this can only
     * answer "ask insert".
     */
    @Override
    public boolean isValid(int slot, ItemResource resource) {
        return true;
    }

    /**
     * Inserts through the library rather than into {@code slot}: books already bound to this item
     * fill first, then an unbound Book of Holding claims the rest. The slot index is ignored for
     * the same reason the Fabric side overrides insert on the combined storage.
     */
    @Override
    public int insert(int slot, ItemResource resource, int amount, TransactionContext transaction) {
        return insert(resource, amount, transaction);
    }

    @Override
    public int insert(ItemResource resource, int amount, TransactionContext transaction) {
        if (resource.isEmpty() || amount <= 0)
            return 0;

        // Snapshot before the write, not after: an aborted or simulated transaction has to be able
        // to put every book back exactly as it was.
        updateSnapshots(transaction);

        // insertIntoLibrary works by draining the stack it is handed, so what it removed from this
        // one is the amount actually taken.
        return (int) library.insert(resource.toStack(amount));
    }

    @Override
    public int extract(int slot, ItemResource resource, int amount, TransactionContext transaction) {
        if (resource.isEmpty() || amount <= 0)
            return 0;

        int index = handlerIndex(slot);
        if (index < 0)
            return 0;

        var container = containers.get(index);
        int local = slot - slotOffsets.get(index);
        var present = container.getItem(local);
        if (present.isEmpty() || !resource.matches(present))
            return 0;

        updateSnapshots(transaction);

        var removed = container.removeItem(local, Math.min(amount, present.getCount()));
        if (removed.isEmpty())
            return 0;

        container.setChanged();
        return removed.getCount();
    }

    @Override
    protected ItemStack[] createSnapshot() {
        return library.snapshotBooks();
    }

    @Override
    protected void revertToSnapshot(ItemStack[] snapshot) {
        library.restoreBooks(snapshot);
    }

    @Override
    protected void onRootCommit(ItemStack[] snapshot) {
        // Only a committed change needs saving; an aborted one has already been put back.
        library.setChanged();
    }
}
