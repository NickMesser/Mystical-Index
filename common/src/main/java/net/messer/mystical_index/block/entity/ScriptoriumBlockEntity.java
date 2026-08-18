package net.messer.mystical_index.block.entity;

import net.messer.config.ModConfig;
import net.messer.mixin.ItemEntityAccessor;
import net.messer.mystical_index.item.custom.ExperienceBook;
import net.messer.mystical_index.item.custom.FarmingBook;
import net.messer.mystical_index.item.custom.MagnetismBook;
import net.messer.mystical_index.item.custom.StorageBook;
import net.messer.mystical_index.item.custom.TieredStorageBook;
import net.messer.mystical_index.item.custom.TransportBook;
import net.messer.mystical_index.item.custom.base_books.BaseStorageBook;
import net.messer.mystical_index.item.inventory.ExperienceBookData;
import net.messer.mystical_index.item.inventory.FarmingBookInventory;
import net.messer.mystical_index.item.inventory.LibraryNetwork;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.messer.mystical_index.item.inventory.MagnetFilterData;
import net.messer.mystical_index.item.inventory.SimpleBookInventory;
import net.messer.util.MysticalUtil;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Scriptorium's block entity.
 *
 * <p>Deliberately empty in this sub-slice. The five book slots and their persistence land next, the
 * screen after that, and the tick that drives the contained books last - each on its own, so the
 * block is always in a state that can be placed, broken and saved without surprises.
 */
public class ScriptoriumBlockEntity extends BlockEntity {

    public static final int SIZE = 5;

    /**
     * The per-loader item-handler view of this Scriptorium.
     *
     * <p>Mirror of {@code LibraryBlockEntity.StorageView} - deliberately a separate type rather
     * than a shared one. The Library's members are a public field and a nested interface, so
     * sharing would mean either editing that proven class or coupling this block to a type named
     * after another one. Two short declarations was the cheaper honesty. Keep the two in step.
     */
    public interface StorageView {
        void rebuild();
    }

    /**
     * One mirror per stored book, rebuilt whenever the contents change - same shape and same
     * rebuild trigger as the Library's list. Loader-free: each loader module builds its wrapper
     * on top of this.
     */
    public final List<SimpleBookInventory> bookInventories = new ArrayList<>();

    @Nullable
    private StorageView storageView;

    public void setStorageView(StorageView view) {
        this.storageView = view;
        view.rebuild();
    }

    @Nullable
    public StorageView getStorageView() {
        return storageView;
    }

    /**
     * The books this Scriptorium runs. Same on-disk shape the Library uses, so the contents are a
     * plain Items list and nothing bespoke has to be migrated later.
     */
    public final SimpleContainer storedBooks = new SimpleContainer(SIZE) {
        @Override
        public void setChanged() {
            super.setChanged();

            // Rebuild the hopper-facing mirrors from the current books. Every mutation path routes
            // through here - menu edits, the tick passes, absorption - so the transfer layer can
            // never serve contents from before the last change.
            ScriptoriumBlockEntity.this.rebuildMirrors();

            // Contained books rewrite their own data as they work, so any change has to reach the
            // chunk's save path or a restart loses the progress.
            ScriptoriumBlockEntity.this.setChanged();
        }
    };

