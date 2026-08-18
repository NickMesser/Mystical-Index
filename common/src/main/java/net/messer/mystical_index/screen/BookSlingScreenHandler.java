package net.messer.mystical_index.screen;

import net.messer.mystical_index.item.custom.BookSling;
import net.messer.mystical_index.item.inventory.BookSlingInventory;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * The Book Sling's five book slots.
 *
 * <p>Real slots holding real books, filtered to what a sling accepts. Book resolution is the usual
 * main-hand-then-offhand scan, identical on both sides.
 *
 * <p>The contained books keep ticking while this screen is open - a slung magnetism book still
 * runs its clock, a slung saturation book still feeds - so the backing container reloads on
 * component identity and the displayed stacks stay live.
 */
public class BookSlingScreenHandler extends AbstractContainerMenu {

    private final Player player;
    private final Container books;

    public BookSlingScreenHandler(int syncId, Inventory playerInventory) {
        super(ModScreenHandlers.BOOK_SLING_SCREEN_HANDLER.get(), syncId);
        this.player = playerInventory.player;
        this.books = new SlingContainer(player);

        // One row of five, centred: five 18px squares span 90px in a 176px panel, matching the
        // magnetism filter row the player already knows.
        for (int col = 0; col < BookSlingInventory.SIZE; col++) {
            addSlot(new Slot(books, col, 44 + col * 18, 20) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return BookSlingInventory.accepts(stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 51 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 109));
        }
    }

    public static ItemStack heldSling(Player player) {
        var main = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (main.getItem() instanceof BookSling)
            return main;

        var off = player.getItemInHand(InteractionHand.OFF_HAND);
        if (off.getItem() instanceof BookSling)
            return off;

        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        ItemStack moved = ItemStack.EMPTY;
        Slot clicked = this.slots.get(slot);
        if (clicked == null || !clicked.hasItem())
            return moved;

        ItemStack inSlot = clicked.getItem();
        moved = inSlot.copy();

        if (slot < BookSlingInventory.SIZE) {
            // Out of the sling, back to the player.
            if (!moveItemStackTo(inSlot, BookSlingInventory.SIZE, this.slots.size(), true))
                return ItemStack.EMPTY;
        } else {
            // Into the sling, and only if it is the kind of book a sling carries.
            if (!BookSlingInventory.accepts(inSlot)
                    || !moveItemStackTo(inSlot, 0, BookSlingInventory.SIZE, false))
                return ItemStack.EMPTY;
        }

        if (inSlot.isEmpty())
            clicked.set(ItemStack.EMPTY);
        else
            clicked.setChanged();

        return moved;
    }

    @Override
    public boolean stillValid(Player player) {
        return !heldSling(player).isEmpty();
    }

    /**
     * Read-through-with-cache view of the sling, reloading whenever anything else writes it.
     *
     * <p><b>Why this cannot dupe or void a book.</b> Both writers run on the server thread: the
     * sling's tick from {@code Player.tick}, and menu clicks from packet handling. Neither can
     * begin while the other is mid-operation, so the only question is whether a stale snapshot can
     * survive from one to the next - and it cannot, for two independent reasons.
     *
     * <p>First, the tick side never holds state across ticks: {@code forEachBookInSling} builds a
     * fresh {@link BookSlingInventory} on every call, so a book removed through the menu between
     * two ticks is simply absent from the next tick's read. There is no retained slot array that
     * could write a removed book back.
     *
     * <p>Second, this container - which IS retained for the screen's lifetime - reloads whenever
     * the custom-data component identity changes, and every write on either side replaces that
     * component. So a tick flush landing between two clicks is picked up before the next click
     * reads, and the menu can never write a slot back from a pre-flush snapshot.
     */
    private static class SlingContainer implements Container {
        private final Player owner;
        private BookSlingInventory backing;
        private CustomData seen;
        private boolean loaded;

        SlingContainer(Player owner) {
            this.owner = owner;
            reloadIfStale();
        }

        // Menus never store the book stack; they store the player and resolve per use.
        private ItemStack sling() {
            return heldSling(owner);
        }

        private void reloadIfStale() {
            var sling = sling();
            if (sling.isEmpty())
                return;

            var current = sling.get(DataComponents.CUSTOM_DATA);
            if (loaded && current == seen)
                return;

            seen = current;
            loaded = true;
            backing = new BookSlingInventory(sling);
        }

        private void adopt() {
            seen = sling().get(DataComponents.CUSTOM_DATA);
        }

        @Override public int getContainerSize() { return BookSlingInventory.SIZE; }

        @Override public boolean isEmpty() {
            reloadIfStale();
            return backing == null || backing.isEmpty();
        }

        @Override public ItemStack getItem(int slot) {
            reloadIfStale();
            return backing == null ? ItemStack.EMPTY : backing.getItem(slot);
        }

        @Override public ItemStack removeItem(int slot, int amount) {
            reloadIfStale();
            if (backing == null)
                return ItemStack.EMPTY;

            var removed = backing.removeItem(slot, amount);
            adopt();
            return removed;
        }

        @Override public ItemStack removeItemNoUpdate(int slot) {
            reloadIfStale();
            if (backing == null)
                return ItemStack.EMPTY;

            var removed = backing.removeItemNoUpdate(slot);
            adopt();
            return removed;
        }

        @Override public void setItem(int slot, ItemStack value) {
            reloadIfStale();
            if (backing == null)
                return;

            backing.setItem(slot, value);
            adopt();
        }

        @Override public void setChanged() {
            reloadIfStale();
            if (backing != null) {
                backing.setChanged();
                adopt();
            }
        }

        @Override public boolean stillValid(Player player) { return true; }

        @Override public void clearContent() {
            reloadIfStale();
            if (backing != null) {
                backing.clearContent();
                adopt();
            }
        }
    }
}
