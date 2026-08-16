package net.messer.mystical_index.neoforge.storage;

import net.messer.mystical_index.block.entity.LibraryBlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

/**
 * The NeoForge capability view of a library, mirroring what the Fabric module exposes through the
 * Transfer API: extraction and the slot listing come from the stored books' mirrors, while every
 * insertion is routed through the library's own two-pass fill rules instead of landing in whichever
 * slot the caller happened to name.
 */
public class LibraryItemHandler implements IItemHandler, LibraryBlockEntity.StorageView {

    private final LibraryBlockEntity library;
    private IItemHandlerModifiable delegate = new CombinedInvWrapper();

    public LibraryItemHandler(LibraryBlockEntity library) {
        this.library = library;
    }

    @Override
    public void rebuild() {
        var wrappers = new IItemHandlerModifiable[library.bookInventories.size()];
        for (int i = 0; i < wrappers.length; i++)
            wrappers[i] = new InvWrapper(library.bookInventories.get(i).contents);

        delegate = new CombinedInvWrapper(wrappers);
    }

    /**
     * A library whose books are all empty contributes no mirrors, and a handler reporting zero
     * slots is never even offered anything by a hopper. One insert-only slot is always advertised
     * so such a library still accepts items, which is what it does on Fabric, where insertion never
     * went through slots in the first place. The extra slot reads as permanently empty, so nothing
     * can be pulled out of it.
     */
    @Override
    public int getSlots() {
        return Math.max(delegate.getSlots(), 1);
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return slot < delegate.getSlots() ? delegate.getStackInSlot(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return slot < delegate.getSlots() ? delegate.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return slot < delegate.getSlots() ? delegate.getSlotLimit(slot) : Item.DEFAULT_MAX_COUNT;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        // Whether a book will take this is decided by the fill rules, not by the slot, so this can
        // only answer "ask insertItem".
        return true;
    }

    /**
     * Inserts through the library rather than into {@code slot}: books already bound to this item
     * fill first, then an unbound Book of Holding claims the rest. The slot index is ignored for
     * the same reason the Fabric side overrides insert on the combined storage.
     *
     * <p>A simulated insert must leave no trace, so it goes to the library's dry run, which replays
     * the real fill against throwaway copies of the books instead of writing and rolling back.
     */
    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty())
            return ItemStack.EMPTY;

        if (simulate) {
            long inserted = library.simulateInsert(stack);
            if (inserted <= 0)
                return stack;
            if (inserted >= stack.getCount())
                return ItemStack.EMPTY;

            return stack.copyWithCount(stack.getCount() - (int) inserted);
        }

        // insertItem must not mutate the caller's stack, and the library's insert works by draining
        // the stack it is handed, so it drains a copy and that copy is the remainder.
        var remainder = stack.copy();
        if (library.insert(remainder) <= 0)
            return stack;

        return remainder.isEmpty() ? ItemStack.EMPTY : remainder;
    }
}
