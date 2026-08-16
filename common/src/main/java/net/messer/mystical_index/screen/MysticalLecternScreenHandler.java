package net.messer.mystical_index.screen;

import net.messer.mystical_index.item.inventory.BookItemVariant;
import net.messer.config.ModConfig;
import net.messer.mystical_index.block.ModBlocks;
import net.messer.mystical_index.block.entity.LibraryBlockEntity;
import net.messer.mystical_index.item.inventory.LibraryNetwork;
import net.messer.mystical_index.network.LecternNetworking;
import net.messer.util.ServerPacketSender;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.recipe.RecipeType;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

import java.util.List;

public class MysticalLecternScreenHandler extends ScreenHandler {
    private final ScreenHandlerContext context;
    private final PlayerEntity player;
    private final CraftingInventory input = new CraftingInventory(this, 3, 3);
    private final CraftingResultInventory result = new CraftingResultInventory();
    private final ServerPlayerEntity serverPlayer;

    private boolean networkDirty;
    private int tickCounter;
    private List<LibraryNetwork.Entry> lastSent = List.of();
    private List<LibraryNetwork.Entry> clientEntries = List.of();

    // A shift-click drives the vanilla QUICK_MOVE loop, which re-enters quickMove (and the result
    // slot's refill) once per craft. Everything below is scoped to a single user click so that one
    // click resolves the library scan and the grid layout once instead of per craft iteration.
    // MAX_CRAFTS_PER_CLICK bounds how many network-fed refills a single click may perform; without
    // it a full network drains into the player's inventory in one click and freezes the tick.
    private static final int MAX_CRAFTS_PER_CLICK = 64;
    private boolean clickActive;
    private List<LibraryBlockEntity> clickLibraries;
    private ItemStack[] clickGridSnapshot;
    private int clickCrafts;
    private boolean resultSyncPending;

