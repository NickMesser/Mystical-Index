package net.messer.mystical_index.item.inventory;

import net.messer.util.MysticalUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * A Book of Experience's stored points, and the transfer maths.
 *
 * <p>Storage is raw experience POINTS as a long, never levels. Levels are nonlinear - the cost of
 * one level ranges from 7 points to hundreds - so a level is not a unit you can add and subtract
 * without loss. Points are, and the level figure shown to the player is derived from them.
 *
 * <p><b>The conservation invariant.</b> Deposit and withdrawal move the same integer number of
 * points in opposite directions, and no code path converts to a level and back. So a deposit
 * followed by a withdrawal returns exactly what was taken, at level 3 and at level 300 alike.
 *
 * <p>One honest caveat: vanilla keeps {@code experienceProgress} as a float, so
 * {@code floor(progress * step)} can read one point low at float-epsilon boundaries. The POINTS are
 * still conserved - nothing is created or destroyed - only the restored progress bar can sit a
 * single point off. That is not worth compensating for, and deliberately is not.
 */
public class ExperienceBookData {

    private static final String POINTS_KEY = "XpPoints";
    private static final String AUTO_COLLECT_KEY = "XpAutoCollect";

    public final ItemStack stack;
    private long points;
    private CustomData seen;
    private boolean loaded;

    public ExperienceBookData(ItemStack stack) {
        this.stack = stack;
        reloadIfStale();
    }

    private void reloadIfStale() {
        var current = stack.get(DataComponents.CUSTOM_DATA);
        if (loaded && current == seen)
            return;

        seen = current;
        loaded = true;

        var compound = MysticalUtil.getCustomData(stack);
        points = compound == null ? 0L : Math.max(0L, compound.getLongOr(POINTS_KEY, 0L));
    }

    public long points() {
        reloadIfStale();
        return points;
    }

    public boolean isEmpty() {
        return points() <= 0L;
    }

    public boolean autoCollect() {
        var compound = MysticalUtil.getCustomData(stack);
        return compound != null && compound.getBooleanOr(AUTO_COLLECT_KEY, false);
    }

    public void setAutoCollect(boolean enabled) {
        MysticalUtil.editCustomData(stack, compound -> compound.putBoolean(AUTO_COLLECT_KEY, enabled));
        seen = stack.get(DataComponents.CUSTOM_DATA);
    }

    private void setPoints(long value) {
        long clamped = Math.max(0L, value);
        MysticalUtil.editCustomData(stack, compound -> compound.putLong(POINTS_KEY, clamped));
        points = clamped;
        seen = stack.get(DataComponents.CUSTOM_DATA);
    }

    /** Adds points directly - used by auto-collect, which already has a point count from the orb. */
    public void add(long amount) {
        if (amount > 0)
            setPoints(points() + amount);
    }

    // ---- transfer -----------------------------------------------------------------------------

    /** Points the player currently holds inside their current level, floored to an integer. */
    private static int progressPoints(Player player) {
        return (int) Math.floor(player.experienceProgress * player.getXpNeededForNextLevel());
    }

    /** Points one full level costs, at the level BELOW the player's current one. */
    private static int previousLevelCost(Player player) {
        int level = player.experienceLevel - 1;
        if (level < 0)
            return 0;
        if (level >= 30)
            return 9 * level - 158;
        if (level >= 15)
            return 5 * level - 38;
        return 2 * level + 7;
    }

    /**
     * Moves one level-step of the player's experience into the book.
     *
     * <p>Partial progress goes first: shedding it drops the player cleanly to the start of the
     * level they are already on. Only when there is no partial progress does a whole level come
     * out. A player with nothing at all is a no-op.
     *
     * <p>The subtraction goes through {@code giveExperiencePoints} with a negative amount, which
     * vanilla handles properly - it clamps totalExperience at zero and borrows down through levels
     * when progress would go negative. Setting the three fields by hand would have to reimplement
     * that borrow, so it does not.
     */
    public int deposit(Player player) {
        int take = progressPoints(player);
        if (take <= 0)
            take = previousLevelCost(player);

        if (take <= 0)
            return 0;

        player.giveExperiencePoints(-take);
        setPoints(points() + take);
        return take;
    }

    /**
     * Gives the player exactly enough to reach their next level, or everything left if that is less.
     */
    public int withdraw(Player player) {
        int step = player.getXpNeededForNextLevel();
        int needed = Math.max(0, step - progressPoints(player));
        if (needed <= 0)
            return 0;

        int give = (int) Math.min(points(), needed);
        if (give <= 0)
            return 0;

        player.giveExperiencePoints(give);
        setPoints(points() - give);
        return give;
    }

    // ---- display ------------------------------------------------------------------------------

    /** Whole levels the stored points are worth, counted from zero the way the player earns them. */
    public int storedLevels() {
        long remaining = points();
        int level = 0;
        while (true) {
            int cost = level >= 30 ? 9 * level - 158 : level >= 15 ? 5 * level - 38 : 2 * level + 7;
            if (remaining < cost)
                return level;

            remaining -= cost;
            level++;
        }
    }
}
