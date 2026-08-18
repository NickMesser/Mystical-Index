package net.messer.mystical_index.item.inventory;

import net.messer.util.MysticalUtil;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Book of Farming's six slots, stored in the book's own custom data.
 *
 * <p>Layout is fixed: {@link #SOIL_SLOT} holds the growing medium, {@link #SEED_SLOT} the crop being
 * grown, and the four {@link #OUTPUT_SLOTS} collect what it produces. Soil and seed are never
 * consumed - the book behaves like a botany pot, not like a furnace - so the only slots that ever
 * change on their own are the outputs.
 *
 * <p>Written in the same {@code Items}/{@code Slot} shape every other book inventory here uses, so
 * the compound stays readable by the same tooling and the slot index survives as an int.
 */
public class FarmingBookInventory implements Container {

    public static final int SOIL_SLOT = 0;
    public static final int SEED_SLOT = 1;
    public static final int FIRST_OUTPUT_SLOT = 2;
    public static final int OUTPUT_SLOTS = 4;
    public static final int SIZE = FIRST_OUTPUT_SLOT + OUTPUT_SLOTS;

    private static final String ITEMS_KEY = "FarmItems";
    private static final String SLOT_KEY = "Slot";
    private static final String PLANTED_AT_KEY = "plantedAt";

    public final ItemStack stack;
    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);

    // The component this cache was filled from. Every write through the shared custom-data path
    // replaces the component object, so an identity check is enough to notice that somebody else
    // has touched the book since - which is exactly what happens when the growth cycle deposits a
    // harvest while the player has the screen open. Without this, the menu would keep serving (and
    // then write back) the slot contents from before the deposit and silently undo it.
    private CustomData seen;

    public FarmingBookInventory(ItemStack stack) {
        this.stack = stack;
        reloadIfStale();
    }

    private void reloadIfStale() {
        var current = stack.get(DataComponents.CUSTOM_DATA);
        if (current == seen)
            return;

        seen = current;
        items.clear();
        if (current != null)
            readNbt(current.copyTag());
    }

    // Same shape the other book inventories write: the codec supplies the item's own keys and the
    // slot index rides alongside as an int. ContainerHelper's list helpers are gone in this
    // version - only the ValueInput/ValueOutput forms remain, and those want a block entity's
    // storage context, which an item has no access to.
    private void readNbt(CompoundTag compound) {
        var list = compound.getListOrEmpty(ITEMS_KEY);
        for (int i = 0; i < list.size(); i++) {
            var entry = list.getCompoundOrEmpty(i);
            int slot = entry.getIntOr(SLOT_KEY, -1);
            if (slot < 0 || slot >= SIZE)
                continue;

            items.set(slot, ItemStack.CODEC.parse(ops(), entry).result().orElse(ItemStack.EMPTY));
        }
    }

    private static RegistryOps<Tag> ops() {
        return MysticalUtil.registryLookup().createSerializationContext(NbtOps.INSTANCE);
    }

    // ---- acceptance rules -------------------------------------------------------------------

    /**
     * The growing mediums the book accepts. Deliberately an explicit list rather than "any solid
     * block": the point is soil, and a catch-all would happily let a crop grow on stone. Kept in
     * one place so it can move into config later without touching the slot or the cycle.
     */
    public static boolean isSoil(ItemStack candidate) {
        if (!(candidate.getItem() instanceof BlockItem blockItem))
            return false;

        BlockState soil = blockItem.getBlock().defaultBlockState();
        return soil.is(Blocks.FARMLAND)
                || soil.is(Blocks.DIRT)
                || soil.is(Blocks.GRASS_BLOCK)
                || soil.is(Blocks.COARSE_DIRT)
                || soil.is(Blocks.ROOTED_DIRT)
                || soil.is(Blocks.PODZOL)
                || soil.is(Blocks.MYCELIUM)
                || soil.is(Blocks.MOSS_BLOCK)
                || soil.is(Blocks.SOUL_SAND);
    }

    /**
     * A seed is any item that places a {@link CropBlock}. That is the same test the old bind-by-
     * right-click used, just read off the item instead of the world.
     */
    public static CropBlock cropFor(ItemStack candidate) {
        if (!(candidate.getItem() instanceof BlockItem blockItem))
            return null;

        Block block = blockItem.getBlock();
        return block instanceof CropBlock crop ? crop : null;
    }

    public static boolean isSeed(ItemStack candidate) {
        return cropFor(candidate) != null;
    }

    // ---- planted state ----------------------------------------------------------------------

    public CropBlock plantedCrop() {
        return cropFor(getItem(SEED_SLOT));
    }

    public boolean hasSoil() {
        return !getItem(SOIL_SLOT).isEmpty() && isSoil(getItem(SOIL_SLOT));
    }

    /** A cycle can only run with both a seed and something to grow it in. */
    public boolean isPlanted() {
        return hasSoil() && plantedCrop() != null;
    }

    public long plantedAt() {
        var compound = MysticalUtil.getCustomData(stack);
        return compound == null ? 0L : compound.getLongOr(PLANTED_AT_KEY, 0L);
    }

    public void setPlantedAt(long time) {
        MysticalUtil.editCustomData(stack, compound -> compound.putLong(PLANTED_AT_KEY, time));
    }

    // ---- output handling --------------------------------------------------------------------

    /**
     * Whether every stack of {@code roll} would fit in the outputs as they stand.
     *
     * <p>Asked before anything is produced. A cycle that cannot be fully deposited is not run at
     * all - the book waits, fully grown, until room appears - because the alternative is either
     * voiding the overflow or leaving the crop in a half-harvested state.
     */
    public boolean canAcceptAll(Iterable<ItemStack> roll) {
        var scratch = NonNullList.withSize(OUTPUT_SLOTS, ItemStack.EMPTY);
        for (int i = 0; i < OUTPUT_SLOTS; i++)
            scratch.set(i, getItem(FIRST_OUTPUT_SLOT + i).copy());

        for (ItemStack produced : roll) {
            if (!depositInto(scratch, produced.copy()))
                return false;
        }
        return true;
    }

    /**
     * Whether any output slot could take anything at all - empty, or a stack with headroom.
     *
     * <p>A coarse pre-filter for the growth cycle: it does not promise a given harvest will fit,
     * only that the fully-full case can be rejected without evaluating a loot table.
     */
    public boolean hasAnyOutputRoom() {
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            var slot = getItem(FIRST_OUTPUT_SLOT + i);
            if (slot.isEmpty())
                return true;

            if (slot.getCount() < Math.min(slot.getMaxStackSize(), getMaxStackSize()))
                return true;
        }
        return false;
    }

    public void deposit(Iterable<ItemStack> roll) {
        for (ItemStack produced : roll) {
            var remaining = produced.copy();
            for (int i = 0; i < OUTPUT_SLOTS; i++) {
                var slot = FIRST_OUTPUT_SLOT + i;
                var existing = getItem(slot);
                if (existing.isEmpty()) {
                    items.set(slot, remaining.copy());
                    remaining.setCount(0);
                    break;
                }
                if (ItemStack.isSameItemSameComponents(existing, remaining)) {
                    int room = Math.min(existing.getMaxStackSize(), getMaxStackSize()) - existing.getCount();
                    int moved = Math.min(room, remaining.getCount());
                    existing.grow(moved);
                    remaining.shrink(moved);
                    if (remaining.isEmpty())
                        break;
                }
            }
        }
        setChanged();
    }

    private boolean depositInto(NonNullList<ItemStack> target, ItemStack produced) {
        for (int i = 0; i < target.size(); i++) {
            var existing = target.get(i);
            if (existing.isEmpty()) {
                target.set(i, produced.copy());
                return true;
            }
            if (ItemStack.isSameItemSameComponents(existing, produced)) {
                int room = Math.min(existing.getMaxStackSize(), getMaxStackSize()) - existing.getCount();
                int moved = Math.min(room, produced.getCount());
                existing.grow(moved);
                produced.shrink(moved);
                if (produced.isEmpty())
                    return true;
            }
        }
        return produced.isEmpty();
    }

    // ---- Container --------------------------------------------------------------------------

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
        // Mutators refresh too, not just readers. Relying on a getItem call happening first in the
        // same tick is true of vanilla's click paths today, but that is an unwritten call-order
        // contract and not something to stake the outputs on.
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

        // Goes through the shared write path so the glint and growth-stage components are
        // recomputed from the new contents automatically. Only this one key is touched, so the
        // growth cycle's timing state written on another tick survives untouched.
        MysticalUtil.editCustomData(stack, compound -> compound.put(ITEMS_KEY, list));
        seen = stack.get(DataComponents.CUSTOM_DATA);
    }

    @Override
    public boolean stillValid(Player player) {
        // The book is resolved from the player's hand every time the menu touches it, so the only
        // way this stops being valid is the player putting it away - which the menu checks itself.
        return true;
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }
}
