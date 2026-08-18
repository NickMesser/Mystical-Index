package net.messer.mystical_index.screen;

import net.messer.mystical_index.item.inventory.BookItemVariant;
import net.messer.config.ModConfig;
import net.messer.mystical_index.block.ModBlocks;
import net.messer.mystical_index.block.entity.LibraryBlockEntity;
import net.messer.mystical_index.item.inventory.LibraryNetwork;
import net.messer.mystical_index.network.LecternNetworking;
import net.messer.util.ServerPacketSender;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.List;

import net.minecraft.server.level.ServerLevel;

public class MysticalLecternScreenHandler extends AbstractContainerMenu {
    private final ContainerLevelAccess context;
    private final Player player;
    private final TransientCraftingContainer input = new TransientCraftingContainer(this, 3, 3);
    private final ResultContainer result = new ResultContainer();
    private final ServerPlayer serverPlayer;

    private boolean networkDirty;
    private int tickCounter;
    private List<LibraryNetwork.Entry> lastSent = List.of();
    private List<net.minecraft.core.BlockPos> lastLinks = null;
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

    public MysticalLecternScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, ContainerLevelAccess.NULL);
    }

    public MysticalLecternScreenHandler(int syncId, Inventory playerInventory, ContainerLevelAccess context) {
        super(ModScreenHandlers.MYSTICAL_LECTERN_SCREEN_HANDLER.get(), syncId);
        this.context = context;
        this.player = playerInventory.player;
        this.serverPlayer = playerInventory.player instanceof ServerPlayer spe ? spe : null;
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

    public LibraryNetwork.Capacity clientCapacity = LibraryNetwork.Capacity.EMPTY;

    /**
     * Ships the linked library set when it differs from what this screen last saw. Rides the same
     * dirty/resend cycle as the contents, so a library appearing or unloading updates a visible
     * overlay without its own timer. lastLinks starts null so the first pass always sends, which
     * is what covers "on screen open".
     */
    private void sendLinksIfChanged() {
        if (serverPlayer == null)
            return;

        var links = context
                .evaluate((world, pos) -> {
                    var found = new java.util.ArrayList<net.minecraft.core.BlockPos>();
                    for (var library : LibraryNetwork.findLibraries(world, pos, ModConfig.LecternRange))
                        found.add(library.getBlockPos());
                    return new java.util.AbstractMap.SimpleEntry<>(pos, found);
                })
                .orElse(null);

        if (links == null)
            return;

        if (links.getValue().equals(lastLinks))
            return;

        lastLinks = links.getValue();
        LecternNetworking.sendLinks(serverPlayer, containerId, links.getKey(),
                ModConfig.LecternRange, links.getValue());
    }

    public void setClientEntries(List<LibraryNetwork.Entry> entries) {
        this.clientEntries = entries;
    }

    private List<LibraryBlockEntity> libraries() {
        if (clickLibraries != null)
            return clickLibraries;

        var found = context.evaluate((world, pos) -> LibraryNetwork.findLibraries(world, pos, ModConfig.LecternRange)).orElse(List.of());
        // Memoise the scan for the rest of the click: one click can hit this many times (each
        // craft's refill, plus shift-inserts), and re-scanning chunks every time is the freeze.
        if (clickActive)
            clickLibraries = found;
        return found;
    }

    @Override
    public void slotsChanged(Container inventory) {
        context.execute((world, pos) -> updateResult(world));
    }

    private void updateResult(Level world) {
        if (world.isClientSide() || serverPlayer == null)
            return;

        var crafted = ItemStack.EMPTY;
        var recipeInput = input.asCraftInput();
        var match = ((ServerLevel) world).recipeAccess().getRecipeFor(RecipeType.CRAFTING, recipeInput, world);
        if (match.isPresent()) {
            // getFirstMatch hands back the registry entry now; the recipe itself is one hop in.
            var entry = match.get();
            if (result.setRecipeUsed(serverPlayer, entry)) {
                var output = entry.value().assemble(recipeInput);
                if (output.isItemEnabled(world.enabledFeatures()))
                    crafted = output;
            }
        }

        result.setItem(0, crafted);
        setRemoteSlot(0, crafted);

        // During a click the auto-refill mutates the grid up to nine times per craft, and each
        // mutation lands here. Hold the result packet and flush a single update when the click
        // ends instead of firing one packet per grid mutation. The loop still terminates correctly
        // because it reads the server-side result slot (set above), not the client packet.
        if (clickActive) {
            resultSyncPending = true;
            return;
        }

        ServerPacketSender.send(serverPlayer, new ClientboundContainerSetSlotPacket(containerId, incrementStateId(), 0, crafted));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(context, player, ModBlocks.MYSTICAL_LECTERN.get());
    }

    @Override
    public void removed(Player player) {
        // The auto-refill keeps the 3x3 grid full and every extract lands the taken stack on the
        // cursor, so both are full of network items at close. Push them back into storage first and
        // only offer/drop what the network refuses; the plain dropInventory + super.onClosed path
        // used to throw stacks on the floor whenever the inventory was full.
        if (serverPlayer != null) {
            var libraries = libraries();

            // Handle the cursor before super.onClosed, which would otherwise offer/drop it itself.
            var cursor = getCarried();
            if (!cursor.isEmpty()) {
                LibraryNetwork.insert(libraries, cursor);
                if (!cursor.isEmpty())
                    player.getInventory().placeItemBackInInventory(cursor);
                setCarried(ItemStack.EMPTY);
            }

            for (int slot = 0; slot < input.getContainerSize(); slot++) {
                var stack = input.removeItemNoUpdate(slot);
                if (stack.isEmpty())
                    continue;

                LibraryNetwork.insert(libraries, stack);
                if (!stack.isEmpty())
                    player.getInventory().placeItemBackInInventory(stack);
            }
        }

        super.removed(player);
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput actionType, Player player) {
        // Establish the per-click scope so the library scan and grid snapshot are resolved once for
        // the whole click (including every iteration of the vanilla QUICK_MOVE loop) and the result
        // packet is coalesced. The guard keeps the outermost call the sole owner of the scope.
        boolean owner = !clickActive;
        if (owner)
            beginClick();
        try {
            super.clicked(slotIndex, button, actionType, player);
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
                        new ClientboundContainerSetSlotPacket(containerId, incrementStateId(), 0, result.getItem(0)));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        var slot = this.slots.get(index);
        if (slot == null || !slot.hasItem())
            return ItemStack.EMPTY;

        var current = slot.getItem();
        var copied = current.copy();

        if (index >= 10) {
            // Shift-clicking out of the player inventory pushes into the library network instead of
            // another slot. Always reporting EMPTY stops onSlotClick's repeat loop, so items no book
            // is bound to just stay where they are instead of hanging the click.
            if (player.level().isClientSide())
                return ItemStack.EMPTY;

            if (LibraryNetwork.insert(libraries(), current) > 0) {
                if (current.isEmpty())
                    slot.set(ItemStack.EMPTY);
                else
                    slot.setChanged();

                networkDirty = true;
            }
            return ItemStack.EMPTY;
        }

        if (index == 0) {
            current.getItem().onCraftedBy(current, player);
            if (!moveItemStackTo(current, 10, 46, true))
                return ItemStack.EMPTY;

            slot.onQuickCraft(current, copied);
        } else {
            // Grid slot: fill the player inventory first, then push whatever will not fit into the
            // library network, so a full inventory no longer blocks the shift-click. The shared tail
            // below still stops the QUICK_MOVE loop once a pass moves nothing (count unchanged).
            moveItemStackTo(current, 10, 46, false);
            if (!current.isEmpty() && !player.level().isClientSide() && LibraryNetwork.insert(libraries(), current) > 0)
                networkDirty = true;
        }

        if (current.isEmpty())
            slot.set(ItemStack.EMPTY);
        else
            slot.setChanged();

        if (current.getCount() == copied.getCount())
            return ItemStack.EMPTY;

        slot.setByPlayer(current);
        if (index == 0)
            player.spawnAtLocation((ServerLevel) player.level(), current);

        return copied;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();

        if (serverPlayer == null)
            return;

        tickCounter++;
        // At most one contents packet per tick, plus a once-a-second resend so books that fill
        // themselves up show through without anyone touching the screen.
        if (!networkDirty && tickCounter % 20 != 0)
            return;

        List<LibraryNetwork.Entry> snapshot = context
                .evaluate((world, pos) -> LibraryNetwork.aggregate(LibraryNetwork.findLibraries(world, pos, ModConfig.LecternRange)))
                .orElse(List.of());

        if (networkDirty || !snapshot.equals(lastSent)) {
            // Computed on the same evaluate() as the aggregate so both describe one consistent
            // view of the network rather than two scans a tick apart.
            var capacity = context
                    .evaluate((world, pos) -> LibraryNetwork.capacity(
                            LibraryNetwork.findLibraries(world, pos, ModConfig.LecternRange)))
                    .orElse(LibraryNetwork.Capacity.EMPTY);

            LecternNetworking.sendContents(serverPlayer, containerId, snapshot, capacity);
            lastSent = snapshot;
            sendLinksIfChanged();
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

        int maxCount = variant.getItem().getDefaultMaxStackSize();

        if (toInventory) {
            long extracted = LibraryNetwork.extract(libraries, variant, Math.min(maxCount, available));
            if (extracted <= 0)
                return;

            var stack = variant.toStack((int) extracted);
            player.getInventory().add(stack);
            if (!stack.isEmpty()) {
                // It came straight out of bound books, so it always fits back in.
                LibraryNetwork.insert(libraries, stack);
                if (!stack.isEmpty())
                    player.spawnAtLocation((ServerLevel) player.level(), stack);
            }

            networkDirty = true;
            return;
        }

        var cursor = getCarried();
        if (cursor.isEmpty()) {
            int amount = (int) Math.min(maxCount, available);
            if (button == 1)
                amount = Math.max(1, amount / 2);

            long extracted = LibraryNetwork.extract(libraries, variant, amount);
            if (extracted > 0)
                setCarried(variant.toStack((int) extracted));
        } else if (variant.matches(cursor) && cursor.getCount() < maxCount) {
            int amount = button == 1 ? 1 : maxCount - cursor.getCount();

            long extracted = LibraryNetwork.extract(libraries, variant, amount);
            if (extracted > 0) {
                cursor.grow((int) extracted);
                setCarried(cursor);
            }
        }

        networkDirty = true;
    }

    public void handleInsert(int button) {
        var cursor = getCarried();
        if (cursor.isEmpty())
            return;

        var libraries = libraries();

        if (button == 1) {
            var single = cursor.copy();
            single.setCount(1);
            if (LibraryNetwork.insert(libraries, single) > 0) {
                cursor.shrink(1);
                setCarried(cursor.isEmpty() ? ItemStack.EMPTY : cursor);
                networkDirty = true;
            }
            return;
        }

        if (LibraryNetwork.insert(libraries, cursor) > 0) {
            setCarried(cursor.isEmpty() ? ItemStack.EMPTY : cursor);
            networkDirty = true;
        }
    }

    // Fills the crafting grid from a recipe the client picked in REI. The client only names item
    // types; every item placed here is sourced and validated by the server.
    public void handleFillRecipe(List<List<BookItemVariant>> slotCandidates) {
        // Hand the current grid back first, like the vanilla recipe book does. Nothing a transfer
        // displaces is ever voided.
        for (int slot = 0; slot < input.getContainerSize(); slot++) {
            var stack = input.getItem(slot);
            if (stack.isEmpty())
                continue;

            input.setItem(slot, ItemStack.EMPTY);
            player.getInventory().placeItemBackInInventory(stack);
        }

        var libraries = libraries();

        for (int slot = 0; slot < input.getContainerSize() && slot < slotCandidates.size(); slot++) {
            for (var variant : slotCandidates.get(slot)) {
                if (variant.isBlank())
                    continue;

                // The player's own items go first; the network only covers what is missing.
                if (takeFromPlayer(variant) || LibraryNetwork.extract(libraries, variant, 1) > 0) {
                    input.setItem(slot, variant.toStack(1));
                    break;
                }
            }
        }

        networkDirty = true;
    }

    private boolean takeFromPlayer(BookItemVariant variant) {
        var main = player.getInventory().getNonEquipmentItems();

        for (int i = 0; i < main.size(); i++) {
            var stack = main.get(i);
            if (stack.isEmpty() || !variant.matches(stack))
                continue;

            stack.shrink(1);
            if (stack.isEmpty())
                main.set(i, ItemStack.EMPTY);

            return true;
        }

        return false;
    }

    private class NetworkResultSlot extends ResultSlot {
        NetworkResultSlot(Player player, CraftingContainer input, Container result, int index, int x, int y) {
            super(player, input, result, index, x, y);
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            var grid = MysticalLecternScreenHandler.this.input;

            // The refill always restores the same recipe layout, so the pre-consumption grid is the
            // same every craft. Snapshot it once per click (before the first craft consumes it) and
            // reuse it, instead of copying all nine slots on every iteration of the QUICK_MOVE loop.
            ItemStack[] snapshot = clickGridSnapshot;
            if (snapshot == null) {
                snapshot = new ItemStack[9];
                for (int i = 0; i < 9; i++)
                    snapshot[i] = grid.getItem(i).copy();
                if (clickActive)
                    clickGridSnapshot = snapshot;
            }

            // Vanilla's ResultSlot.onTake is what consumes the grid: it walks the crafting input
            // and calls craftSlots.removeItem(i, 1) per ingredient, then places any recipe
            // remainders (buckets and the like). This override previously called setByPlayer
            // instead, which updates the slot but consumes NOTHING - so the ingredients were never
            // taken, and the refill loop below (which only tops up slots it finds ALREADY empty)
            // had nothing to do. Delegating restores the exact vanilla consume on both loaders.
            //
            // Byte-for-byte diff of ResultSlot.onTake across the Fabric and NeoForge runtime jars:
            // identical except for two CommonHooks.setCraftingPlayer calls NeoForge wraps around
            // getRemainingItems. The consume mechanics - craftSlots.removeItem(i, 1) - are the same
            // instruction on both, so owning the call site is what makes the semantics loader-proof.
            super.onTake(player, stack);

            if (player.level().isClientSide())
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
                if (snapshot[i].isEmpty() || !grid.getItem(i).isEmpty())
                    continue;

                var variant = BookItemVariant.of(snapshot[i]);
                if (LibraryNetwork.extract(libraries, variant, 1) > 0)
                    grid.setItem(i, variant.toStack(1));
            }

            networkDirty = true;
        }
    }
}
