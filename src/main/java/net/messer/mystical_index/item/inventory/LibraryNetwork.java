package net.messer.mystical_index.item.inventory;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.messer.mystical_index.block.entity.LibraryBlockEntity;
import net.messer.mystical_index.item.custom.base_books.BaseStorageBook;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LibraryNetwork {

    public record Entry(ItemVariant variant, long count) {}

    public static List<LibraryBlockEntity> findLibraries(World world, BlockPos center, int range) {
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
                if (!(chunk instanceof WorldChunk worldChunk))
                    continue;

                for (var blockEntity : worldChunk.getBlockEntities().values()) {
                    if (!(blockEntity instanceof LibraryBlockEntity library) || library.isRemoved())
                        continue;

                    var pos = library.getPos();
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
        Map<ItemVariant, Long> totals = new HashMap<>();

        for (var library : libraries) {
            for (var bookStack : library.storedBooks.heldStacks) {
                if (!(bookStack.getItem() instanceof BaseStorageBook storageBook))
                    continue;

                var inventory = storageBook.getInventory(bookStack);
                for (int slot = 0; slot < inventory.size(); slot++) {
                    var stored = inventory.getStack(slot);
                    if (stored.isEmpty())
                        continue;

                    totals.merge(ItemVariant.of(stored), (long) stored.getCount(), Long::sum);
                }
            }
        }

        var entries = new ArrayList<Entry>(totals.size());
        totals.forEach((variant, count) -> entries.add(new Entry(variant, count)));
        entries.sort(Comparator
                .comparing((Entry entry) -> Registries.ITEM.getId(entry.variant().getItem()).toString())
                .thenComparingInt(entry -> entry.variant().getComponents().hashCode()));
        return entries;
    }

    public static long count(List<LibraryBlockEntity> libraries, ItemVariant variant) {
        long total = 0;

        for (var library : libraries) {
            for (var bookStack : library.storedBooks.heldStacks) {
                if (!(bookStack.getItem() instanceof BaseStorageBook storageBook))
                    continue;

                var inventory = storageBook.getInventory(bookStack);
                for (int slot = 0; slot < inventory.size(); slot++) {
                    var stored = inventory.getStack(slot);
                    if (!stored.isEmpty() && variant.matches(stored))
                        total += stored.getCount();
                }
            }
        }

        return total;
    }

    public static long extract(List<LibraryBlockEntity> libraries, ItemVariant variant, long amount) {
        long extracted = 0;

        for (var library : libraries) {
            if (extracted >= amount)
                break;

            boolean changed = false;
            for (var bookStack : library.storedBooks.heldStacks) {
                if (extracted >= amount)
                    break;
                if (!(bookStack.getItem() instanceof BaseStorageBook storageBook))
                    continue;

                var inventory = storageBook.getInventory(bookStack);
                for (int slot = 0; slot < inventory.size() && extracted < amount; slot++) {
                    var stored = inventory.getStack(slot);
                    if (stored.isEmpty() || !variant.matches(stored))
                        continue;

                    int take = (int) Math.min(amount - extracted, stored.getCount());
                    var removed = inventory.removeStack(slot, take);
                    if (removed.isEmpty())
                        continue;

                    extracted += removed.getCount();
                    changed = true;
                }
            }

            if (changed)
                library.markDirty();
        }

        return extracted;
    }

    // Pass 1: top up the books that already hold this type.
    private static boolean insertExisting(LibraryBlockEntity library, ItemStack stack) {
        boolean changed = false;

        for (var bookStack : library.storedBooks.heldStacks) {
            if (stack.isEmpty())
                break;
            if (!(bookStack.getItem() instanceof BaseStorageBook storageBook))
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
    private static boolean insertNewTypes(LibraryBlockEntity library, ItemStack stack) {
        boolean changed = false;

        for (var bookStack : library.storedBooks.heldStacks) {
            if (stack.isEmpty())
                break;
            if (!(bookStack.getItem() instanceof BaseStorageBook storageBook))
                continue;

            int before = stack.getCount();
            storageBook.getInventory(bookStack).tryAddStack(stack, true);
            if (stack.getCount() != before)
                changed = true;
        }

        return changed;
    }

    public static long insertIntoLibrary(LibraryBlockEntity library, ItemStack stack) {
        int originalCount = stack.getCount();

        boolean changed = insertExisting(library, stack);
        if (!stack.isEmpty() && insertNewTypes(library, stack))
            changed = true;

        if (changed)
            library.markDirty();

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
            if (insertExisting(library, stack))
                changed.add(library);
        }

        for (var library : libraries) {
            if (stack.isEmpty())
                break;
            if (insertNewTypes(library, stack) && !changed.contains(library))
                changed.add(library);
        }

        for (var library : changed)
            library.markDirty();

        return originalCount - stack.getCount();
    }
}
