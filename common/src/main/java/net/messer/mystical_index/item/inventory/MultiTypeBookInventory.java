package net.messer.mystical_index.item.inventory;

import net.messer.mystical_index.item.custom.base_books.BaseStorageBook;
import net.messer.util.MysticalUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;

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
    public final NonNullList<ItemStack> storedItems;

    public MultiTypeBookInventory(ItemStack bookStack, int types, int stacksPerType) {
        this.bookStack = bookStack;
        this.stacksPerType = Math.max(1, Math.min(stacksPerType, MAX_SLOTS));
        this.types = Math.max(1, Math.min(types, MAX_SLOTS));

        var compound = MysticalUtil.getCustomData(bookStack);

        if (compound != null && compound.contains(LEGACY_ITEM_KEY)) {
            this.storedItems = migrate(compound, this.types, this.stacksPerType);
            // migrate() strips the legacy key from this copy, so the very same copy has to be the
            // one that goes back onto the stack or the book would migrate again on every read.
            writeNbt(compound);
            return;
        }

        var loaded = NonNullList.withSize(sizeFor(this.types, this.stacksPerType, storedSlotCount(compound)), ItemStack.EMPTY);
        if (compound != null)
            readInto(compound, loaded);

        // A tier upgrade re-reads the same flat slot list at a larger stacksPerType, so two types
        // that used to be adjacent single-slot buckets now share one bucket. bucketVariant only
        // reports the first, silently orphaning the second. When any bucket holds more than one
        // variant, repack the whole book by item type so every bucket is single-variant again and
        // write the fixed layout back. A book that is already correct is left untouched.
        if (hasMultiVariantBucket(loaded, this.stacksPerType)) {
            this.storedItems = repack(loaded, this.types, this.stacksPerType);
            writeNbt();
        } else {
            this.storedItems = loaded;
        }
    }

    private static boolean hasMultiVariantBucket(NonNullList<ItemStack> items, int stacksPerType) {
        for (int start = 0; start < items.size(); start += stacksPerType) {
            ItemStack variant = ItemStack.EMPTY;
            for (int slot = start; slot < start + stacksPerType && slot < items.size(); slot++) {
                var existing = items.get(slot);
                if (existing.isEmpty())
                    continue;

                if (variant.isEmpty())
                    variant = existing;
                else if (!ItemStack.isSameItemSameComponents(variant, existing))
                    return true;
            }
        }

        return false;
    }

    // ItemStack lost its encode/fromNbt helpers; the codec is the supported route and
    // produces the same compound shape they did.
    private static RegistryOps<Tag> ops() {
        return MysticalUtil.registryLookup().createSerializationContext(NbtOps.INSTANCE);
    }

    // The only place a stored slot index is decoded. Books written before the int format stored
    // "Slot" as a byte, and reading a byte tag as an int yields nothing, which would silently pile
    // every stack of every old book into slot 0.
    private static int readSlot(CompoundTag entry) {
        var tag = entry.get(SLOT_KEY);
        if (tag == null)
            return -1;

        if (tag.getId() == Tag.TAG_INT)
            return entry.getIntOr(SLOT_KEY, -1);

        // Unsigned: the legacy byte format stored slots 128..255 as negative bytes.
        return entry.getByteOr(SLOT_KEY, (byte) 0) & 255;
    }

    private static void readInto(CompoundTag compound, NonNullList<ItemStack> target) {
        var list = compound.getListOrEmpty(ITEMS_KEY);
        for (int i = 0; i < list.size(); i++) {
            var entry = list.getCompoundOrEmpty(i);
            int slot = readSlot(entry);
            if (slot < 0 || slot >= target.size())
                continue;

            target.set(slot, ItemStack.CODEC.parse(ops(), entry).result().orElse(ItemStack.EMPTY));
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

    private static int storedSlotCount(CompoundTag compound) {
        if (compound == null)
            return 0;

        var list = compound.getListOrEmpty(ITEMS_KEY);
        int count = 0;
        for (int i = 0; i < list.size(); i++) {
            int slot = readSlot(list.getCompoundOrEmpty(i)) + 1;
            if (slot > count)
                count = slot;
        }

        return count;
    }

    // Books written in the old single-type format are re-laid out into buckets on first read.
    private static NonNullList<ItemStack> migrate(CompoundTag compound, int types, int stacksPerType) {
        var legacy = NonNullList.withSize(Math.max(1, storedSlotCount(compound)), ItemStack.EMPTY);
        readInto(compound, legacy);
        compound.remove(LEGACY_ITEM_KEY);

        return repack(legacy, types, stacksPerType);
    }

    // Groups the contents by item type (areItemsAndComponentsEqual) and lays each group out bucket
    // by bucket, one type per bucket, padding to the bucket boundary between types. Grace overflow
    // is preserved: contents needing more buckets than the tier allows get them anyway, so nothing
    // is voided; such a book drains normally but takes nothing new until it fits.
    private static NonNullList<ItemStack> repack(List<ItemStack> contents, int types, int stacksPerType) {
        var groups = new ArrayList<List<ItemStack>>();
        for (var stack : contents) {
            if (stack.isEmpty())
                continue;

            List<ItemStack> group = null;
            for (var candidate : groups) {
                if (ItemStack.isSameItemSameComponents(candidate.get(0), stack)) {
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

        var repacked = NonNullList.withSize(clampSlots(Math.max(types, neededBuckets) * stacksPerType, stacksPerType), ItemStack.EMPTY);

        int slot = 0;
        for (var group : groups) {
            for (var stack : group) {
                if (slot < repacked.size())
                    repacked.set(slot, stack);

                slot++;
            }

            while (slot % stacksPerType != 0)
                slot++;
        }

        return repacked;
    }

    // Same "Items" shape vanilla uses, but the slot index is an int so books can exceed 256 slots.
    public void writeNbt() {
        writeNbt(MysticalUtil.copyCustomData(bookStack));
    }

    private void writeNbt(CompoundTag compound) {
        var list = new ListTag();
        for (int slot = 0; slot < storedItems.size(); slot++) {
            var stack = storedItems.get(slot);
            if (stack.isEmpty())
                continue;

            // The codec writes the item's own keys; the slot index is added afterwards, which
            // leaves exactly the {id, count, components, Slot} shape encode() used to produce.
            var encoded = ItemStack.CODEC.encodeStart(ops(), stack).result().orElse(null);
            if (!(encoded instanceof CompoundTag entry))
                continue;

            entry.putInt(SLOT_KEY, slot);
            list.add(entry);
        }

        compound.put(ITEMS_KEY, list);
        MysticalUtil.setCustomData(bookStack, compound);
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
        int max = stack.getMaxStackSize();
        int start = bucket * stacksPerType;

        for (int slot = start; slot < start + stacksPerType && !stack.isEmpty(); slot++) {
            var existing = storedItems.get(slot);

            if (existing.isEmpty()) {
                var placed = stack.copy();
                placed.setCount(Math.min(stack.getCount(), max));
                storedItems.set(slot, placed);
                stack.shrink(placed.getCount());
                continue;
            }

            if (!ItemStack.isSameItemSameComponents(existing, stack) || existing.getCount() >= max)
                continue;

            int room = Math.min(max - existing.getCount(), stack.getCount());
            existing.grow(room);
            stack.shrink(room);
        }
    }

    @Override
    public boolean tryAddStack(ItemStack stack, boolean allowNewTypes) {
        if (stack.isEmpty())
            return true;

        // No storage book may be stored inside a storage book. Nested books multiply their stored
        // data ~40^depth until the packet is too large and the player is disconnected.
        if (stack.getItem() instanceof BaseStorageBook)
            return false;

        int parents = stack.getCount();
        int limit = getTypeCapacity();

        for (int bucket = 0; bucket < limit && !stack.isEmpty(); bucket++) {
            if (ItemStack.isSameItemSameComponents(bucketVariant(bucket), stack))
                fillBucket(bucket, stack);
        }

        if (allowNewTypes) {
            for (int bucket = 0; bucket < limit && !stack.isEmpty(); bucket++) {
                if (bucketVariant(bucket).isEmpty())
                    fillBucket(bucket, stack);
            }
        }

        if (stack.getCount() != parents)
            setChanged();

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
    public int getContainerSize() {
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
    public ItemStack getItem(int slot) {
        return storedItems.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        var removed = ContainerHelper.removeItem(storedItems, slot, amount);
        setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        var removed = ContainerHelper.takeItem(storedItems, slot);
        setChanged();
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        storedItems.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > stack.getMaxStackSize())
            stack.setCount(stack.getMaxStackSize());

        setChanged();
    }

    @Override
    public void setChanged() {
        writeNbt();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        storedItems.clear();
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }
}
