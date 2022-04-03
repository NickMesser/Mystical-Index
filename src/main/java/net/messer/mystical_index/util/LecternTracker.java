package net.messer.mystical_index.util;

import net.messer.mystical_index.block.entity.IndexLecternBlockEntity;
import net.messer.mystical_index.util.request.IIndexInteractable;
import net.minecraft.block.entity.BlockEntity;
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

    public static void tryRegisterToLectern(IIndexInteractable interactable) {
        tryRegisterToLectern(interactable, true);
    }

    public static void tryRegisterToLectern(IIndexInteractable interactable, boolean particles) {
        if (interactable instanceof BlockEntity blockEntity) {
            var pos = blockEntity.getPos();

            for (IndexLecternBlockEntity lectern : indexLecterns) {
                var lPos = lectern.getPos();
                var range = lectern.getMaxRange(true);

                if (lPos.getX() - range <= pos.getX() && lPos.getY() - range <= pos.getY() && lPos.getZ() - range <= pos.getZ() &&
                    lPos.getX() + range >= pos.getX() && lPos.getY() + range >= pos.getY() && lPos.getZ() + range >= pos.getZ()) {
                    lectern.getLinkedLibraries().add(interactable, particles ? WorldEffects::registrationParticles : i -> {});
                }
            }
        }
    }

    public static void unRegisterFromLectern(IIndexInteractable interactable) {
        for (IndexLecternBlockEntity lectern : indexLecterns) {
            lectern.getLinkedLibraries().interactables.remove(interactable);
        }
    }
}
