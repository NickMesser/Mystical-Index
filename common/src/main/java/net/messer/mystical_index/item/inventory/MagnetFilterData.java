package net.messer.mystical_index.item.inventory;

import net.messer.util.MysticalUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The Book of Magnetism's mode and item filter.
 *
 * <p>Stored under its own keys, deliberately distinct from the legacy {@code Filtered Items} list
 * the book has always written: an old book keeps working untouched, and the migration in
 * {@link #modeOf} reads the legacy list to decide what an unlabelled book should mean.
 *
 * <p>Reads are cached behind an identity check on the custom-data component, the same trick the
 * farming inventory uses - the magnet keeps ticking while its screen is open, so anything holding
 * one of these has to notice writes made underneath it.
 */
public class MagnetFilterData {

    /** What the magnet does with the filter. */
    public enum Mode {
        ALL, WHITELIST, BLACKLIST, NONE;

        public Mode next() {
            return switch (this) {
                case ALL -> WHITELIST;
                case WHITELIST -> BLACKLIST;
                case BLACKLIST -> NONE;
                case NONE -> ALL;
            };
        }

        public String translationKey() {
            return "gui.mystical_index.magnetism.mode." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** Ghost slots shown on screen. Stored lists longer than this are read and honoured. */
    public static final int FILTER_SLOTS = 5;

    private static final String MODE_KEY = "MagnetMode";
    private static final String PREV_MODE_KEY = "MagnetPrevMode";
    private static final String FILTER_KEY = "MagnetFilter";
    private static final String LEGACY_FILTER_KEY = "Filtered Items";

    public final ItemStack stack;
    private final List<Item> slots = new ArrayList<>();
    private Set<Item> lookup = Set.of();      // grid + legacy, for WHITELIST
    private Set<Item> gridOnly = Set.of();    // the five squares only, for BLACKLIST
    private CustomData seen;
    private boolean loaded;

    public MagnetFilterData(ItemStack stack) {
        this.stack = stack;
        reloadIfStale();
    }

    private void reloadIfStale() {
        var current = stack.get(DataComponents.CUSTOM_DATA);
        if (loaded && current == seen)
            return;

        seen = current;
        loaded = true;
        slots.clear();
        while (slots.size() < FILTER_SLOTS)
            slots.add(null);

        var compound = MysticalUtil.getCustomData(stack);
        if (compound == null) {
            lookup = Set.of();
            gridOnly = Set.of();
            return;
        }

        // One set built per reload, not per candidate entity: the pull loop asks this question for
        // every item entity in range every tick, and a per-entity NBT walk would be the expensive
        // part of the whole book.
        var built = new HashSet<Item>();

        // The grid was briefly nine slots and is now five. A longer stored list is still read in
        // full so nothing a player set is silently dropped; only the first FILTER_SLOTS entries
        // have a square to sit in, and the next write trims the list to that length.
        var list = compound.getListOrEmpty(FILTER_KEY);
        for (int i = 0; i < list.size(); i++) {
            var item = itemFrom(list.getStringOr(i, ""));
            if (item == null)
                continue;

            if (i < FILTER_SLOTS)
                slots.set(i, item);
            built.add(item);
        }

        gridOnly = Set.copyOf(built);

        // The legacy list is a WHITELIST-era artifact and is only meaningful as one. An old book
        // carries its filter under "Filtered Items" and was migrated to WHITELIST on the strength
        // of it, so whitelisting has to honour it or that book suddenly attracts nothing. Blacklist
        // must NOT: those entries were "pull these", and reading them as "never pull these" would
        // silently ban everything the player once collected. Display stays the five squares.
        var legacy = compound.getListOrEmpty(LEGACY_FILTER_KEY);
        for (int i = 0; i < legacy.size(); i++) {
            var item = itemFrom(legacy.getCompoundOrEmpty(i).getStringOr("ItemName", ""));
            if (item != null)
                built.add(item);
        }

        lookup = built;
    }

    private static Item itemFrom(String id) {
        var parsed = Identifier.tryParse(id);
        if (parsed == null)
            return null;

        var item = BuiltInRegistries.ITEM.getValue(parsed);
        return item == null || item == net.minecraft.world.item.Items.AIR ? null : item;
    }

    // ---- mode -------------------------------------------------------------------------------

    /**
     * The book's mode, migrating an unlabelled book on read.
     *
     * <p>An old book has no mode key but does have a legacy filter list, and its behaviour was
     * "pull only what is listed" - so it becomes WHITELIST, not ALL. Reading it as ALL would turn
     * every existing carefully-filtered magnet into a vacuum, which is the one migration outcome a
     * player would actually notice and hate. A book with no data at all is new, and defaults to
     * ALL as designed.
     */
    public static Mode modeOf(ItemStack stack) {
        var compound = MysticalUtil.getCustomData(stack);
        if (compound == null)
            return Mode.ALL;

        var stored = compound.getStringOr(MODE_KEY, "");
        if (!stored.isEmpty()) {
            try {
                return Mode.valueOf(stored);
            } catch (IllegalArgumentException ignored) {
                return Mode.ALL;
            }
        }

        return compound.getListOrEmpty(LEGACY_FILTER_KEY).isEmpty() ? Mode.ALL : Mode.WHITELIST;
    }

    public static void setMode(ItemStack stack, Mode mode) {
        var previous = modeOf(stack);
        MysticalUtil.editCustomData(stack, compound -> {
            compound.putString(MODE_KEY, mode.name());
            // Only a non-NONE mode is worth remembering; otherwise the sneak toggle would have
            // nothing to come back to after being flicked off twice.
            if (previous != Mode.NONE)
                compound.putString(PREV_MODE_KEY, previous.name());
        });
    }

    /** Flips between NONE and whatever the book was doing before, for the sneak shortcut. */
    public static Mode toggleDisabled(ItemStack stack) {
        var current = modeOf(stack);
        if (current != Mode.NONE) {
            setMode(stack, Mode.NONE);
            return Mode.NONE;
        }

        var compound = MysticalUtil.getCustomData(stack);
        var previous = compound == null ? "" : compound.getStringOr(PREV_MODE_KEY, "");
        Mode restored = Mode.ALL;
        if (!previous.isEmpty()) {
            try {
                restored = Mode.valueOf(previous);
            } catch (IllegalArgumentException ignored) {
                restored = Mode.ALL;
            }
        }
        if (restored == Mode.NONE)
            restored = Mode.ALL;

        setMode(stack, restored);
        return restored;
    }

    // ---- filter -----------------------------------------------------------------------------

    public Item slot(int index) {
        reloadIfStale();
        return index >= 0 && index < FILTER_SLOTS ? slots.get(index) : null;
    }

    public int filledCount() {
        reloadIfStale();
        return lookup.size();
    }

    /** Whether the magnet should pull this item, given the book's mode. */
    public boolean allows(Item item) {
        reloadIfStale();
        return switch (modeOf(stack)) {
            case ALL -> true;
            case NONE -> false;
            case WHITELIST -> lookup.contains(item);
            // Grid only, deliberately - see the note in reloadIfStale about the legacy list.
            case BLACKLIST -> !gridOnly.contains(item);
        };
    }

    public void setSlot(int index, Item item) {
        if (index < 0 || index >= FILTER_SLOTS)
            return;

        reloadIfStale();
        slots.set(index, item);
        write();
    }

    /**
     * Puts an item in the first empty ghost slot.
     *
     * <p>Returns false and changes nothing when the item is already filtered or all five squares
     * are taken. A full grid is a no-op rather than replacing the oldest entry: silently evicting
     * something the player chose is the more surprising of the two, and there is no undo.
     */
    public boolean addToFirstEmpty(Item item) {
        reloadIfStale();
        if (lookup.contains(item))
            return false;

        for (int i = 0; i < FILTER_SLOTS; i++) {
            if (slots.get(i) == null) {
                slots.set(i, item);
                write();
                return true;
            }
        }
        return false;
    }

    private void write() {
        var list = new ListTag();
        for (var item : slots)
            list.add(StringTag.valueOf(item == null ? "" : BuiltInRegistries.ITEM.getKey(item).toString()));

        // Its own key, written through the shared path: the tick state and the legacy list are
        // untouched, and the write rides the self-updating suppression the book already has.
        MysticalUtil.editCustomData(stack, compound -> compound.put(FILTER_KEY, list));
        seen = stack.get(DataComponents.CUSTOM_DATA);
    }
}
