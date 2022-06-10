package net.messer.mystical_index.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.world.World;

@Environment(value = EnvType.CLIENT)
public class Particles {
    /**
     * Spawns particles covering all faces of a block.
     * @param world The world to spawn particles in.
     * @param pos The position of the block.
     * @param effect The particle effect to spawn.
     * @param amount The amount of particles to spawn.
     * @param speed The maximum speed of the particles.
     */
    public static void spawnParticlesCoveringBlock(World world, BlockPos pos, ParticleEffect effect, UniformIntProvider amount, Double speed) {
        for (Direction direction : Direction.values()) {
            int i = amount.get(world.random);
            for (int j = 0; j < i; ++j) {
                spawnParticle(world, pos, direction, effect, speed);
            }
        }
    }

    public static void spawnParticles(World world, Vec3d pos, ParticleEffect effect, double deltaX, double deltaY, double deltaZ, UniformIntProvider count, double speed) {
        int i = count.get(world.random);
        for (int j = 0; j < i; ++j) {
            double d = pos.x + MathHelper.nextDouble(world.random, -1.0, 1.0) * deltaX;
            double e = pos.y + MathHelper.nextDouble(world.random, -1.0, 1.0) * deltaY;
            double f = pos.z + MathHelper.nextDouble(world.random, -1.0, 1.0) * deltaZ;
            double g = MathHelper.nextDouble(world.random, -speed, speed);
            double h = MathHelper.nextDouble(world.random, -speed, speed);
            double k = MathHelper.nextDouble(world.random, -speed, speed);
            world.addParticle(effect, d, e, f, g, h, k);
        }
    }

    /**
     * Spawns one particle along the face of a block.
     * @param world The world to spawn particle in.
     * @param pos The position of the block.
     * @param direction The face of the block to spawn the particle on.
     * @param effect The particle effect to spawn.
     * @param speed The maximum speed of the particle.
     */
    public static void spawnParticle(World world, BlockPos pos, Direction direction, ParticleEffect effect, Double speed) {
        Vec3d vec3d = Vec3d.ofCenter(pos);
        int i = direction.getOffsetX();
        int j = direction.getOffsetY();
        int k = direction.getOffsetZ();
        double d = vec3d.x + (i == 0 ? MathHelper.nextDouble(world.random, -0.5, 0.5) : (double)i * 0.55);
        double e = vec3d.y + (j == 0 ? MathHelper.nextDouble(world.random, -0.5, 0.5) : (double)j * 0.55);
        double f = vec3d.z + (k == 0 ? MathHelper.nextDouble(world.random, -0.5, 0.5) : (double)k * 0.55);
        double g = i == 0 ? MathHelper.nextDouble(world.random, -speed, speed) : 0.0;
        double h = j == 0 ? MathHelper.nextDouble(world.random, -speed, speed) : 0.0;
        double l = k == 0 ? MathHelper.nextDouble(world.random, -speed, speed) : 0.0;
        world.addParticle(effect, d, e, f, g, h, l);
    }

    public static void drawParticleCircle(int tick, World world, Vec3d pos, int cycleTicks, int cycleOffset, double radius) {
        double animationPos = (tick + cycleOffset) % cycleTicks / ((double) cycleTicks) * (2 * Math.PI);
        Vec3d particlePos = pos.add(radius * Math.cos(animationPos), 0, radius * Math.sin(animationPos));
        world.addParticle(ParticleTypes.ENCHANT, particlePos.getX(), particlePos.getY(), particlePos.getZ(), 0, 0, 0);
    }
}