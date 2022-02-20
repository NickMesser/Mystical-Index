package net.messer.mystical_index.util;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class ParticleSystem {
    private static final double ITEMS_PER_PARTICLE = 4;

    public static void extractionParticles(Request request, BlockEntity source) {
        World world = source.getWorld();
        Vec3d requestSourcePos = request.getSourcePosition().add(0, 1, 0);
        if (world != null && requestSourcePos != null) {
            Vec3d sourcePos = Vec3d.ofCenter(source.getPos());
            Vec3d velocity = sourcePos.subtract(requestSourcePos);
            int particleAmount = (int) Math.ceil(request.getAmountExtracted() / ITEMS_PER_PARTICLE);
            for (int i = 0; i < particleAmount; i++)
                ((ServerWorld) world).spawnParticles(ParticleTypes.ENCHANT,
                        requestSourcePos.getX(), requestSourcePos.getY(), requestSourcePos.getZ(),
                        0,
                        velocity.getX(), velocity.getY(), velocity.getZ(),
                        1
                );
        }
    }
}
