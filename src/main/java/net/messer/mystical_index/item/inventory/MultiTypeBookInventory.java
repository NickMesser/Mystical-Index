package net.messer.mystical_index.item.inventory;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.collection.DefaultedList;

import java.util.ArrayList;
import java.util.List;

// A book split into type buckets: each bucket holds one item type across stacksPerType slots, and
// a type that outgrows a bucket claims another one.
public class MultiTypeBookInventory implements BookInventory {

    // Slot indices are written as ints, so serialization no longer caps the size. This is purely a
    // runaway-config guard so a nonsense tier setting cannot allocate an enormous backing list.
    private static final int MAX_SLOTS = 4096;

    private static final String LEGACY_ITEM_KEY = "storedItem";
    private static final String ITEMS_KEY = "Items";
    private static final String SLOT_KEY = "Slot";

    public final ItemStack bookStack;
    public final int types;
    public final int stacksPerType;
    public final DefaultedList<ItemStack> storedItems;

    public MultiTypeBookInventory(ItemStack bookStack, int types, int stacksPerType) {
        this.bookStack = bookStack;
        this.stacksPerType = Math.max(1, Math.min(stacksPerType, MAX_SLOTS));
        this.types = Math.max(1, Math.min(types, MAX_SLOTS));

        var compound = bookStack.hasNbt() ? bookStack.getNbt() : null;

        if (compound != null && compound.contains(LEGACY_ITEM_KEY)) {
            this.storedItems = migrate(compound, this.types, this.stacksPerType);
            writeNbt();
            return;
        }

        this.storedItems = DefaultedList.ofSize(sizeFor(this.types, this.stacksPerType, storedSlotCount(compound)), ItemStack.EMPTY);
        if (compound != null)
            readInto(compound, storedItems);
    }

    // The only place a stored slot index is decoded. Books written before the int format stored
    // "Slot" as a byte, and NbtCompound.getInt returns 0 for a byte tag, which would silently pile
    // every stack of every old book into slot 0.
    private static int readSlot(NbtCompound entry) {
        var tag = entry.get(SLOT_KEY);
        if (tag == null)
            return -1;

        if (tag.getType() == NbtElement.INT_TYPE)
            return entry.getInt(SLOT_KEY);

        return entry.getByte(SLOT_KEY) & 255;
    }

    private static void readInto(NbtCompound compound, DefaultedList<ItemStack> target) {
        var list = compound.getList(ITEMS_KEY, NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            var entry = list.getCompound(i);
            int slot = readSlot(entry);
            if (slot < 0 || slot >= target.size())
                continue;

            target.set(slot, ItemStack.fromNbt(entry));
        }
    }

    private static int clampSlots(int slots, int stacksPerType) {
        if (slots <= MAX_SLOTS)
            return slots;

        return Math.max(stacksPerType, (MAX_SLOTS / stacksPerType) * stacksPerType);
    }

    // Grace buckets from a migration have to survive round-trips, so the backing list is rebuilt
    // to whatever the stored slots need whenever that is larger than the tier allows.
    private static int sizeFor(int types, int stacksPerType, int storedSlots) {
        int buckets = Math.max(types, (storedSlots + stacksPerType - 1) / stacksPerType);
        return clampSlots(buckets * stacksPerType, stacksPerType);
    }

    private static int storedSlotCount(NbtCompound compound) {
        if (compound == null)
            return 0;

        var list = compound.getList(ITEMS_KEY, NbtElement.COMPOUND_TYPE);
        int count = 0;
        for (int i = 0; i < list.size(); i++) {
            int slot = readSlot(list.getCompound(i)) + 1;
            if (slot > count)
                count = slot;
        }

        return count;
    }

    // Books written in the old single-type format are re-laid out into buckets on first read.
    private static DefaultedList<ItemStack> migrate(NbtCompound compound, int types, int stacksPerType) {
        var legacy = DefaultedList.ofSize(Math.max(1, storedSlotCount(compound)), ItemStack.EMPTY);
        readInto(compound, legacy);
        compound.remove(LEGACY_ITEM_KEY);

        var groups = new ArrayList<List<ItemStack>>();
        for (var stack : legacy) {
            if (stack.isEmpty())
                continue;

            List<ItemStack> group = null;
            for (var candidate : groups) {
                if (ItemStack.canCombine(candidate.get(0), stack)) {
                    group = candidate;
                    break;
                }
            }

            if (group == null) {
                group = new ArrayList<>();
                groups.add(group);
            }

            group.add(stack.copy());
        }

        int neededBuckets = 0;
        for (var group : groups)
            neededBuckets += (group.size() + stacksPerType - 1) / stacksPerType;

        // Grace overflow: contents that need more buckets than the tier allows get them anyway,
        // so nothing is voided. Such a book drains normally but takes nothing new until it fits.
        var migrated = DefaultedList.ofSize(clampSlots(Math.max(types, neededBuckets) * stacksPerType, stacksPerType), ItemStack.EMPTY);

        int slot = 0;
        for (var group : groups) {
            for (var stack : group) {
                if (slot < migrated.size())
                    migrated.set(slot, stack);

                slot++;
            }

            while (slot % stacksPerType != 0)
                slot++;
        }

        return migrated;
    }