    public MysticalLecternScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, ScreenHandlerContext.EMPTY);
    }

    public MysticalLecternScreenHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
        super(ModScreenHandlers.MYSTICAL_LECTERN_SCREEN_HANDLER.get(), syncId);
        this.context = context;
        this.player = playerInventory.player;
        this.serverPlayer = playerInventory.player instanceof ServerPlayerEntity spe ? spe : null;
        this.networkDirty = this.serverPlayer != null;

        this.addSlot(new NetworkResultSlot(this.player, input, result, 0, 124, 90));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new Slot(input, col + row * 3, 30 + col * 18, 72 + row * 18));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 139 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 197));
        }
    }

    public List<LibraryNetwork.Entry> getClientEntries() {
        return clientEntries;
    }

    public void setClientEntries(List<LibraryNetwork.Entry> entries) {
        this.clientEntries = entries;
    }

    private List<LibraryBlockEntity> libraries() {
        if (clickLibraries != null)
            return clickLibraries;

        var found = context.get((world, pos) -> LibraryNetwork.findLibraries(world, pos, ModConfig.LecternRange)).orElse(List.of());
        // Memoise the scan for the rest of the click: one click can hit this many times (each
        // craft's refill, plus shift-inserts), and re-scanning chunks every time is the freeze.
        if (clickActive)
            clickLibraries = found;
        return found;
    }

    @Override
    public void onContentChanged(Inventory inventory) {
        context.run((world, pos) -> updateResult(world));
    }

    private void updateResult(World world) {
        if (world.isClient || serverPlayer == null)
            return;

        var crafted = ItemStack.EMPTY;
        var recipeInput = input.createRecipeInput();
        var match = world.getRecipeManager().getFirstMatch(RecipeType.CRAFTING, recipeInput, world);
        if (match.isPresent()) {
            // getFirstMatch hands back the registry entry now; the recipe itself is one hop in.
            var entry = match.get();
            if (result.shouldCraftRecipe(world, serverPlayer, entry)) {
                var output = entry.value().craft(recipeInput, world.getRegistryManager());
                if (output.isItemEnabled(world.getEnabledFeatures()))
                    crafted = output;
            }
        }

        result.setStack(0, crafted);
        setPreviousTrackedSlot(0, crafted);

        // During a click the auto-refill mutates the grid up to nine times per craft, and each
        // mutation lands here. Hold the result packet and flush a single update when the click
        // ends instead of firing one packet per grid mutation. The loop still terminates correctly
        // because it reads the server-side result slot (set above), not the client packet.
        if (clickActive) {
            resultSyncPending = true;
            return;
        }

        ServerPacketSender.send(serverPlayer, new ScreenHandlerSlotUpdateS2CPacket(syncId, nextRevision(), 0, crafted));
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return canUse(context, player, ModBlocks.MYSTICAL_LECTERN.get());
    }

    @Override
    public void onClosed(PlayerEntity player) {
        // The auto-refill keeps the 3x3 grid full and every extract lands the taken stack on the
        // cursor, so both are full of network items at close. Push them back into storage first and
        // only offer/drop what the network refuses; the plain dropInventory + super.onClosed path
        // used to throw stacks on the floor whenever the inventory was full.
        if (serverPlayer != null) {
            var libraries = libraries();

            // Handle the cursor before super.onClosed, which would otherwise offer/drop it itself.
            var cursor = getCursorStack();
            if (!cursor.isEmpty()) {
                LibraryNetwork.insert(libraries, cursor);
                if (!cursor.isEmpty())
                    player.getInventory().offerOrDrop(cursor);
                setCursorStack(ItemStack.EMPTY);
            }

            for (int slot = 0; slot < input.size(); slot++) {
                var stack = input.removeStack(slot);
                if (stack.isEmpty())
                    continue;

                LibraryNetwork.insert(libraries, stack);
                if (!stack.isEmpty())
                    player.getInventory().offerOrDrop(stack);
            }
        }

        super.onClosed(player);
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        // Establish the per-click scope so the library scan and grid snapshot are resolved once for
        // the whole click (including every iteration of the vanilla QUICK_MOVE loop) and the result
        // packet is coalesced. The guard keeps the outermost call the sole owner of the scope.
        boolean owner = !clickActive;
        if (owner)
            beginClick();
        try {
            super.onSlotClick(slotIndex, button, actionType, player);
        } finally {
            if (owner)
                endClick();
        }
    }

    private void beginClick() {
        clickActive = true;
        clickLibraries = null;
        clickGridSnapshot = null;
        clickCrafts = 0;
        resultSyncPending = false;
    }

    private void endClick() {
        clickActive = false;
        clickLibraries = null;
        clickGridSnapshot = null;
        clickCrafts = 0;

        // Flush the single coalesced result update the click accumulated, if any.
        if (resultSyncPending) {
            resultSyncPending = false;
            if (serverPlayer != null)
                ServerPacketSender.send(serverPlayer,
                        new ScreenHandlerSlotUpdateS2CPacket(syncId, nextRevision(), 0, result.getStack(0)));
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        var slot = this.slots.get(index);
        if (slot == null || !slot.hasStack())
            return ItemStack.EMPTY;

        var current = slot.getStack();
        var copied = current.copy();

        if (index >= 10) {
            // Shift-clicking out of the player inventory pushes into the library network instead of
            // another slot. Always reporting EMPTY stops onSlotClick's repeat loop, so items no book
            // is bound to just stay where they are instead of hanging the click.
            if (player.getWorld().isClient)
                return ItemStack.EMPTY;

            if (LibraryNetwork.insert(libraries(), current) > 0) {
                if (current.isEmpty())
                    slot.setStack(ItemStack.EMPTY);
                else
                    slot.markDirty();

                networkDirty = true;
            }
            return ItemStack.EMPTY;
        }

        if (index == 0) {
            current.getItem().onCraftByPlayer(current, player.getWorld(), player);
            if (!insertItem(current, 10, 46, true))
                return ItemStack.EMPTY;

            slot.onQuickTransfer(current, copied);
        } else {
            // Grid slot: fill the player inventory first, then push whatever will not fit into the
            // library network, so a full inventory no longer blocks the shift-click. The shared tail
            // below still stops the QUICK_MOVE loop once a pass moves nothing (count unchanged).
            insertItem(current, 10, 46, false);
            if (!current.isEmpty() && !player.getWorld().isClient && LibraryNetwork.insert(libraries(), current) > 0)
                networkDirty = true;
        }

        if (current.isEmpty())
            slot.setStack(ItemStack.EMPTY);
        else
            slot.markDirty();

        if (current.getCount() == copied.getCount())
            return ItemStack.EMPTY;

        slot.onTakeItem(player, current);
        if (index == 0)
            player.dropItem(current, false);

        return copied;
    }

    @Override
    public void sendContentUpdates() {
        super.sendContentUpdates();

        if (serverPlayer == null)
            return;

        tickCounter++;
        // At most one contents packet per tick, plus a once-a-second resend so books that fill
        // themselves up show through without anyone touching the screen.
        if (!networkDirty && tickCounter % 20 != 0)
            return;

        List<LibraryNetwork.Entry> snapshot = context
                .get((world, pos) -> LibraryNetwork.aggregate(LibraryNetwork.findLibraries(world, pos, ModConfig.LecternRange)))
                .orElse(List.of());

        if (networkDirty || !snapshot.equals(lastSent)) {
            LecternNetworking.sendContents(serverPlayer, syncId, snapshot);
            lastSent = snapshot;
        }

        networkDirty = false;
    }

    public void handleExtract(BookItemVariant variant, int button, boolean toInventory) {
        if (variant.isBlank())
            return;

        var libraries = libraries();
        long available = LibraryNetwork.count(libraries, variant);
        if (available <= 0)
            return;

        int maxCount = variant.getItem().getMaxCount();

        if (toInventory) {
            long extracted = LibraryNetwork.extract(libraries, variant, Math.min(maxCount, available));
            if (extracted <= 0)
                return;

            var stack = variant.toStack((int) extracted);
            player.getInventory().insertStack(stack);
            if (!stack.isEmpty()) {
                // It came straight out of bound books, so it always fits back in.
                LibraryNetwork.insert(libraries, stack);
                if (!stack.isEmpty())
                    player.dropItem(stack, false);
            }

            networkDirty = true;
            return;
        }

        var cursor = getCursorStack();
        if (cursor.isEmpty()) {
            int amount = (int) Math.min(maxCount, available);
            if (button == 1)
                amount = Math.max(1, amount / 2);

            long extracted = LibraryNetwork.extract(libraries, variant, amount);
            if (extracted > 0)
                setCursorStack(variant.toStack((int) extracted));
        } else if (variant.matches(cursor) && cursor.getCount() < maxCount) {
            int amount = button == 1 ? 1 : maxCount - cursor.getCount();

            long extracted = LibraryNetwork.extract(libraries, variant, amount);
            if (extracted > 0) {
                cursor.increment((int) extracted);
                setCursorStack(cursor);
            }
        }

        networkDirty = true;
    }

    public void handleInsert(int button) {
        var cursor = getCursorStack();
        if (cursor.isEmpty())
            return;

        var libraries = libraries();

        if (button == 1) {
            var single = cursor.copy();
            single.setCount(1);
            if (LibraryNetwork.insert(libraries, single) > 0) {
                cursor.decrement(1);
                setCursorStack(cursor.isEmpty() ? ItemStack.EMPTY : cursor);
                networkDirty = true;
            }
            return;
        }

        if (LibraryNetwork.insert(libraries, cursor) > 0) {
            setCursorStack(cursor.isEmpty() ? ItemStack.EMPTY : cursor);
            networkDirty = true;
        }
    }

    // Fills the crafting grid from a recipe the client picked in REI. The client only names item
    // types; every item placed here is sourced and validated by the server.
    public void handleFillRecipe(List<List<BookItemVariant>> slotCandidates) {
        // Hand the current grid back first, like the vanilla recipe book does. Nothing a transfer
        // displaces is ever voided.
        for (int slot = 0; slot < input.size(); slot++) {
            var stack = input.getStack(slot);
            if (stack.isEmpty())
                continue;

            input.setStack(slot, ItemStack.EMPTY);
            player.getInventory().offerOrDrop(stack);
        }

        var libraries = libraries();

        for (int slot = 0; slot < input.size() && slot < slotCandidates.size(); slot++) {
            for (var variant : slotCandidates.get(slot)) {
                if (variant.isBlank())
                    continue;

                // The player's own items go first; the network only covers what is missing.
                if (takeFromPlayer(variant) || LibraryNetwork.extract(libraries, variant, 1) > 0) {
                    input.setStack(slot, variant.toStack(1));
                    break;
                }
            }
        }

        networkDirty = true;
    }

    private boolean takeFromPlayer(BookItemVariant variant) {
        var main = player.getInventory().main;

        for (int i = 0; i < main.size(); i++) {
            var stack = main.get(i);
            if (stack.isEmpty() || !variant.matches(stack))
                continue;

            stack.decrement(1);
            if (stack.isEmpty())
                main.set(i, ItemStack.EMPTY);

            return true;
        }

        return false;
    }

    private class NetworkResultSlot extends CraftingResultSlot {
        NetworkResultSlot(PlayerEntity player, RecipeInputInventory input, Inventory result, int index, int x, int y) {
            super(player, input, result, index, x, y);
        }

        @Override
        public void onTakeItem(PlayerEntity player, ItemStack stack) {
            var grid = MysticalLecternScreenHandler.this.input;

            // The refill always restores the same recipe layout, so the pre-consumption grid is the
            // same every craft. Snapshot it once per click (before the first craft consumes it) and
            // reuse it, instead of copying all nine slots on every iteration of the QUICK_MOVE loop.
            ItemStack[] snapshot = clickGridSnapshot;
            if (snapshot == null) {
                snapshot = new ItemStack[9];
                for (int i = 0; i < 9; i++)
                    snapshot[i] = grid.getStack(i).copy();
                if (clickActive)
                    clickGridSnapshot = snapshot;
            }

            super.onTakeItem(player, stack);

            if (player.getWorld().isClient)
                return;

            // Bound the network-fed refills per click. Once the budget is spent the grid is left to
            // drain, the result slot recomputes to empty, and the QUICK_MOVE loop ends on its own,
            // capping the expensive extract calls a single shift-click can trigger.
            if (clickCrafts >= MAX_CRAFTS_PER_CLICK) {
                networkDirty = true;
                return;
            }
            clickCrafts++;

            var libraries = libraries();
            for (int i = 0; i < 9; i++) {
                if (snapshot[i].isEmpty() || !grid.getStack(i).isEmpty())
                    continue;

                var variant = BookItemVariant.of(snapshot[i]);
                if (LibraryNetwork.extract(libraries, variant, 1) > 0)
                    grid.setStack(i, variant.toStack(1));
            }

            networkDirty = true;
        }
    }
}
