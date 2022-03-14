package net.messer.mystical_index.util;

import net.messer.mystical_index.block.entity.IndexLecternBlockEntity;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class LecternTracker {
    public static final Set<IndexLecternBlockEntity> indexLecterns = Collections.newSetFromMap(new WeakHashMap<>());

    public static void addIndexLectern(IndexLecternBlockEntity lectern) {
        indexLecterns.add(lectern);
    }

    public static void removeIndexLectern(IndexLecternBlockEntity lectern) {
        indexLecterns.remove(lectern);
    }

    @Nullable
    public static IndexLecternBlockEntity findNearestLectern(ServerPlayerEntity player, double maxDistance) {
        double closestFound = -1.0;
        IndexLecternBlockEntity result = null;
        for (IndexLecternBlockEntity lectern : indexLecterns) {
            Vec3d pos = Vec3d.ofCenter(lectern.getPos());
            double squaredDistance = player.squaredDistanceTo(pos.getX(), pos.getY(), pos.getZ());
            if (!(maxDistance < 0.0) && !(player.getWorld() == lectern.getWorld() && squaredDistance < maxDistance * maxDistance) || closestFound != -1.0 && !(squaredDistance < closestFound)) continue;
            closestFound = squaredDistance;
            result = lectern;
        }
        return result;
    }
}