    // Same "Items" shape vanilla uses, but the slot index is an int so books can exceed 256 slots.
    public void writeNbt() {
        var list = new NbtList();
        for (int slot = 0; slot < storedItems.size(); slot++) {
            var stack = storedItems.get(slot);
            if (stack.isEmpty())
                continue;

            var entry = new NbtCompound();
            entry.putInt(SLOT_KEY, slot);
            stack.writeNbt(entry);
            list.add(entry);
        }

        bookStack.getOrCreateNbt().put(ITEMS_KEY, list);
    }

    public int bucketCount() {
        return storedItems.size() / stacksPerType;
    }

    // Grace buckets do not raise the limit, and a clamped config lowers it.
    public int getTypeCapacity() {
        return Math.min(types, bucketCount());
    }

    private ItemStack bucketVariant(int bucket) {
        int start = bucket * stacksPerType;
        for (int slot = start; slot < start + stacksPerType; slot++) {
            var existing = storedItems.get(slot);
            if (!existing.isEmpty())
                return existing;
        }

        return ItemStack.EMPTY;
    }

    private void fillBucket(int bucket, ItemStack stack) {
        int max = stack.getMaxCount();
        int start = bucket * stacksPerType;

        for (int slot = start; slot < start + stacksPerType && !stack.isEmpty(); slot++) {
            var existing = storedItems.get(slot);

            if (existing.isEmpty()) {
                var placed = stack.copy();
                placed.setCount(Math.min(stack.getCount(), max));
                storedItems.set(slot, placed);
                stack.decrement(placed.getCount());
                continue;
            }

            if (!ItemStack.canCombine(existing, stack) || existing.getCount() >= max)
                continue;

            int room = Math.min(max - existing.getCount(), stack.getCount());
            existing.increment(room);
            stack.decrement(room);
        }
    }

    @Override
    public boolean tryAddStack(ItemStack stack, boolean allowNewTypes) {
        if (stack.isEmpty())
            return true;

        int before = stack.getCount();
        int limit = getTypeCapacity();

        for (int bucket = 0; bucket < limit && !stack.isEmpty(); bucket++) {
            if (ItemStack.canCombine(bucketVariant(bucket), stack))
                fillBucket(bucket, stack);
        }

        if (allowNewTypes) {
            for (int bucket = 0; bucket < limit && !stack.isEmpty(); bucket++) {
                if (bucketVariant(bucket).isEmpty())
                    fillBucket(bucket, stack);
            }
        }

        if (stack.getCount() != before)
            markDirty();

        return stack.isEmpty();
    }

    // Slot grouping and bucket grouping agree here, since a type spilling over several buckets
    // merges into one entry either way.
    public List<BookContentsTooltipData.TypeSummary> getTypeSummaries() {
        return BookContentsTooltipData.fromInventory(this).summaries();
    }

    public int getUsedTypeCount() {
        return getTypeSummaries().size();
    }

    @Override
    public int size() {
        return storedItems.size();
    }

    @Override
    public boolean isEmpty() {
        for (var stack : storedItems) {
            if (!stack.isEmpty())
                return false;
        }

        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        return storedItems.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        var removed = Inventories.splitStack(storedItems, slot, amount);
        markDirty();
        return removed;
    }

    @Override
    public ItemStack removeStack(int slot) {
        var removed = Inventories.removeStack(storedItems, slot);
        markDirty();
        return removed;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        storedItems.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > stack.getMaxCount())
            stack.setCount(stack.getMaxCount());

        markDirty();
    }

    @Override
    public void markDirty() {
        writeNbt();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void clear() {
        storedItems.clear();
    }

    @Override
    public int getMaxCountPerStack() {
        return 64;
    }
}
