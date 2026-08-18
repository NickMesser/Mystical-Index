package net.messer.mystical_index.screen;

import net.messer.mystical_index.item.custom.MagnetismBook;
import net.messer.mystical_index.item.inventory.MagnetFilterData;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Book of Magnetism's filter screen: nine ghost slots and a mode button.
 *
 * <p>The book is resolved from whichever hand holds it, main hand first, identically on both sides
 * - the same reasoning as the farming book, where carrying the slot index in menu data would go
 * stale if the inventory shifted under an open screen.
 *
 * <p><b>Ghost slot click matrix.</b> Nothing here ever moves a real item; the slots display a
 * filter, not contents.
 * <ul>
 *   <li>cursor holding an item + click → that slot is set to the cursor item, cursor untouched</li>
 *   <li>cursor holding the item already in that slot + click → slot cleared (click again to undo)</li>
 *   <li>empty cursor + click → slot cleared</li>
 *   <li>shift-click a player inventory stack → sets the first empty ghost slot, stack unmoved</li>
 *   <li>shift-click a ghost slot → clears it</li>
 *   <li>quick-move can never place a real item in the book: every path here either sets an id or
 *       clears one, and {@link #quickMoveStack} refuses to transfer into the ghost range</li>
 * </ul>
 */
public class MagnetismScreenHandler extends AbstractContainerMenu {

    public static final int MODE_BUTTON_ID = 0;

    private final Player player;
    private final Container ghosts;

    public MagnetismScreenHandler(int syncId, Inventory playerInventory) {
        super(ModScreenHandlers.MAGNETISM_SCREEN_HANDLER.get(), syncId);
        this.player = playerInventory.player;
        this.ghosts = new GhostContainer(player);

        // One row of five, centred: five 18px squares span 90px in a 176px panel, so the row
        // starts at 43 and a 3x3 block can no longer be mistaken for a crafting grid.
        for (int col = 0; col < MagnetFilterData.FILTER_SLOTS; col++) {
            addSlot(new Slot(ghosts, col, 44 + col * 18, 20) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }

                @Override
                public boolean mayPickup(Player player) {
                    return false;
                }
            });
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    public static ItemStack heldBook(Player player) {
        var main = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (main.getItem() instanceof MagnetismBook)
            return main;

        var off = player.getItemInHand(InteractionHand.OFF_HAND);
        if (off.getItem() instanceof MagnetismBook)
            return off;

        return ItemStack.EMPTY;
    }

    public MagnetFilterData filter() {
        return new MagnetFilterData(heldBook(player));
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput actionType, Player player) {
        var book = heldBook(player);

        if (!book.isEmpty() && slotIndex >= 0 && slotIndex < MagnetFilterData.FILTER_SLOTS) {
            var data = new MagnetFilterData(book);
            var carried = getCarried();

            if (carried.isEmpty() || carried.getItem() == data.slot(slotIndex))
                data.setSlot(slotIndex, null);
            else
                data.setSlot(slotIndex, carried.getItem());

            // Deliberately not calling super: the whole point is that no item changes hands.
            return;
        }

        // Shift-clicking a real stack sets the filter instead of moving anything.
        if (!book.isEmpty() && actionType == ContainerInput.QUICK_MOVE
                && slotIndex >= MagnetFilterData.FILTER_SLOTS && slotIndex < this.slots.size()) {
            var stack = this.slots.get(slotIndex).getItem();
            if (!stack.isEmpty()) {
                new MagnetFilterData(book).addToFirstEmpty(stack.getItem());
                return;
            }
        }

        super.clicked(slotIndex, button, actionType, player);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != MODE_BUTTON_ID)
            return false;

        var book = heldBook(player);
        if (book.isEmpty())
            return false;

        // Goes through setMode, which is also what the sneak gesture uses - so the previous-mode
        // bookkeeping behaves identically whichever way the mode changed, including when the cycle
        // passes through NONE.
        MagnetFilterData.setMode(book, MagnetFilterData.modeOf(book).next());
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        // Handled entirely in clicked(): a quick-move onto the ghost range would otherwise be the
        // one path that could put a real item in the book.
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return !heldBook(player).isEmpty();
    }

    /**
     * Read-through view of the filter as a container, so vanilla's slot rendering can draw it.
     *
     * <p>Every read goes back to {@link MagnetFilterData}, which does its own component-identity
     * staleness check - so a filter item added by the sneak-at-an-item gesture while this screen is
     * open shows up in the grid on the next frame, with nothing to invalidate by hand.
     */
    private record GhostContainer(Player owner) implements Container {
        @Override public int getContainerSize() { return MagnetFilterData.FILTER_SLOTS; }
        @Override public boolean isEmpty() {
            for (int i = 0; i < MagnetFilterData.FILTER_SLOTS; i++)
                if (!getItem(i).isEmpty()) return false;
            return true;
        }
        @Override public ItemStack getItem(int slot) {
            // Resolves the book on every read instead of holding the one that existed when the
            // menu opened. Ghost slots deliberately ignore the slot-sync channel - that is the
            // no-real-items hardening - so re-reading components is their ONLY update path, and
            // when the server syncs the hotbar the client builds a NEW ItemStack. Watching the old
            // object's component identity can never fire, which is why filter edits only appeared
            // after closing and reopening: reopening resolved the hand afresh.
            var book = heldBook(owner);
            if (book.isEmpty()) return ItemStack.EMPTY;
            var item = new MagnetFilterData(book).slot(slot);
            return item == null ? ItemStack.EMPTY : new ItemStack(item);
        }
        @Override public ItemStack removeItem(int slot, int amount) { return ItemStack.EMPTY; }
        @Override public ItemStack removeItemNoUpdate(int slot) { return ItemStack.EMPTY; }
        @Override public void setItem(int slot, ItemStack value) { }
        @Override public void setChanged() { }
        @Override public boolean stillValid(Player player) { return true; }
        @Override public void clearContent() { }
    }
}
