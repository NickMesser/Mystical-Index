package net.messer.mystical_index.util;

import net.messer.mystical_index.util.request.Request;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class ParticleSystem {
    private static final double ITEMS_PER_PARTICLE = 4;

    public static void extractionParticles(Request request, BlockEntity source) {
        Vec3d sourcePos = request.getSourcePosition();

        if (sourcePos != null) {
            movingEnchantParticles(
                    source.getWorld(),
                    Vec3d.ofCenter(source.getPos()),
                    sourcePos.add(0, 1, 0),
                    (int) Math.ceil(request.getAmountAffected() / ITEMS_PER_PARTICLE)
            );
        }
    }

    public static void insertionParticles(Request request, BlockEntity source) { // TODO oopify
        Vec3d sourcePos = request.getSourcePosition();

        if (sourcePos != null) {
            movingEnchantParticles(
                    source.getWorld(),
                    sourcePos.add(0, 1, 0),
                    Vec3d.ofCenter(source.getPos()),
                    (int) Math.ceil(request.getAmountAffected() / ITEMS_PER_PARTICLE)
            );
        }
    }

    public static void movingEnchantParticles(World world, Vec3d source, Vec3d destination, int amount) {
        Vec3d velocity = source.subtract(destination);
        for (int i = 0; i < amount; i++)
            ((ServerWorld) world).spawnParticles(ParticleTypes.ENCHANT,
                    destination.getX(), destination.getY(), destination.getZ(),
                    0,
                    velocity.getX(), velocity.getY(), velocity.getZ(),
                    1
            );
    }
}
