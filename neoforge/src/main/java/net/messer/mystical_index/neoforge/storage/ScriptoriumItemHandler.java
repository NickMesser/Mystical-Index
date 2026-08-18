package net.messer.mystical_index.neoforge.storage;

import net.messer.mystical_index.block.entity.ScriptoriumBlockEntity;
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
 * The NeoForge capability view of a Scriptorium.
 *
 * <p>Mirror of {@link LibraryItemHandler} - a deliberate parallel copy rather than a shared class,
 * for the same reason the Fabric pair are parallel: the two block entities expose their mirrors as
 * a public field and a nested interface, and sharing would have meant editing the Library's proven
 * transactional code. Keep the two in step: a fix here almost certainly belongs there too.
 *
 * <p>Two differences from the Library, both deliberate: insertion routes through
 * {@code offerToBooks} so hopper and ground absorption share one rule, and farming OUTPUTS are
 * served alongside the book mirrors. A farming book's {@code getInventory} is its legacy store, so
 * the mirrors never see the outputs; the Library deliberately keeps serving legacy contents only,
 * while the Scriptorium is the automation block and lets the produce out.
 */
public class ScriptoriumItemHandler extends SnapshotJournal<ItemStack[]>
        implements ResourceHandler<ItemResource>, ScriptoriumBlockEntity.StorageView {

    private final ScriptoriumBlockEntity scriptorium;

    // Flattened view: global slot i lives in containers[k] at i - slotOffsets[k]. Rebuilt whenever
    // the books change so a query never reconstructs it.
    private final List<Container> containers = new ArrayList<>();
    private final List<Integer> slotOffsets = new ArrayList<>();
    private int totalSlots;

    public ScriptoriumItemHandler(ScriptoriumBlockEntity scriptorium) {
        this.scriptorium = scriptorium;
    }

    @Override
    public void rebuild() {
        containers.clear();
        slotOffsets.clear();
        totalSlots = 0;

        for (var bookInventory : scriptorium.bookInventories)
            addContainer(bookInventory.contents);

        for (var outputs : scriptorium.farmingOutputs)
            addContainer(outputs);
    }

    private void addContainer(Container container) {
        containers.add(container);
        slotOffsets.add(totalSlots);
        totalSlots += container.getContainerSize();
    }

    /**
     * A Scriptorium whose books are all empty contributes no mirrors, and a handler reporting zero
     * slots is never even offered anything by a hopper. One insert-only slot is always advertised
     * so such a block still accepts items, matching what it does on Fabric where insertion never
     * went through slots at all. The extra slot reads as permanently empty, so nothing can be
     * pulled out of it.
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
        int limit = index < 0 ? Item.ABSOLUTE_MAX_STACK_SIZE : containers.get(index).getMaxStackSize();
        if (resource.isEmpty())
            return limit;

        return Math.min(limit, resource.toStack().getMaxStackSize());
    }

    /** Which book will take a stack is decided by the fill rules, not by the slot. */
    @Override
    public boolean isValid(int slot, ItemResource resource) {
        return true;
    }

    @Override
    public int insert(int slot, ItemResource resource, int amount, TransactionContext transaction) {
        return insert(resource, amount, transaction);
    }

    @Override
    public int insert(ItemResource resource, int amount, TransactionContext transaction) {
        if (resource.isEmpty() || amount <= 0)
            return 0;

        // Snapshot before the write: an aborted or simulated transaction has to be able to put
        // every book back exactly as it was.
        updateSnapshots(transaction);

        var stack = resource.toStack(amount);
        int before = stack.getCount();
        scriptorium.offerToBooks(stack);

        // offerToBooks writes inside the book stacks, which never notifies the container - so the
        // mirrors would not rebuild until the next tick and an extract in the same tick could read
        // pre-insert contents. Same reason LibraryNetwork needs markLibraryChanged.
        if (before != stack.getCount())
            scriptorium.storedBooks.setChanged();

        return before - stack.getCount();
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
        return scriptorium.snapshotBooks();
    }

    @Override
    protected void revertToSnapshot(ItemStack[] snapshot) {
        scriptorium.restoreBooks(snapshot);
    }

    @Override
    protected void onRootCommit(ItemStack[] snapshot) {
        scriptorium.setChanged();
    }
}
