package net.messer.mystical_index.screen;

import net.messer.mystical_index.block.ModBlocks;
import net.messer.mystical_index.item.custom.base_books.BaseStorageBook;
import net.messer.mystical_index.item.inventory.LibraryBookSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.Slot;
import net.minecraft.world.World;

public class LibraryInventoryScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final World world;
    private final ScreenHandlerContext context;

    // Client side: no world/pos to bind to, so canUse can never test the block.
    public LibraryInventoryScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(5), ScreenHandlerContext.EMPTY);
    }

    public LibraryInventoryScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory) {
        this(syncId, playerInventory, inventory, ScreenHandlerContext.EMPTY);
    }

    public LibraryInventoryScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, ScreenHandlerContext context) {
        super(ModScreenHandlers.LIBRARY_INVENTORY_SCREEN_HANDLER.get(), syncId);
        checkSize(inventory,5);
        this.inventory = inventory;
        this.world = playerInventory.player.getWorld();
        this.context = context;
        inventory.onOpen(playerInventory.player);

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
    public ItemStack quickMove(PlayerEntity player, int slot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot2 = (Slot) this.slots.get(slot);
        if (slot2 != null && slot2.hasStack()) {
            ItemStack originalStack = slot2.getStack();
            newStack = originalStack.copy();
            if (slot < this.inventory.size()) {
                if (!this.insertItem(originalStack, this.inventory.size(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.insertItem(originalStack, 0, this.inventory.size(), false)) {
                return ItemStack.EMPTY;
            }

            if (originalStack.isEmpty()) {
                slot2.setStack(ItemStack.EMPTY);
            } else {
                slot2.markDirty();
            }
        }

        return newStack;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        // SimpleInventory.canPlayerUse is always true, so the screen never closed when the block
        // was broken or the player walked away — books dropped in then vanished into an orphaned
        // handler. Bind to the block instead so the screen auto-closes.
        return canUse(context, player, ModBlocks.LIBRARY.get());
    }

    public static boolean isStorageBook(ItemStack itemStack){
        if(itemStack.getItem() instanceof BaseStorageBook) return true;

        return false;
    }

}
