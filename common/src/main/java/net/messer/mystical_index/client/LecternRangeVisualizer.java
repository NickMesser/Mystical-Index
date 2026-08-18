package net.messer.mystical_index.client;

import net.messer.mystical_index.block.custom.MysticalLecternBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-side, opt-in visualization of a lectern's reach.
 *
 * <p>Draws two things while a lectern is toggled on: a dotted chain out to every library the SERVER
 * said it linked, and a ring at the radius the server used. Nothing here discovers anything - the
 * positions arrive in a payload, because a client-side rescan would disagree with the server the
 * moment chunk loading differed, and a tool that lies about the network is worse than no tool.
 *
 * <p>The toggle deliberately outlives the screen: turning it on and walking around is the useful
 * mode. It is dropped on a world change, and dropped silently the moment the block at the toggled
 * position stops being a lectern, so a broken or moved lectern cannot leave a ghost ring behind.
 */
public class LecternRangeVisualizer {

    /** What the server told us about one lectern. */
    public record Links(BlockPos lectern, int radius, List<BlockPos> libraries) {}

    private static final Map<BlockPos, Links> TOGGLED = new HashMap<>();
    private static ClientLevel boundLevel;

    // Refresh cadence and budget. Particles are spawned twice a second rather than every tick -
    // END_ROD lives comfortably longer than 10 ticks, so a faster refresh only stacks duplicates
    // on the same spots and turns the chain into a glowing rope.
    private static final int REFRESH_TICKS = 10;

    // Hard ceiling on spawns per refresh, across every toggled lectern combined. Chains are
    // budgeted first (they carry the actual information), the ring gets whatever is left.
    // Scaling rule: one particle per block of chain length, halved past 32 blocks so a distant
    // library costs about the same as a near one, and the ring is a fixed 16 points regardless of
    // radius - a bigger circle gets sparser rather than denser.
    private static final int MAX_PARTICLES_PER_REFRESH = 240;
    private static final int RING_POINTS = 16;
    private static final double CHAIN_SPACING = 1.0;
    private static final double CHAIN_HALVE_BEYOND = 32.0;

    private static int tickCounter;
    private static float ringPhase;

    public static boolean isToggled(BlockPos lectern) {
        return TOGGLED.containsKey(lectern);
    }

    /** Turns a lectern's visualization on with the server's answer, or off if it was already on. */
    public static void toggle(Links links) {
        if (TOGGLED.remove(links.lectern()) == null)
            TOGGLED.put(links.lectern(), links);
    }

    /** Refreshes an already-toggled lectern in place; ignored if it is currently off. */
    public static void update(Links links) {
        if (TOGGLED.containsKey(links.lectern()))
            TOGGLED.put(links.lectern(), links);
    }

    public static void clear() {
        TOGGLED.clear();
    }

    public static void tick(ClientLevel level) {
        // A dimension change hands us a different level instance; anything remembered describes a
        // world we are no longer in.
        if (level != boundLevel) {
            boundLevel = level;
            clear();
            return;
        }

        if (TOGGLED.isEmpty())
            return;

        if (++tickCounter % REFRESH_TICKS != 0)
            return;

        ringPhase += 0.37F;

        // Drop anything that is no longer a lectern before spending any budget on it.
        var stale = new ArrayList<BlockPos>();
        for (var pos : TOGGLED.keySet()) {
            if (!(level.getBlockState(pos).getBlock() instanceof MysticalLecternBlock))
                stale.add(pos);
        }
        for (var pos : stale)
            TOGGLED.remove(pos);

        int budget = MAX_PARTICLES_PER_REFRESH;
        for (var links : TOGGLED.values()) {
            budget = drawChains(level, links, budget);
            budget = drawRing(level, links, budget);
            if (budget <= 0)
                return;
        }
    }

    private static int drawChains(ClientLevel level, Links links, int budget) {
        Vec3 from = Vec3.atCenterOf(links.lectern());

        for (var library : links.libraries()) {
            if (budget <= 0)
                return 0;

            Vec3 to = Vec3.atCenterOf(library);
            double distance = from.distanceTo(to);
            if (distance < 0.5)
                continue;

            int points = (int) Math.ceil(distance / CHAIN_SPACING);
            if (distance > CHAIN_HALVE_BEYOND)
                points = Math.max(1, points / 2);
            points = Math.min(points, budget);

            for (int i = 1; i <= points; i++) {
                double t = i / (double) (points + 1);
                spawn(level, from.lerp(to, t));
            }
            budget -= points;
        }

        return budget;
    }

    private static int drawRing(ClientLevel level, Links links, int budget) {
        int points = Math.min(RING_POINTS, budget);
        if (points <= 0)
            return 0;

        Vec3 centre = Vec3.atCenterOf(links.lectern());
        for (int i = 0; i < points; i++) {
            // The phase advances every refresh so the ring shimmers around its circumference
            // instead of strobing the same sixteen dots on and off.
            double angle = (i / (double) points) * Math.PI * 2.0 + ringPhase;
            spawn(level, new Vec3(
                    centre.x + Math.cos(angle) * links.radius(),
                    centre.y,
                    centre.z + Math.sin(angle) * links.radius()));
        }

        return budget - points;
    }

    /**
     * One marker particle, held still.
     *
     * <p>END_ROD rather than the mod's usual ENCHANT, and the difference is not cosmetic: ENCHANT
     * reads its velocity arguments as an ORIGIN OFFSET and animates inward to the spawn point (the
     * lectern's ambient glyphs rely on exactly that), so it would streak across the world instead
     * of marking a position. END_ROD with zero velocity stays where it is put and glows, which is
     * what a survey line needs. Do not "fix" this back to ENCHANT.
     */
    private static void spawn(ClientLevel level, Vec3 at) {
        level.addParticle(ParticleTypes.END_ROD, at.x, at.y, at.z, 0.0, 0.0, 0.0);
    }
}
