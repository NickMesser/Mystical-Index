package net.messer.mystical_index.screen;

import net.messer.mystical_index.block.ModBlocks;
import net.messer.mystical_index.item.custom.BookSling;
import net.messer.mystical_index.item.custom.base_books.BaseStorageBook;
import net.messer.mystical_index.item.inventory.LibraryBookSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.Level;

public class LibraryInventoryScreenHandler extends AbstractContainerMenu {
    private final Container inventory;
    private final Level world;
    private final ContainerLevelAccess context;

    // Client side: no world/pos to bind to, so canUse can never test the block.
    public LibraryInventoryScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(5), ContainerLevelAccess.NULL);
    }

    public LibraryInventoryScreenHandler(int syncId, Inventory playerInventory, Container inventory) {
        this(syncId, playerInventory, inventory, ContainerLevelAccess.NULL);
    }

    public LibraryInventoryScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerLevelAccess context) {
        super(ModScreenHandlers.LIBRARY_INVENTORY_SCREEN_HANDLER.get(), syncId);
        checkContainerSize(inventory, 5);
        this.inventory = inventory;
        this.world = playerInventory.player.level();
        this.context = context;
        inventory.startOpen(playerInventory.player);

        for (int i = 0; i < 5; i++) {
            this.addSlot(new LibraryBookSlot(this, inventory, i, 44 + i * 18, 20));
        }

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                this.addSlot(new Slot(playerInventory, x + y * 9 + 9, 8 + x * 18, y * 18 + 51));
            }
        }
        for (int x = 0; x < 9; x++) {
            this.addSlot(new Slot(playerInventory, x, 8 + x * 18, 109));
        }
    }



    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot2 = (Slot) this.slots.get(slot);
        if (slot2 != null && slot2.hasItem()) {
            ItemStack originalStack = slot2.getItem();
            newStack = originalStack.copy();
            if (slot < this.inventory.getContainerSize()) {
                if (!this.moveItemStackTo(originalStack, this.inventory.getContainerSize(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(originalStack, 0, this.inventory.getContainerSize(), false)) {
                return ItemStack.EMPTY;
            }

            if (originalStack.isEmpty()) {
                slot2.set(ItemStack.EMPTY);
            } else {
                slot2.setChanged();
            }
        }

        return newStack;
    }

    @Override
    public boolean stillValid(Player player) {
        // SimpleInventory.canPlayerUse is always true, so the screen never closed when the block
        // was broken or the player walked away — books dropped in then vanished into an orphaned
        // handler. Bind to the block instead so the screen auto-closes.
        return stillValid(context, player, ModBlocks.LIBRARY.get());
    }

    public static boolean isStorageBook(ItemStack itemStack){
        // Slings are barred outright rather than merely failing to be a BaseStorageBook. Their
        // contents only tick in a player inventory, so one parked in a library would look stored
        // while quietly doing nothing - and the lectern network enumerates library books, which has
        // no business descending into a sling. Stated here so it stays true if the hierarchy moves.
        if(itemStack.getItem() instanceof BookSling) return false;

        if(itemStack.getItem() instanceof BaseStorageBook) return true;

        return false;
    }

}
