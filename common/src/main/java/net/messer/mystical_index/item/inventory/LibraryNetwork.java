package net.messer.mystical_index.item.inventory;

import net.minecraft.core.registries.Registries;
import net.messer.mystical_index.block.entity.LibraryBlockEntity;
import net.messer.mystical_index.item.custom.FarmingBook;
import net.messer.mystical_index.item.custom.base_books.BaseStorageBook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LibraryNetwork {

    public record Entry(BookItemVariant variant, long count) {}

    /** How full the insertable side of the network is, in type slots. */
    public record Capacity(int used, int total, int books, int libraries) {
        public static final Capacity EMPTY = new Capacity(0, 0, 0, 0);
    }

    /**
     * Type-slot occupancy across the books the network can actually insert into.
     *
     * <p>Farming books are excluded by the same guard the insert passes use, and for the same
     * reason: their slots are produce-only, so counting them would advertise capacity that no
     * deposit can ever reach. Their legacy leftovers are excluded with them - visible and
     * drainable, but not somewhere anything can be put.
     *
     * <p>A multi-type book reports its own type slots; every other storage book is one type slot,
     * used when it holds anything.
     */
    public static Capacity capacity(List<LibraryBlockEntity> libraries) {
        int used = 0, total = 0, books = 0;

        for (var library : libraries) {
            for (var bookStack : library.storedBooks.getItems()) {
                if (!(bookStack.getItem() instanceof BaseStorageBook storageBook))
                    continue;
                if (bookStack.getItem() instanceof FarmingBook)
                    continue;

                books++;
                var inventory = storageBook.getInventory(bookStack);
                if (inventory instanceof MultiTypeBookInventory multi) {
                    used += multi.getUsedTypeCount();
                    total += multi.getTypeCapacity();
                } else {
                    total += 1;
                    if (!inventory.isEmpty())
                        used += 1;
                }
            }
        }

        return new Capacity(used, total, books, libraries.size());
    }

    /**
     * The farming outputs a book contributes to the network, or null if it is not a farming book.
     *
     * <p>Only the four output slots are ever exposed. Soil and seed are deliberately invisible: a
     * lectern pull that took the seed would silently end a growth cycle, and the player would have
     * no way to see why their book stopped.
     *
     * <p>Returned as the inventory itself so every network write goes through
     * {@link FarmingBookInventory#removeItem}, which persists via the shared custom-data path -
     * that is what keeps the growth float, the glint and the inventory's own staleness check
     * correct. Network code never touches the compound directly.
     */
    private static FarmingBookInventory farmingOutputs(ItemStack bookStack) {
        return bookStack.getItem() instanceof FarmingBook ? new FarmingBookInventory(bookStack) : null;
    }

    private static int outputSlot(int i) {
        return FarmingBookInventory.FIRST_OUTPUT_SLOT + i;
    }


    /**
     * Marks a library changed after the network mutated the CONTENTS of its books.
     *
     * <p>BlockEntity.setChanged only flags the chunk for saving. The transfer-layer mirrors and the
     * hopper view are rebuilt by storedBooks.setChanged, and until book ticking was removed the
     * Library happened to call that every tick, which quietly covered every path here. With the
     * tick gone each mutation has to say so itself or a hopper serves contents from before the
     * lectern touched them.
     */
    private static void markLibraryChanged(LibraryBlockEntity library) {
        library.storedBooks.setChanged();
        library.setChanged();
    }

    public static List<LibraryBlockEntity> findLibraries(Level world, BlockPos center, int range) {
        var libraries = new ArrayList<LibraryBlockEntity>();

        int minChunkX = (center.getX() - range) >> 4;
        int maxChunkX = (center.getX() + range) >> 4;
        int minChunkZ = (center.getZ() - range) >> 4;
        int maxChunkZ = (center.getZ() + range) >> 4;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                // create = false: an unloaded chunk simply has no libraries in it. Forcing it to
                // load would let a lectern keep chunks alive just by sitting there.
                var chunk = world.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
                if (!(chunk instanceof LevelChunk worldChunk))
                    continue;

                for (var blockEntity : worldChunk.getBlockEntities().values()) {
                    // Type-based on purpose: Scriptoriums are deliberately NOT part of the lectern
                    // network. They are where books work; the Library is where books are stored,
                    // and only the Library's contents are browsable from a lectern.
                    if (!(blockEntity instanceof LibraryBlockEntity library) || library.isRemoved())
                        continue;

                    var pos = library.getBlockPos();
                    if (Math.abs(pos.getX() - center.getX()) <= range
                            && Math.abs(pos.getY() - center.getY()) <= range
                            && Math.abs(pos.getZ() - center.getZ()) <= range) {
                        libraries.add(library);
                    }
                }
            }
        }

        return libraries;
    }

    public static List<Entry> aggregate(List<LibraryBlockEntity> libraries) {
        Map<BookItemVariant, Long> totals = new HashMap<>();

        for (var library : libraries) {
            for (var bookStack : library.storedBooks.getItems()) {
                if (!(bookStack.getItem() instanceof BaseStorageBook storageBook))
                    continue;

                var inventory = storageBook.getInventory(bookStack);
                for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                    var stored = inventory.getItem(slot);
                    if (stored.isEmpty())
                        continue;

                    totals.merge(BookItemVariant.of(stored), (long) stored.getCount(), Long::sum);
                }

                var farming = farmingOutputs(bookStack);
                if (farming == null)
                    continue;

                for (int i = 0; i < FarmingBookInventory.OUTPUT_SLOTS; i++) {
                    var produced = farming.getItem(outputSlot(i));
                    if (produced.isEmpty())
                        continue;

                    totals.merge(BookItemVariant.of(produced), (long) produced.getCount(), Long::sum);
                }
            }
        }

        var entries = new ArrayList<Entry>(totals.size());
        totals.forEach((variant, count) -> entries.add(new Entry(variant, count)));
        entries.sort(Comparator
                .comparing((Entry entry) -> BuiltInRegistries.ITEM.getKey(entry.variant().getItem()).toString())
                .thenComparingInt(entry -> entry.variant().getComponents().hashCode()));
        return entries;
    }

    public static long count(List<LibraryBlockEntity> libraries, BookItemVariant variant) {
        long total = 0;

        for (var library : libraries) {
            for (var bookStack : library.storedBooks.getItems()) {
                if (!(bookStack.getItem() instanceof BaseStorageBook storageBook))
                    continue;

                var inventory = storageBook.getInventory(bookStack);
                for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                    var stored = inventory.getItem(slot);
                    if (!stored.isEmpty() && variant.matches(stored))
                        total += stored.getCount();
                }

                var farming = farmingOutputs(bookStack);
                if (farming == null)
                    continue;

                for (int i = 0; i < FarmingBookInventory.OUTPUT_SLOTS; i++) {
                    var produced = farming.getItem(outputSlot(i));
                    if (!produced.isEmpty() && variant.matches(produced))
                        total += produced.getCount();
                }
            }
        }

        return total;
    }

    public static long extract(List<LibraryBlockEntity> libraries, BookItemVariant variant, long amount) {
        long extracted = 0;

        for (var library : libraries) {
            if (extracted >= amount)
                break;

            boolean changed = false;
            for (var bookStack : library.storedBooks.getItems()) {
                if (extracted >= amount)
                    break;
                if (!(bookStack.getItem() instanceof BaseStorageBook storageBook))
                    continue;

                var inventory = storageBook.getInventory(bookStack);
                for (int slot = 0; slot < inventory.getContainerSize() && extracted < amount; slot++) {
                    var stored = inventory.getItem(slot);
                    if (stored.isEmpty() || !variant.matches(stored))
                        continue;

                    int take = (int) Math.min(amount - extracted, stored.getCount());
                    var removed = inventory.removeItem(slot, take);
                    if (removed.isEmpty())
                        continue;

                    extracted += removed.getCount();
                    changed = true;
                }

                var farming = farmingOutputs(bookStack);
                if (farming == null)
                    continue;

                for (int i = 0; i < FarmingBookInventory.OUTPUT_SLOTS && extracted < amount; i++) {
                    int slot = outputSlot(i);
                    var produced = farming.getItem(slot);
                    if (produced.isEmpty() || !variant.matches(produced))
                        continue;

                    int take = (int) Math.min(amount - extracted, produced.getCount());
                    var removed = farming.removeItem(slot, take);
                    if (removed.isEmpty())
                        continue;

                    extracted += removed.getCount();
                    changed = true;
                }
            }

            if (changed)
                markLibraryChanged(library);
        }

        return extracted;
    }

    // Pass 1: top up the books that already hold this type.
    private static boolean insertExisting(List<ItemStack> bookStacks, ItemStack stack) {
        boolean changed = false;

        for (var bookStack : bookStacks) {
            if (stack.isEmpty())
                break;
            if (!(bookStack.getItem() instanceof BaseStorageBook storageBook))
                continue;
            // Farming books are produce-only. The guard lives here, in the enumeration, rather
            // than as a check inside some write path: a book the insert loop never sees cannot be
            // filled by any caller, present or future - player deposits, crafting-grid returns and
            // the loader transfer layers all funnel through these two passes.
            if (bookStack.getItem() instanceof FarmingBook)
                continue;

            int before = stack.getCount();
            // The boolean means "fully absorbed", not "took something", so the count delta is
            // the only honest measure of whether this book accepted anything.
            storageBook.getInventory(bookStack).tryAddStack(stack, false);
            if (stack.getCount() != before)
                changed = true;
        }

        return changed;
    }

    // Pass 2: a book with a free type slot claims whatever pass 1 could not place. Only a Book of
    // Holding ever says yes here; a bound book ignores the flag and stays single-type.
    private static boolean insertNewTypes(List<ItemStack> bookStacks, ItemStack stack) {
        boolean changed = false;

        for (var bookStack : bookStacks) {
            if (stack.isEmpty())
                break;
            if (!(bookStack.getItem() instanceof BaseStorageBook storageBook))
                continue;
            if (bookStack.getItem() instanceof FarmingBook)
                continue;

            int before = stack.getCount();
            storageBook.getInventory(bookStack).tryAddStack(stack, true);
            if (stack.getCount() != before)
                changed = true;
        }

        return changed;
    }

    /**
     * The two-pass fill against a bare list of book stacks, with no block entity attached.
     *
     * <p>Split out so a simulated insert can run the very same rules over throwaway copies of the
     * books instead of maintaining a second, parallel "how much would fit" implementation.
     */
    public static long insertIntoBooks(List<ItemStack> bookStacks, ItemStack stack) {
        int originalCount = stack.getCount();

        insertExisting(bookStacks, stack);
        if (!stack.isEmpty())
            insertNewTypes(bookStacks, stack);

        return originalCount - stack.getCount();
    }

    /**
     * Pass one only - top up the books that already hold this type, and never open a fresh slot.
     *
     * <p>The "deposit what this Library already stocks" rule, for the Book of Transport's held
     * gesture. A separate entry point rather than a boolean on
     * {@link #insertIntoLibrary} because the two are different promises: one fills whatever it can,
     * the other guarantees it will not claim new storage, which is what makes a sweep of the whole
     * player inventory safe to fire blind.
     */
    public static long insertExistingIntoLibrary(LibraryBlockEntity library, ItemStack stack) {
        int originalCount = stack.getCount();

        if (insertExisting(library.storedBooks.getItems(), stack))
            markLibraryChanged(library);

        return originalCount - stack.getCount();
    }

    public static long insertIntoLibrary(LibraryBlockEntity library, ItemStack stack) {
        var bookStacks = library.storedBooks.getItems();
        int originalCount = stack.getCount();

        boolean changed = insertExisting(bookStacks, stack);
        if (!stack.isEmpty() && insertNewTypes(bookStacks, stack))
            changed = true;

        if (changed)
            markLibraryChanged(library);

        return originalCount - stack.getCount();
    }

    public static long insert(List<LibraryBlockEntity> libraries, ItemStack stack) {
        int originalCount = stack.getCount();
        var changed = new ArrayList<LibraryBlockEntity>();

        // Every book already holding the type fills before any book opens a fresh type slot,
        // otherwise library #1 burns a slot while library #2 still had room in an existing one.
        for (var library : libraries) {
            if (stack.isEmpty())
                break;
            if (insertExisting(library.storedBooks.getItems(), stack))
                changed.add(library);
        }

        for (var library : libraries) {
            if (stack.isEmpty())
                break;
            if (insertNewTypes(library.storedBooks.getItems(), stack) && !changed.contains(library))
                changed.add(library);
        }

        for (var library : changed)
            markLibraryChanged(library);

        return originalCount - stack.getCount();
    }
}
