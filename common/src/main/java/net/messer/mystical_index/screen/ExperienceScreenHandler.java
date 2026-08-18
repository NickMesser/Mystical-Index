package net.messer.mystical_index.screen;

import net.messer.mystical_index.item.custom.ExperienceBook;
import net.messer.mystical_index.item.inventory.ExperienceBookData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Book of Experience's controls: deposit, withdraw, and the auto-collect switch.
 *
 * <p>No book slots - the book stores a number, not items - so this is the player inventory plus
 * three buttons. All three go through {@code clickMenuButton}, which is server-authoritative, and
 * every one of them resolves the book fresh: menus never hold the stack, because a synced slot
 * update replaces the client's ItemStack object and a captured one goes orphaned.
 *
 * <p>The transfer maths lives entirely in {@link ExperienceBookData} - this layer only decides
 * which of the two directions to ask for.
 */
public class ExperienceScreenHandler extends AbstractContainerMenu {

    public static final int BUTTON_DEPOSIT = 0;
    public static final int BUTTON_WITHDRAW = 1;
    public static final int BUTTON_AUTO = 2;

    private final Player player;

    public ExperienceScreenHandler(int syncId, Inventory playerInventory) {
        super(ModScreenHandlers.EXPERIENCE_SCREEN_HANDLER.get(), syncId);
        this.player = playerInventory.player;

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
        if (main.getItem() instanceof ExperienceBook)
            return main;

        var off = player.getItemInHand(InteractionHand.OFF_HAND);
        if (off.getItem() instanceof ExperienceBook)
            return off;

        return ItemStack.EMPTY;
    }

    /** Live-resolved data for the screen to read; never cached. */
    public ExperienceBookData data() {
        var book = heldBook(player);
        return book.isEmpty() ? null : new ExperienceBookData(book);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        var book = heldBook(player);
        if (book.isEmpty())
            return false;

        var data = new ExperienceBookData(book);
        switch (id) {
            case BUTTON_DEPOSIT -> data.deposit(player);
            case BUTTON_WITHDRAW -> data.withdraw(player);
            case BUTTON_AUTO -> data.setAutoCollect(!data.autoCollect());
            default -> {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        // Nothing to move into: the book holds no items.
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return !heldBook(player).isEmpty();
    }
}
