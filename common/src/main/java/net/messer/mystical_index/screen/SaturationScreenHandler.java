package net.messer.mystical_index.screen;

import net.messer.config.ModConfig;
import net.messer.mystical_index.item.custom.SaturationBook;
import net.messer.mystical_index.item.inventory.SingleItemStackingInventory;
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
 * The Book of Saturation's single food slot.
 *
 * <p>The slot is a real store, not a ghost: it holds the food the book eats from. It is backed by
 * the same {@link SingleItemStackingInventory} the book has always used, so an existing book opens
 * with its contents already in the slot and the feeding path is untouched.
 *
 * <p>Book resolution is the usual main-hand-then-offhand scan, identical on both sides.
 */
public class SaturationScreenHandler extends AbstractContainerMenu {

    private final Player player;
    private final Container food;

    public SaturationScreenHandler(int syncId, Inventory playerInventory) {
        super(ModScreenHandlers.SATURATION_SCREEN_HANDLER.get(), syncId);
        this.player = playerInventory.player;
        this.food = new FoodContainer(player);

        addSlot(new Slot(food, 0, 80, 20) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                // FOOD, not CONSUMABLE. CONSUMABLE only says "this can be drunk or eaten" and
                // covers potions and milk, which carry no nutrition at all - they would sit in the
                // slot doing nothing. FOOD is what holds nutrition and saturation, and it is what
                // the feed path hands to FoodData.eat.
                return stack.has(DataComponents.FOOD);
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 51 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 109));
        }
    }

    public static ItemStack heldBook(Player player) {
        var main = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (main.getItem() instanceof SaturationBook)
            return main;

        var off = player.getItemInHand(InteractionHand.OFF_HAND);
        if (off.getItem() instanceof SaturationBook)
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

        if (slot == 0) {
            if (!moveItemStackTo(inSlot, 1, this.slots.size(), true))
                return ItemStack.EMPTY;
        } else {
            // Only food goes in, and only into the one slot.
            if (!inSlot.has(DataComponents.FOOD) || !moveItemStackTo(inSlot, 0, 1, false))
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
        return !heldBook(player).isEmpty();
    }

    /**
     * Adapter onto the book's stored inventory that notices writes made underneath it.
     *
     * <p>This is the sharpest case of the staleness problem in the mod: the auto-feed shrinks the
     * stored stack on a tick while the screen is displaying it. Every write through the shared
     * custom-data path replaces the component object, so an identity check is a complete signal
     * that the backing inventory has to be rebuilt - which is what makes the visible count drop as
     * the book eats, and makes a click straight after a feed act on the real remaining stack rather
     * than on a snapshot from before it.
     */
    private static class FoodContainer implements Container {
        private final Player owner;
        private SingleItemStackingInventory backing;
        private CustomData seen;
        private boolean loaded;

        FoodContainer(Player owner) {
            this.owner = owner;
            reloadIfStale();
        }

        // Menus never store the book stack; they store the player and resolve per use. A synced
        // slot update replaces the client's ItemStack object, so a captured one goes orphaned.
        private ItemStack book() {
            return heldBook(owner);
        }

        private void reloadIfStale() {
            var book = book();
            if (book.isEmpty())
                return;

            var current = book.get(DataComponents.CUSTOM_DATA);
            if (loaded && current == seen)
                return;

            seen = current;
            loaded = true;
            backing = new SingleItemStackingInventory(book(), ModConfig.SaturationBookMaxStacks);
        }

        private void flush() {
            if (backing == null)
                return;

            // Goes through the inventory's own write, which funnels into the shared custom-data
            // path - so the glint and the reequip suppression stay correct and only this book's
            // own keys are touched.
            backing.writeNbt();
            seen = book().get(DataComponents.CUSTOM_DATA);
        }

        @Override public int getContainerSize() { return 1; }

        @Override public boolean isEmpty() {
            reloadIfStale();
            return backing == null || backing.getFirstItemStack().isEmpty();
        }

        @Override public ItemStack getItem(int slot) {
            reloadIfStale();
            return backing == null ? ItemStack.EMPTY : backing.getFirstItemStack();
        }

        @Override public ItemStack removeItem(int slot, int amount) {
            reloadIfStale();
            if (backing == null)
                return ItemStack.EMPTY;

            var stack = backing.getFirstItemStack();
            if (stack.isEmpty())
                return ItemStack.EMPTY;

            var taken = stack.split(Math.min(amount, stack.getCount()));
            flush();
            return taken;
        }

        @Override public ItemStack removeItemNoUpdate(int slot) {
            return removeItem(slot, Integer.MAX_VALUE);
        }

        @Override public void setItem(int slot, ItemStack value) {
            reloadIfStale();
            if (backing == null)
                return;

            backing.setItem(0, value);
            if (!value.isEmpty())
                backing.setCurrentlyStoredItem(value.getItem());

            flush();
        }

        @Override public void setChanged() { flush(); }
        @Override public boolean stillValid(Player player) { return true; }
        @Override public void clearContent() { setItem(0, ItemStack.EMPTY); }
    }
}
