package net.messer.mystical_index.item.inventory;
import net.messer.config.ModConfig;
import net.messer.mystical_index.item.custom.base_books.BaseStorageBook;
import net.messer.util.MysticalUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;

import java.util.Iterator;

public class SingleItemStackingInventory implements BookInventory {
    // Slot indices are written as ints, so serialization no longer caps the size at 256. This is a
    // runaway-config guard so a nonsense size cannot allocate an enormous backing list.
    private static final int MAX_SLOTS = 4096;

    private static final String ITEMS_KEY = "Items";
    private static final String SLOT_KEY = "Slot";
    private static final String STORED_ITEM_KEY = "storedItem";

    public final ItemStack stack;
    public final int inventorySize;
    public final DefaultedList<ItemStack> storedItems;
    public Item currentlyStoredItem;

    public int maxStacks = 1;

    public SingleItemStackingInventory(ItemStack stack , int size){
        this.stack = stack;
        this.inventorySize = size;
        this.currentlyStoredItem = Items.AIR;
        this.maxStacks = ModConfig.StorageBookMaxStacks;

        // Grow the backing list to whatever the stored slots need when that is larger than the
        // configured size, so lowering the config drains those slots gracefully instead of
        // truncating (and deleting) them on the next write.
        var compound = MysticalUtil.getCustomData(stack);
        this.storedItems = DefaultedList.ofSize(sizeFor(size, storedSlotCount(compound)), ItemStack.EMPTY);
        if (compound != null) {
            readNbt();
        }
    }

    private static int sizeFor(int inventorySize, int storedSlots) {
        return Math.min(MAX_SLOTS, Math.max(inventorySize, storedSlots));
    }

    public void setCurrentlyStoredItem(Item item){
        this.currentlyStoredItem = item;
        this.markDirty();
    }

    public boolean tryRemoveOneItem(){
        for (int i = 0; i < storedItems.size(); i++) {
            if(storedItems.get(i).getItem() != Items.AIR){
                storedItems.get(i).decrement(1);
                if(storedItems.get(i).getCount() == 0){
                    storedItems.set(i, ItemStack.EMPTY);
                }
                this.markDirty();
                return true;
            }
        }
        return false;
    }

    public int getCountOfStoredItem(){
        int count = 0;
        for (ItemStack item: storedItems) {
            if(item.getItem() != Items.AIR)
                count += item.getCount();
        }
        return count;
    }

    public ItemStack getFirstItemStack(){
        for (int i = 0; i < storedItems.size(); i++) {
            if(storedItems.get(i).getItem() != Items.AIR){
                return storedItems.get(i);
            }
        }
        return ItemStack.EMPTY;
    }


    // Network-facing insert. A bound book is a single type by definition, so it never takes a new
    // one whatever the caller asks for. The Boolean overload below is the internal bypass.
    @Override
    public boolean tryAddStack(ItemStack stack, boolean allowNewTypes){
        return tryAddStack(stack, Boolean.FALSE);
    }

    public boolean tryAddStack(ItemStack stack, Boolean bypassItemCheck){
        // No storage book may be stored inside a storage book. Without this, books nest and their
        // data grows ~40^depth until the packet is too large and the player is disconnected.
        if (stack.getItem() instanceof BaseStorageBook)
            return false;

        if(stack.getItem() != currentlyStoredItem && !bypassItemCheck)
            return false;

        for (ItemStack item: storedItems) {
            if (ItemStack.areItemsAndComponentsEqual(item, stack)) {
                int combinedCount = item.getCount() + stack.getCount();
                if (combinedCount > this.getMaxCountPerStack() && item.getCount() < this.getMaxCountPerStack()) {
                    var remainder = this.getMaxCountPerStack() - item.getCount();
                    item.increment(remainder);
                    stack.decrement(remainder);
                    this.markDirty();
                    if(stack.getCount() > 0)
                        return tryAddStack(stack, Boolean.TRUE);
                    else
                        return true;
                }

                if(combinedCount <= this.getMaxCountPerStack()){
                    item.increment(stack.getCount());
                    stack.setCount(0);
                    this.markDirty();
                    return true;
                }
            }
        }

        // Cap every placed stack to the smaller of the book's per-stack limit and the item's own
        // max, and split the input rather than dropping the whole thing into one slot. A count
        // above the max would overflow vanilla's single-byte Count field and be wiped on reload;
        // splitting also lets one oversized insert spill across several empty slots.
        boolean changed = false;
        for (int i = 0; i < storedItems.size() && !stack.isEmpty(); i++) {
            if (storedItems.get(i).isEmpty()) {
                int cap = Math.min(this.getMaxCountPerStack(), stack.getMaxCount());
                storedItems.set(i, stack.split(cap));
                changed = true;
            }
        }

        if (changed)
            this.markDirty();

        return stack.isEmpty();
    }

