package net.messer.mystical_index.item.inventory;

import net.messer.mystical_index.item.custom.BookSling;
import net.messer.util.MysticalUtil;
import net.messer.util.SelfUpdatingBook;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * The five books a Book Sling carries.
 *
 * <p>Stored in the sling's own custom data under its own key, in the same {@code Items}/{@code Slot}
 * shape every other book inventory here uses.
 *
 * <p>The contained books keep ticking while the sling is carried, so this cache invalidates on
 * component identity exactly like the farming inventory - anything holding one of these has to
 * notice a write made underneath it.
 */
public class BookSlingInventory implements Container {

    public static final int SIZE = 5;

    private static final String ITEMS_KEY = "SlingItems";
    private static final String SLOT_KEY = "Slot";

    public final ItemStack stack;
    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private CustomData seen;
    private boolean loaded;

    public BookSlingInventory(ItemStack stack) {
        this.stack = stack;
        reloadIfStale();
    }

    /**
     * What a sling will accept.
     *
     * <p>{@link SelfUpdatingBook} is exactly the set of books whose effect happens passively while
     * carried - it was introduced to mark the books that rewrite themselves on tick, which is the
     * same population. The second clause is what makes nesting structurally impossible: a sling is
     * itself a self-updating book, so without it a sling could hold a sling.
     */
    public static boolean accepts(ItemStack candidate) {
        return MysticalUtil.isEffectBook(candidate);
    }

    private static RegistryOps<Tag> ops() {
        return MysticalUtil.registryLookup().createSerializationContext(NbtOps.INSTANCE);
    }

    private void reloadIfStale() {
        var current = stack.get(DataComponents.CUSTOM_DATA);
        if (loaded && current == seen)
            return;

        seen = current;
        loaded = true;
        items.clear();
        while (items.size() < SIZE)
            items.add(ItemStack.EMPTY);

        var compound = MysticalUtil.getCustomData(stack);
        if (compound == null)
            return;

        var list = compound.getListOrEmpty(ITEMS_KEY);
        for (int i = 0; i < list.size(); i++) {
            var entry = list.getCompoundOrEmpty(i);
            int slot = entry.getIntOr(SLOT_KEY, -1);
            if (slot < 0 || slot >= SIZE)
                continue;

            items.set(slot, ItemStack.CODEC.parse(ops(), entry).result().orElse(ItemStack.EMPTY));
        }
    }

    /**
     * Writes one slot back without disturbing the others.
     *
     * <p>Used by the effect delegation: a contained book that rewrote itself on tick needs its new
     * stack persisted, and only that slot changed. Everything still funnels through the one write
     * below so the sling's own derived components stay correct.
     */
    public void setSlotAndFlush(int slot, ItemStack value) {
        reloadIfStale();
        if (slot < 0 || slot >= SIZE)
            return;

        items.set(slot, value);
        setChanged();
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        reloadIfStale();
        for (ItemStack item : items) {
            if (!item.isEmpty())
                return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        reloadIfStale();
        return slot >= 0 && slot < SIZE ? items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        reloadIfStale();
        var removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty())
            setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        reloadIfStale();
        var removed = ContainerHelper.takeItem(items, slot);
        if (!removed.isEmpty())
            setChanged();
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack value) {
        reloadIfStale();
        if (slot < 0 || slot >= SIZE)
            return;

        items.set(slot, value);
        setChanged();
    }

    @Override
    public void setChanged() {
        var list = new ListTag();
        for (int slot = 0; slot < items.size(); slot++) {
            var item = items.get(slot);
            if (item.isEmpty())
                continue;

            var encoded = ItemStack.CODEC.encodeStart(ops(), item).result().orElse(null);
            if (!(encoded instanceof CompoundTag entry))
                continue;

            entry.putInt(SLOT_KEY, slot);
            list.add(entry);
        }

        // One key, through the shared path, so the sling's glint stays right and the write rides
        // the reequip suppression it already has as a SelfUpdatingBook.
        MysticalUtil.editCustomData(stack, compound -> compound.put(ITEMS_KEY, list));
        seen = stack.get(DataComponents.CUSTOM_DATA);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        items.clear();
        while (items.size() < SIZE)
            items.add(ItemStack.EMPTY);
        setChanged();
    }
}
