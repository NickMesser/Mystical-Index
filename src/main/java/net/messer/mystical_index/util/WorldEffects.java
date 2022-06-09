package net.messer.mystical_index.util;

import net.messer.mystical_index.util.request.IndexInteractable;
import net.messer.mystical_index.util.request.Request;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.util.ParticleUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.world.World;

public class WorldEffects {
    private static final double ITEMS_PER_PARTICLE = 16;
    private static final double PARTICLE_SPACING = 0.4;

    public static void extractionParticles(Request request, BlockEntity source) {
        Vec3d sourcePos = request.getSourcePosition();

        if (sourcePos != null) {
            movingEnchantParticles(
                    source.getWorld(),
                    Vec3d.ofCenter(source.getPos()),
                    sourcePos,
                    (int) Math.ceil(request.getAmountAffected() / ITEMS_PER_PARTICLE)
            );
        }
    }

    public static void insertionParticles(Request request, BlockEntity source) { // TODO oopify
        Vec3d sourcePos = request.getSourcePosition();

        if (sourcePos != null) {
            movingEnchantParticles(
                    source.getWorld(),
                    sourcePos,
                    Vec3d.ofCenter(source.getPos()),
                    (int) Math.ceil(request.getAmountAffected() / ITEMS_PER_PARTICLE)
            );
        }
    }

    private static void movingEnchantParticles(World world, Vec3d source, Vec3d destination, int amount) {
        Vec3d velocity = source.subtract(destination);
        double length = velocity.length();
        for (int i = 0; i < amount; i++) {
            for (double d = 0; d < length; d += PARTICLE_SPACING) {
                Vec3d modVelocity = velocity.multiply(d / length);
                ((ServerWorld) world).spawnParticles(ParticleTypes.ENCHANT,
                        destination.getX(), destination.getY() + 1, destination.getZ(),
                        0,
                        modVelocity.getX(), modVelocity.getY() - 1, modVelocity.getZ(),
                        1
                );
            }
        }
    }

    public static void drawParticleCircle(ServerWorld world, Vec3d pos, int cycleTicks, int cycleOffset, double radius) {
        int currentTick = world.getServer().getTicks();
        double animationPos = (currentTick + cycleOffset) % cycleTicks / ((double) cycleTicks) * (2 * Math.PI);
        Vec3d particlePos = pos.add(radius * Math.cos(animationPos), 0, radius * Math.sin(animationPos));
        world.spawnParticles(ParticleTypes.ENCHANT, particlePos.getX(), particlePos.getY(), particlePos.getZ(), 1, 0, 0, 0, 0);
    }

    public static void registrationParticles(IndexInteractable interactable) { // TODO add subtle sound
        if (interactable instanceof BlockEntity blockEntity) {
            blockParticles(blockEntity.getWorld(), blockEntity.getPos(), ParticleTypes.SOUL_FIRE_FLAME);
        }
    }

    public static void blockParticles(World world, BlockPos pos, ParticleEffect effect) {
        if (world instanceof ServerWorld serverWorld) {
            ServerParticleUtil.spawnParticle(serverWorld, pos, effect, UniformIntProvider.create(3, 5));
        }
    }
}