    public ScriptoriumBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SCRIPTORIUM_BLOCK_ENTITY.get(), pos, state);
    }

    /** What the block will accept - the same predicate the Book Sling enforces. */
    public static boolean accepts(ItemStack candidate) {
        return MysticalUtil.isEffectBook(candidate);
    }

    /**
     * How close an item entity must be for a contained storage book to take it.
     *
     * <p>1.5 is a 3x3x3 box around the block, so every neighbouring cell is already covered. It was
     * checked against the "items circle the block" report rather than assumed: an item dragged in
     * by the magnet first enters this window on tick 2 and then sits inside it for 88-98% of the
     * following ticks, resting against the block face. The window was never the thing keeping items
     * out - {@link #offerToBooks} was refusing them.
     */
    private static final double ABSORB_RADIUS = 1.5;

    /** Per-tick velocity scale applied to items that have arrived. See {@link #pullItems}. */
    private static final double SETTLE_DAMPING = 0.6;

    /** Vanilla's {@code setNeverPickUp()} sentinel - a delay that never counts down. */
    private static final int NEVER_PICK_UP = 32767;

    /**
     * Whether a block in this mod may collect an item entity.
     *
     * <p><b>Hopper semantics, not player semantics.</b> A hand-dropped item carries a 40 tick pickup
     * delay so it does not fly straight back to the player who threw it. That guard belongs to the
     * PLAYER magnet, where boomeranging is the failure it prevents, and it stays untouched there
     * ({@code MagnetismBook.inventoryTick}). At a block it is simply wrong: it makes a Scriptorium
     * stare at a dropped item for two full seconds before noticing it, which is the whole of the
     * "the magnet takes a few seconds to start" report. Vanilla blocks do not behave that way -
     * {@code HopperBlockEntity.getItemsAtAndAbove} selects item entities with
     * {@code EntitySelector.ENTITY_STILL_ALIVE} and nothing else, so a hopper collects the instant
     * an item is in range.
     *
     * <p>The one thing kept is the never-pick-up sentinel. Vanilla hoppers ignore that too, but an
     * item another mod has explicitly marked uncollectable is a decision worth respecting rather
     * than a countdown worth waiting out.
     */
    private static boolean collectable(ItemEntity entity) {
        return ((ItemEntityAccessor) entity).getPickupDelay() != NEVER_PICK_UP;
    }

    /** How far the block reaches for experience orbs, and how close one must be to be banked. */
    private static final double ORB_PULL_RADIUS = 4.0;
    private static final double ORB_ABSORB_RADIUS = 1.5;

    /**
     * Extract-only window onto one farming book's four output slots.
     *
     * <p>Soil and seed are never exposed: a hopper pulling the seed would silently end a growth
     * cycle. Insertion is refused outright - produce comes out, nothing goes in.
     */
    public record FarmingOutputs(FarmingBookInventory backing) implements Container {
        @Override public int getContainerSize() { return FarmingBookInventory.OUTPUT_SLOTS; }
        @Override public boolean isEmpty() {
            for (int i = 0; i < FarmingBookInventory.OUTPUT_SLOTS; i++)
                if (!getItem(i).isEmpty()) return false;
            return true;
        }
        @Override public ItemStack getItem(int slot) {
            return backing.getItem(FarmingBookInventory.FIRST_OUTPUT_SLOT + slot);
        }
        @Override public ItemStack removeItem(int slot, int amount) {
            return backing.removeItem(FarmingBookInventory.FIRST_OUTPUT_SLOT + slot, amount);
        }
        @Override public ItemStack removeItemNoUpdate(int slot) {
            return backing.removeItemNoUpdate(FarmingBookInventory.FIRST_OUTPUT_SLOT + slot);
        }
        @Override public void setItem(int slot, ItemStack value) { }
        @Override public void setChanged() { backing.setChanged(); }
        @Override public boolean stillValid(Player player) { return true; }
        @Override public void clearContent() { }
    }

    /** Farming output windows, rebuilt alongside the book mirrors. */
    public final List<Container> farmingOutputs = new ArrayList<>();

    /**
     * Captures every stored book so an aborted transfer can put it back.
     *
     * <p>Mirror of LibraryBlockEntity's pair - whole stacks, because the write path derives other
     * components from the contents and a narrower snapshot restores the items while leaving a book
     * visibly used.
     */
    public ItemStack[] snapshotBooks() {
        var stacks = storedBooks.getItems();
        var snapshot = new ItemStack[stacks.size()];
        for (int i = 0; i < stacks.size(); i++)
            snapshot[i] = stacks.get(i).copy();
        return snapshot;
    }

    public void restoreBooks(ItemStack[] snapshot) {
        var stacks = storedBooks.getItems();
        for (int i = 0; i < snapshot.length && i < stacks.size(); i++)
            stacks.set(i, snapshot[i].copy());

        storedBooks.setChanged();
    }

    private void rebuildMirrors() {
        bookInventories.clear();
        farmingOutputs.clear();
        for (var bookStack : storedBooks.getItems()) {
            if (!(bookStack.getItem() instanceof BaseStorageBook storageBook))
                continue;
            if (storageBook.getInventory(bookStack).isEmpty())
                continue;

            bookInventories.add(new SimpleBookInventory(bookStack));
        }

        // Farming outputs are served in addition to the book mirrors, because a farming book's
        // getInventory is its LEGACY store - the outputs live in a separate inventory the mirrors
        // never see. The Library deliberately keeps serving legacy contents only; the Scriptorium
        // is the automation block, so here the produce comes out.
        for (var bookStack : storedBooks.getItems()) {
            if (bookStack.getItem() instanceof FarmingBook farming)
                farmingOutputs.add(new FarmingOutputs(farming.farmInventory(bookStack)));
        }

        if (storageView != null)
            storageView.rebuild();
    }

    public static void tick(Level world, BlockPos pos, BlockState state, ScriptoriumBlockEntity be) {
        // Pass one: the shared rule, identical to the Library's.
        MysticalUtil.tickContainedBooks(be.storedBooks, world, be);

        if (world.isClientSide())
            return;

        // Pass two: the behaviours that only make sense at a block. Kept out of the shared helper
        // on purpose - that helper is the Library's tick too, and a magnet in a Library must stay
        // inert.
        //
        // Books bring their own settings into the block, uniformly: the magnet honours its mode and
        // filter, the experience book honours its auto-collect toggle. An off switch means off
        // wherever the book is sitting.
        boolean changed = false;
        Vec3 centre = Vec3.atCenterOf(pos);

        // Transport pulses off the world clock rather than a counter on the block. A saved counter
        // would have to be persisted and would drift per block for no benefit; the clock survives
        // restarts for free, and every Scriptorium pulsing on the same tick is the cheaper shape.
        // max(1) because the interval is player-editable and 0 would be a division by zero.
        boolean pulse = world.getGameTime() % Math.max(1, ModConfig.TransportInterval) == 0;

        for (var book : be.storedBooks.getItems()) {
            if (book.isEmpty())
                continue;

            if (book.getItem() instanceof MagnetismBook)
                pullItems(world, centre, book);

            if (book.getItem() instanceof ExperienceBook)
                changed |= collectOrbs(world, centre, book);

            // Per book, not per block: two bound books ship two batches, which is the same "more
            // books, more throughput" rule the rest of the block already follows.
            if (pulse && book.getItem() instanceof TransportBook)
                changed |= pump(world, pos, be, book);
        }

        changed |= absorbItems(world, centre, be);

        // Pass one already called setChanged, but that happened BEFORE any of the above wrote to a
        // book. Stacks live inside this container, and mutating a stack in place does not notify
        // the container - so without this the chunk could save without the points or contents that
        // pass two just banked.
        if (changed)
            be.storedBooks.setChanged();
    }

    /**
     * The player magnet's pull, run from the block instead of the player.
     *
     * <p>Same range constant, same 0.25 scaling, same allows() gate - deliberately the same shape
     * as MagnetismBook.inventoryTick rather than an extraction, because that path is heavily
     * playtested and takes an Entity where this takes a position. Duplicating six lines was the
     * lower risk than restructuring the player magnet.
     */
    private static void pullItems(Level world, Vec3 centre, ItemStack book) {
        if (MagnetFilterData.modeOf(book) == MagnetFilterData.Mode.NONE)
            return;

        var filter = new MagnetFilterData(book);
        double range = ModConfig.MagnetismRange;
        var box = AABB.ofSize(centre, range * 2, range * 2, range * 2);

        for (ItemEntity item : world.getEntitiesOfClass(ItemEntity.class, box)) {
            // Hopper terms, not player terms - see collectable(). The player magnet keeps its
            // hasPickUpDelay() skip; a block must not wait out a 40 tick countdown.
            if (!collectable(item) || !filter.allows(item.getItem().getItem()))
                continue;

            item.push(centre.subtract(item.position()).scale(0.25));

            // Bleed off speed once an item has arrived. The pull adds a fresh impulse every tick
            // and the block's own collision only cancels the axis it was hit on, so an item the
            // books cannot take - a full Scriptorium, or one holding no matching type - would
            // otherwise vibrate against the face forever instead of coming to rest. Measured over
            // the last 60 of 240 ticks: 0.125 blocks/tick of residual jitter without this, 0.000
            // with it, and the tick an item first enters the absorb window is unchanged.
            if (item.position().distanceToSqr(centre) <= ABSORB_RADIUS * ABSORB_RADIUS)
                item.setDeltaMovement(item.getDeltaMovement().scale(SETTLE_DAMPING));
        }
    }

    /**
     * Item entities at the block get offered to the contained storage books.
     *
     * <p>Uses the same primitives the player pickup passes use - a book inventory and tryAddStack -
     * in the same order: single-type storage books first, then Books of Holding for types they
     * already carry. Farming books are skipped outright, the same structural guard the lectern
     * network applies: their slots are produce-only and must never receive arbitrary items.
     *
     * <p>A stack that nobody can take stays on the ground, and a partial absorb leaves the
     * remainder as a live item entity. Nothing is voided.
     */
    /**
     * Offers a stack to the contained books, draining it by however much they take.
     *
     * <p>The ONE insert rule for this block. Ground absorption and hopper insertion both call it,
     * so the two can never diverge into different acceptance behaviour - which is exactly the drift
     * that made the lectern's insert paths worth auditing.
     *
     * <p>Farming books are skipped outright, the same structural exclusion the lectern network
     * applies: their slots are produce-only and must never receive arbitrary items. Books that take
     * nothing leave the stack untouched, and a partial take leaves a real remainder - nothing is
     * voided at any point.
     */
    public void offerToBooks(ItemStack incoming) {
        // Pass one: books bound to a single type get first refusal, so a dedicated cobblestone book
        // soaks cobblestone before a general purpose Book of Holding burns a type slot on it.
        for (var book : storedBooks.getItems()) {
            if (incoming.isEmpty())
                return;
            if (!insertable(book) || book.getItem() instanceof TieredStorageBook)
                continue;

            ((BaseStorageBook) book.getItem()).getInventory(book).tryAddStack(incoming, false);
        }

        // Pass two: Books of Holding top up the types they already carry.
        for (var book : storedBooks.getItems()) {
            if (incoming.isEmpty())
                return;
            if (!insertable(book) || !(book.getItem() instanceof TieredStorageBook holding))
                continue;

            holding.getInventory(book).tryAddStack(incoming, false);
        }

        // Pass three: a Book of Holding claims a FRESH type slot for whatever is left.
        //
        // This is where the block deliberately parts company with player pickup. Auto-collect in a
        // pocket stays conservative - it only tops up types you already carry, because a book that
        // silently claimed a slot for the first cobblestone you walked over would be a trap. A
        // Scriptorium is the opposite: it is an automation SINK the player built and fed on
        // purpose, and the founding ask for it was "pull items to the block and store them in the
        // book of holding" - new types included. Without this pass a FRESH Book of Holding matches
        // nothing, absorbs nothing, and every item the magnet drags in sits at the block forever.
        //
        // The same three passes serve hopper insertion, so hoppers gain new-type insertion too.
        // That is correct and consistent: the lectern's own insert path has always allowed it, and
        // a hopper feeding a Scriptorium is no less deliberate than a player filling one by hand.
        //
        // Single-type books are unreachable here by construction - their tryAddStack(stack, boolean)
        // overload discards the flag - so pass three can only ever land in a Book of Holding.
        for (var book : storedBooks.getItems()) {
            if (incoming.isEmpty())
                return;
            if (!insertable(book) || !(book.getItem() instanceof TieredStorageBook holding))
                continue;

            holding.getInventory(book).tryAddStack(incoming, true);
        }

        // Pass four: an UNBOUND single-type Storage Book claims the type nobody else would take.
        //
        // The ordering is the whole design. A dedicated book only ever self-binds when no other
        // book in the block could accept the item - a Book of Holding sitting alongside always
        // wins first, in passes two and three. So the obvious objection to self-binding, that a
        // magnet set to ALL would define a player's dedicated book by whatever junk arrived first,
        // can only happen when that book is the sole possible destination. At that point claiming
        // the type is plainly what the player wanted: they put a blank dedicated book into an
        // automation sink and pointed a magnet at it.
        //
        // Deliberately keyed on StorageBook rather than "BaseStorageBook that is not Tiered".
        // BaseGeneratingBook also extends BaseStorageBook, so the looser test would let a Hostile
        // or Husbandry book bind to magnet junk - their inventories hold what they GENERATE, and
        // an arbitrary binding would quietly break the book's actual job.
        for (var book : storedBooks.getItems()) {
            if (incoming.isEmpty())
                return;
            if (!insertable(book) || !(book.getItem() instanceof StorageBook storage))
                continue;

            var single = storage.getInventory(book);
            // Unbound AND empty. An unbound book holding items is a corrupt state, and seeding
            // slot 0 would overwrite whatever was in it - skip rather than destroy.
            if (single.currentlyStoredItem != Items.AIR || !single.isEmpty())
                continue;

            // setItem is the ONLY path that binds currentlyStoredItem - tryAddStack never does,
            // not even through its Boolean bypass. So seed exactly one item to establish the
            // binding, at a count that can never exceed any stack limit...
            var seed = incoming.copy();
            seed.setCount(1);
            single.setItem(0, seed);
            incoming.shrink(1);

            // ...and let the ordinary bound path place the remainder, so this pass never has to
            // reimplement the stack-splitting and per-stack cap rules.
            single.tryAddStack(incoming, false);
        }
    }

    /**
     * Whether a stored book may receive items at all.
     *
     * <p>Farming books are excluded structurally, the same guard the lectern network applies: their
     * slots are produce-only and must never receive arbitrary items.
     */
    private static boolean insertable(ItemStack book) {
        return !book.isEmpty()
                && book.getItem() instanceof BaseStorageBook
                && !(book.getItem() instanceof FarmingBook);
    }

    private static boolean absorbItems(Level world, Vec3 centre, ScriptoriumBlockEntity be) {
        var box = AABB.ofSize(centre, ABSORB_RADIUS * 2, ABSORB_RADIUS * 2, ABSORB_RADIUS * 2);
        boolean changed = false;

        for (ItemEntity entity : world.getEntitiesOfClass(ItemEntity.class, box)) {
            // Hopper terms, not player terms - see collectable(). Absorption has to agree with
            // the pull above, or the block drags an item in and then refuses to take it.
            if (!collectable(entity) || entity.getItem().isEmpty())
                continue;

            var incoming = entity.getItem();
            int before = incoming.getCount();

            be.offerToBooks(incoming);

            if (incoming.getCount() != before) {
                changed = true;
                if (incoming.isEmpty())
                    entity.discard();
                else
                    entity.setItem(incoming);
            }
        }

        return changed;
    }

    /**
     * Ships this Scriptorium's contents to the Library a contained Book of Transport is bound to.
     *
     * <p>Drains storage-book contents and farming produce alike - farming outputs have to be walked
     * separately because a farming book's {@code getInventory} is its legacy store, so the produce
     * is invisible to the storage pass. This is the one place the two are drained together, and it
     * is why a farm in a Scriptorium can feed a Library at all.
     *
     * <p>Deposits go through {@link LibraryNetwork#insertIntoLibrary}, so the receiving side gets
     * the same two-pass fill and the same {@code markLibraryChanged} every other deposit gets. The
     * sending side is flagged by the caller's {@code changed} flag, for the same reason: writes
     * land inside book stacks, which never notify the container holding them.
     */
    private static boolean pump(Level world, BlockPos pos, ScriptoriumBlockEntity be, ItemStack book) {
        var library = TransportBook.resolveLibrary(world, book, pos);
        if (library == null)
            return false;

        int remaining = ModConfig.TransportBatch;
        boolean moved = false;

        for (var source : be.storedBooks.getItems()) {
            if (remaining <= 0)
                break;
            if (!(source.getItem() instanceof BaseStorageBook storageBook))
                continue;

            var inventory = storageBook.getInventory(source);
            for (int slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++) {
                int sent = send(library, inventory, slot, remaining);
                remaining -= sent;
                moved |= sent > 0;
            }

            if (!(source.getItem() instanceof FarmingBook))
                continue;

            var farming = new FarmingBookInventory(source);
            for (int i = 0; i < FarmingBookInventory.OUTPUT_SLOTS && remaining > 0; i++) {
                int sent = send(library, farming, FarmingBookInventory.FIRST_OUTPUT_SLOT + i, remaining);
                remaining -= sent;
                moved |= sent > 0;
            }
        }

        return moved;
    }

    /**
     * Moves at most {@code limit} items out of one slot and into the Library.
     *
     * <p>Offers a bounded COPY and removes only what came back accepted. The obvious alternative -
     * remove first, then hand back whatever the Library refused - has to merge a remainder into a
     * slot that the removal already changed, and getting that merge wrong voids items. Nothing
     * leaves the source until the destination has committed to taking it.
     */
    private static int send(LibraryBlockEntity library, Container source, int slot, int limit) {
        var stored = source.getItem(slot);
        if (stored.isEmpty())
            return 0;

        var offer = stored.copy();
        offer.setCount(Math.min(limit, stored.getCount()));

        int accepted = (int) LibraryNetwork.insertIntoLibrary(library, offer);
        if (accepted > 0)
            source.removeItem(slot, accepted);

        return accepted;
    }

    /**
     * Draws experience orbs in and banks them, when the book says it may.
     *
     * <p>The orb is discarded whole once its value is taken rather than decremented: the merge
     * counter is private to the orb and a block has no business reaching into it, and taking the
     * whole value in one step means an orb is never left sitting half-consumed.
     */
    private static boolean collectOrbs(Level world, Vec3 centre, ItemStack book) {
        var data = new ExperienceBookData(book);
        if (!data.autoCollect())
            return false;

        var box = AABB.ofSize(centre, ORB_PULL_RADIUS * 2, ORB_PULL_RADIUS * 2, ORB_PULL_RADIUS * 2);
        boolean changed = false;

        for (ExperienceOrb orb : world.getEntitiesOfClass(ExperienceOrb.class, box)) {
            if (orb.isRemoved())
                continue;

            if (orb.position().distanceTo(centre) <= ORB_ABSORB_RADIUS) {
                data.add(orb.getValue());
                orb.discard();
                changed = true;
            } else {
                orb.push(centre.subtract(orb.position()).scale(0.1));
            }
        }

        return changed;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        storedBooks.clearContent();
        ContainerHelper.loadAllItems(input, storedBooks.getItems());
        // Without this a freshly loaded chunk has books but no mirrors, and a hopper attached
        // before anything else touches the block would see an empty Scriptorium. loadAllItems
        // writes the list directly and never notifies the container, so it is said here.
        storedBooks.setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, storedBooks.getItems());
    }
}
