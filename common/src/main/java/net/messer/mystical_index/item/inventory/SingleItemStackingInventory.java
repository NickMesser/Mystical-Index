package net.messer.mystical_index.item.inventory;
import net.minecraft.core.registries.Registries;
import net.messer.config.ModConfig;
import net.messer.mystical_index.item.custom.base_books.BaseStorageBook;
import net.messer.util.MysticalUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;

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
    public final NonNullList<ItemStack> storedItems;
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
        this.storedItems = NonNullList.withSize(sizeFor(size, storedSlotCount(compound)), ItemStack.EMPTY);
        if (compound != null) {
            readNbt();
        }
    }

    private static int sizeFor(int inventorySize, int storedSlots) {
        return Math.min(MAX_SLOTS, Math.max(inventorySize, storedSlots));
    }

    public void setCurrentlyStoredItem(Item item){
        this.currentlyStoredItem = item;
        this.setChanged();
    }

    public boolean tryRemoveOneItem(){
        for (int i = 0; i < storedItems.size(); i++) {
            if(storedItems.get(i).getItem() != Items.AIR){
                storedItems.get(i).shrink(1);
                if(storedItems.get(i).getCount() == 0){
                    storedItems.set(i, ItemStack.EMPTY);
                }
                this.setChanged();
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
    // one whatever the caller asks for.
    //
    // READ THE PARAMETER NAME: the flag is deliberately IGNORED, not forwarded. That is a load
    // bearing property, not an oversight - it is what lets callers run a blanket "allow new types"
    // pass over every book (LibraryNetwork.insertNewTypes, and the Scriptorium's pass three)
    // knowing a single-type book can never be forced to accept something outside its binding.
    //
    // It is also a trap, so: neither this overload NOR the Boolean bypass below ever sets
    // currentlyStoredItem. Passing true here does NOT bind an unbound book and does NOT make it
    // accept anything. setItem is the only path that binds - see ScriptoriumBlockEntity pass four.
    @Override
    public boolean tryAddStack(ItemStack stack, boolean ignoredAllowNewTypes){
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
            if (ItemStack.isSameItemSameComponents(item, stack)) {
                int combinedCount = item.getCount() + stack.getCount();
                if (combinedCount > this.getMaxStackSize() && item.getCount() < this.getMaxStackSize()) {
                    var remainder = this.getMaxStackSize() - item.getCount();
                    item.grow(remainder);
                    stack.shrink(remainder);
                    this.setChanged();
                    if(stack.getCount() > 0)
                        return tryAddStack(stack, Boolean.TRUE);
                    else
                        return true;
                }

                if(combinedCount <= this.getMaxStackSize()){
                    item.grow(stack.getCount());
                    stack.setCount(0);
                    this.setChanged();
                    return true;
                }
            }
        }

        // Cap every placed stack to the smaller of the book's per-stack limit and the item's own
        // max, and split the input rather than dropping the whole thing into one slot. A count
        // above the max would overflow vanilla's single-byte Count field and be wiped on reload;
        // splitting also lets one oversized insert spill across several empty slots.
        boolean deserialize = false;
        for (int i = 0; i < storedItems.size() && !stack.isEmpty(); i++) {
            if (storedItems.get(i).isEmpty()) {
                int state = Math.min(this.getMaxStackSize(), stack.getMaxStackSize());
                storedItems.set(i, stack.split(state));
                deserialize = true;
            }
        }

        if (deserialize)
            this.setChanged();

        return stack.isEmpty();
    }

    // The registry-aware ops every item read and write goes through. ItemStack lost its encode/
    // fromNbt helpers; the codec is the supported route now, and it produces the same compound
    // shape those helpers did.
    private static RegistryOps<Tag> ops() {
        return MysticalUtil.registryLookup().createSerializationContext(NbtOps.INSTANCE);
    }

    // Same "Items" shape vanilla uses, but the slot index is an int, so a book configured with more
    // than 256 slots no longer wraps slot 256 onto 0 the way ContainerHelper's byte "Slot"
    // tag does.
    public void writeNbt(){
        // The compound fetched from a component is a copy, so the whole thing has to be handed
        // back to the stack once the writes are in.
        CompoundTag nbtData = MysticalUtil.copyCustomData(stack);

        nbtData.putString(STORED_ITEM_KEY, BuiltInRegistries.ITEM.getKey(this.currentlyStoredItem).toString());

        var list = new ListTag();
        for (int slot = 0; slot < storedItems.size(); slot++) {
            var item = storedItems.get(slot);
            if (item.isEmpty())
                continue;

            // The codec writes the item's own keys; the slot index is added afterwards, which
            // leaves exactly the {id, count, components, Slot} shape encode() used to produce.
            var encoded = ItemStack.CODEC.encodeStart(ops(), item).result().orElse(null);
            if (!(encoded instanceof CompoundTag entry))
                continue;

            entry.putInt(SLOT_KEY, slot);
            list.add(entry);
        }

        nbtData.put(ITEMS_KEY, list);
        MysticalUtil.setCustomData(stack, nbtData);
    }

    public void readNbt(){
        CompoundTag compound = MysticalUtil.getCustomData(stack);
        if (compound == null) {
            return;
        }

        readInto(compound, storedItems);
        // getString hands back an Optional now; an absent key has to keep meaning "" so the
        // lookup below still lands on AIR exactly as it did before.
        var itemName = compound.getStringOr(STORED_ITEM_KEY, "");
        currentlyStoredItem = BuiltInRegistries.ITEM.getValue(Identifier.tryParse(itemName));
    }

    private static void readInto(CompoundTag compound, NonNullList<ItemStack> target) {
        // getListOrEmpty replaces the old type-filtered getList: a missing or wrongly typed key
        // yields an empty list, which is the same "read nothing" outcome as before.
        var list = compound.getListOrEmpty(ITEMS_KEY);
        for (int i = 0; i < list.size(); i++) {
            var entry = list.getCompoundOrEmpty(i);
            int slot = readSlot(entry);
            if (slot < 0 || slot >= target.size())
                continue;

            target.set(slot, ItemStack.CODEC.parse(ops(), entry).result().orElse(ItemStack.EMPTY));
        }
    }

    // Books written before the int format stored "Slot" as a byte, and reading a byte tag as an
    // int yields nothing, which would silently pile every old stack into slot 0. Sniff the tag's
    // actual type and fall back to reading it as a byte so those books still load.
    private static int readSlot(CompoundTag entry) {
        var tag = entry.get(SLOT_KEY);
        if (tag == null)
            return -1;

        if (tag.getId() == Tag.TAG_INT)
            return entry.getIntOr(SLOT_KEY, -1);

        // Unsigned: the legacy byte format stored slots 128..255 as negative bytes.
        return entry.getByteOr(SLOT_KEY, (byte) 0) & 255;
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


    @Override
    public int getMaxStackSize() {
        // The book's per-stack cap is the stored item's own max, not a hardcoded 64: storing 64 of
        // a 16-max item (ender pearls, signs, ...) would produce an invalid stack.
        ItemStack first = getFirstItemStack();
        if (!first.isEmpty())
            return first.getMaxStackSize();
        if (currentlyStoredItem != Items.AIR)
            return currentlyStoredItem.getDefaultMaxStackSize();
        return 64;
    }

    @Override
    public int getContainerSize() {
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
    public ItemStack getItem(int slot) {
        return storedItems.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = ContainerHelper.removeItem(storedItems, slot, amount);
        this.setChanged();
        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack removed = ContainerHelper.takeItem(storedItems, slot);
        this.setChanged();
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.storedItems.set(slot, stack);
        // Only a real item redefines what this book stores; clearing a slot must not reset the
        // filter to air, or emptying the book forgets what it was bound to.
        if (!stack.isEmpty()) {
            this.currentlyStoredItem = stack.getItem();
            if (stack.getCount() > this.getMaxStackSize()) {
                stack.setCount(this.getMaxStackSize());
            }
        }

        this.setChanged();
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

}