    // Same "Items" shape vanilla uses, but the slot index is an int, so a book configured with more
    // than 256 slots no longer wraps slot 256 onto 0 the way Inventories.writeNbt's byte "Slot"
    // tag does.
    public void writeNbt(){
        // The compound fetched from a component is a copy, so the whole thing has to be handed
        // back to the stack once the writes are in.
        NbtCompound nbtData = MysticalUtil.copyCustomData(stack);

        nbtData.putString(STORED_ITEM_KEY, Registries.ITEM.getId(this.currentlyStoredItem).toString());

        var list = new NbtList();
        for (int slot = 0; slot < storedItems.size(); slot++) {
            var item = storedItems.get(slot);
            if (item.isEmpty())
                continue;

            var entry = new NbtCompound();
            entry.putInt(SLOT_KEY, slot);
            // encode() merges into a copy of the prefix and returns it; the prefix itself is left
            // untouched, so the returned compound is the only one carrying the item data.
            list.add(item.encode(MysticalUtil.registryLookup(), entry));
        }

        nbtData.put(ITEMS_KEY, list);
        MysticalUtil.setCustomData(stack, nbtData);
    }

    public void readNbt(){
        NbtCompound compound = MysticalUtil.getCustomData(stack);
        if (compound == null) {
            return;
        }

        readInto(compound, storedItems);
        var itemName = compound.getString(STORED_ITEM_KEY);
        currentlyStoredItem = Registries.ITEM.get(Identifier.tryParse(itemName));
    }

    private static void readInto(NbtCompound compound, DefaultedList<ItemStack> target) {
        var list = compound.getList(ITEMS_KEY, NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            var entry = list.getCompound(i);
            int slot = readSlot(entry);
            if (slot < 0 || slot >= target.size())
                continue;

            target.set(slot, ItemStack.fromNbt(MysticalUtil.registryLookup(), entry).orElse(ItemStack.EMPTY));
        }
    }

    // Books written before the int format stored "Slot" as a byte, and NbtCompound.getInt returns
    // 0 for a byte tag, which would silently pile every old stack into slot 0. Fall back to reading
    // it as a byte so those books still load.
    private static int readSlot(NbtCompound entry) {
        var tag = entry.get(SLOT_KEY);
        if (tag == null)
            return -1;

        if (tag.getType() == NbtElement.INT_TYPE)
            return entry.getInt(SLOT_KEY);

        return entry.getByte(SLOT_KEY) & 255;
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


    @Override
    public int getMaxCountPerStack() {
        // The book's per-stack cap is the stored item's own max, not a hardcoded 64: storing 64 of
        // a 16-max item (ender pearls, signs, ...) would produce an invalid stack.
        ItemStack first = getFirstItemStack();
        if (!first.isEmpty())
            return first.getMaxCount();
        if (currentlyStoredItem != Items.AIR)
            return currentlyStoredItem.getMaxCount();
        return 64;
    }

    @Override
    public int size() {
        // The backing list, not the configured size: when a lowered config left more stored slots
        // than it allows, those extra slots must stay visible so they can drain instead of being
        // hidden (and effectively lost) behind a smaller reported size.
        return storedItems.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : storedItems){
            if(stack.getItem() != Items.AIR || !stack.isEmpty()){
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        return storedItems.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack stack = Inventories.splitStack(storedItems, slot, amount);
        this.markDirty();
        return stack;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack removed = Inventories.removeStack(storedItems, slot);
        this.markDirty();
        return removed;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        this.storedItems.set(slot, stack);
        // Only a real item redefines what this book stores; clearing a slot must not reset the
        // filter to air, or emptying the book forgets what it was bound to.
        if (!stack.isEmpty()) {
            this.currentlyStoredItem = stack.getItem();
            if (stack.getCount() > this.getMaxCountPerStack()) {
                stack.setCount(this.getMaxCountPerStack());
            }
        }

        this.markDirty();
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

}
