package net.messer.mystical_index.screen;

import net.messer.mystical_index.block.ModBlocks;
import net.messer.mystical_index.block.entity.ScriptoriumBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Scriptorium's five book slots.
 *
 * <p>A block menu, not an item one: the container belongs to the block entity and the menu holds a
 * live reference to it, so contents sync through the normal slot channel and there is no stack to
 * capture or resolve. The client constructor binds a throwaway container of the same size - the
 * server's contents arrive over that channel.
 */
public class ScriptoriumScreenHandler extends AbstractContainerMenu {

    public static final int SIZE = ScriptoriumBlockEntity.SIZE;

    private final Container inventory;
    private final ContainerLevelAccess context;

    // Client side: no world or position to bind to, so stillValid can never test the block.
    public ScriptoriumScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(SIZE), ContainerLevelAccess.NULL);
    }

    public ScriptoriumScreenHandler(int syncId, Inventory playerInventory, Container inventory,
                                    ContainerLevelAccess context) {
        super(ModScreenHandlers.SCRIPTORIUM_SCREEN_HANDLER.get(), syncId);
        checkContainerSize(inventory, SIZE);
        this.inventory = inventory;
        this.context = context;
        inventory.startOpen(playerInventory.player);

        for (int i = 0; i < SIZE; i++) {
            addSlot(new Slot(inventory, i, 44 + i * 18, 20) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    // The one shared predicate - a Book Sling is refused here exactly as it is by
                    // the sling itself, so books can never nest through a block either.
                    return ScriptoriumBlockEntity.accepts(stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, row * 18 + 51));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 109));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        ItemStack moved = ItemStack.EMPTY;
        Slot clicked = this.slots.get(slot);
        if (clicked == null || !clicked.hasItem())
            return moved;

        ItemStack inSlot = clicked.getItem();
        moved = inSlot.copy();

        if (slot < SIZE) {
            // Out of the block, back to the player.
            if (!moveItemStackTo(inSlot, SIZE, this.slots.size(), true))
                return ItemStack.EMPTY;
        } else {
            // In, but only what the block accepts.
            if (!ScriptoriumBlockEntity.accepts(inSlot) || !moveItemStackTo(inSlot, 0, SIZE, false))
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
        // Bound to the block rather than the container: SimpleContainer.stillValid is always true,
        // so without this the screen would stay open after the block was broken or the player
        // walked away, and books dropped in would vanish into an orphaned handler.
        return stillValid(context, player, ModBlocks.SCRIPTORIUM.get());
    }
}
