package net.messer.mystical_index.screen;

import net.messer.mystical_index.item.custom.FarmingBook;
import net.messer.mystical_index.item.inventory.FarmingBookInventory;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Book of Farming's six slots plus the player inventory.
 *
 * <p>The book is resolved from whichever hand is holding it rather than being passed in as menu
 * data. Both sides can do that lookup identically - the client already knows what the player is
 * holding - so there is no payload to keep in sync and no slot index that can go stale if the
 * inventory shifts underneath the open screen. The trade-off is that the menu closes if the book
 * leaves the player's hands, which {@link #stillValid} enforces; that is the correct behaviour
 * anyway, since the thing being edited would otherwise be gone.
 */
public class FarmingBookScreenHandler extends AbstractContainerMenu {

    private final Player player;
    private final FarmingBookInventory inventory;

    public FarmingBookScreenHandler(int syncId, Inventory playerInventory) {
        super(ModScreenHandlers.FARMING_BOOK_SCREEN_HANDLER.get(), syncId);

        this.player = playerInventory.player;
        // Resolved once here only to seed the slots; every read below goes through inventory(),
        // which re-resolves. Menus never hold the book stack across frames.
        this.inventory = new FarmingBookInventory(heldBook(player));

        // Soil, then seed, then the four outputs - matching the panel art.
        addSlot(new Slot(inventory, FarmingBookInventory.SOIL_SLOT, 26, 20) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return FarmingBookInventory.isSoil(stack);
            }
        });
        addSlot(new Slot(inventory, FarmingBookInventory.SEED_SLOT, 52, 20) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return FarmingBookInventory.isSeed(stack);
            }
        });
        for (int i = 0; i < FarmingBookInventory.OUTPUT_SLOTS; i++) {
            addSlot(new Slot(inventory, FarmingBookInventory.FIRST_OUTPUT_SLOT + i, 98 + i * 18, 20) {
                // Harvest only comes out. Letting items in would make the outputs a second
                // storage space and let a player stuff them full to stall the cycle deliberately.
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                addSlot(new Slot(playerInventory, x + y * 9 + 9, 8 + x * 18, y * 18 + 51));
            }
        }
        for (int x = 0; x < 9; x++) {
            addSlot(new Slot(playerInventory, x, 8 + x * 18, 109));
        }
    }

    /** The Book of Farming the player currently has out, main hand first. */
    public static ItemStack heldBook(Player player) {
        var main = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (main.getItem() instanceof FarmingBook)
            return main;

        var off = player.getItemInHand(InteractionHand.OFF_HAND);
        if (off.getItem() instanceof FarmingBook)
            return off;

        return ItemStack.EMPTY;
    }

    public FarmingBookInventory inventory() {
        return inventory;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        ItemStack moved = ItemStack.EMPTY;
        Slot clicked = this.slots.get(slot);
        if (clicked == null || !clicked.hasItem())
            return moved;

        ItemStack inSlot = clicked.getItem();
        moved = inSlot.copy();

        if (slot < FarmingBookInventory.SIZE) {
            // Out of the book, into the player.
            if (!moveItemStackTo(inSlot, FarmingBookInventory.SIZE, this.slots.size(), true))
                return ItemStack.EMPTY;
        } else {
            // Into the book: only the two input slots will take anything, and each is offered
            // only what it accepts, so a shift-clicked seed cannot land in the soil slot.
            boolean placed = FarmingBookInventory.isSoil(inSlot)
                    ? moveItemStackTo(inSlot, FarmingBookInventory.SOIL_SLOT, FarmingBookInventory.SOIL_SLOT + 1, false)
                    : FarmingBookInventory.isSeed(inSlot)
                            && moveItemStackTo(inSlot, FarmingBookInventory.SEED_SLOT, FarmingBookInventory.SEED_SLOT + 1, false);

            if (!placed)
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

    @Override
    public void removed(Player player) {
        super.removed(player);
        // Every slot write already goes through the inventory's own setChanged, which persists
        // into the book's custom data; this is just the final flush for the closing frame.
        inventory.setChanged();
    }
}
