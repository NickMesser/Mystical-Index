package net.messer.mystical_index.util;

import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class LecternTracker {
    public static final Set<LecternBlockEntity> indexLecterns = Collections.newSetFromMap(new WeakHashMap<>());

    public static void addIndexLectern(LecternBlockEntity lectern) {
        indexLecterns.add(lectern);
    }

    public static void removeIndexLectern(LecternBlockEntity lectern) {
        indexLecterns.remove(lectern);
    }

    @Nullable
    public static LecternBlockEntity findNearestLectern(ServerPlayerEntity player, double maxDistance) {
        double closestFound = -1.0;
        LecternBlockEntity result = null;
        for (LecternBlockEntity lectern : indexLecterns) {
            Vec3d pos = Vec3d.ofCenter(lectern.getPos());
            double squaredDistance = player.squaredDistanceTo(pos.getX(), pos.getY(), pos.getZ());
            if (!(maxDistance < 0.0) && !(player.getWorld() == lectern.getWorld() && squaredDistance < maxDistance * maxDistance) || closestFound != -1.0 && !(squaredDistance < closestFound)) continue;
            closestFound = squaredDistance;
            result = lectern;
        }
        return result;
    }
}
