package net.messer.mystical_index.util;

import net.messer.mystical_index.block.entity.MysticalLecternBlockEntity;
import net.messer.mystical_index.item.custom.book.MysticalBookItem;
import net.messer.mystical_index.item.custom.page.type.IndexingTypePage;
import net.messer.mystical_index.util.request.IndexInteractable;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.apache.logging.log4j.util.TriConsumer;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class LecternTracker {
    public static final Set<MysticalLecternBlockEntity> indexLecterns = Collections.newSetFromMap(new WeakHashMap<>());

    public static void addIndexLectern(MysticalLecternBlockEntity lectern) {
        indexLecterns.add(lectern);
    }

    public static void removeIndexLectern(MysticalLecternBlockEntity lectern) {
        indexLecterns.remove(lectern);
    }

    @Nullable
    public static MysticalLecternBlockEntity findNearestLectern(ServerPlayerEntity player, double maxDistance) {
        double closestFound = -1.0;
        MysticalLecternBlockEntity result = null;
        for (MysticalLecternBlockEntity lectern : indexLecterns) {
            Vec3d pos = Vec3d.ofCenter(lectern.getPos());
            double squaredDistance = player.squaredDistanceTo(pos.getX(), pos.getY(), pos.getZ());
            if (!(maxDistance < 0.0) && !(player.getWorld() == lectern.getWorld() && squaredDistance < maxDistance * maxDistance) || closestFound != -1.0 && !(squaredDistance < closestFound)) continue;
            closestFound = squaredDistance;
            result = lectern;
        }
        return result;
    }

    public static void tryRegisterToLectern(IndexInteractable interactable) {
        tryRegisterToLectern(interactable, true);
    }

    public static void tryRegisterToLectern(IndexInteractable interactable, boolean particles) {
        if (interactable instanceof BlockEntity blockEntity) {
            var pos = blockEntity.getPos();

            forEachIndexingLectern((book, lectern, page) -> {
                if (page.hasRangedLinking(book)) {
                    var lPos = lectern.getPos();
                    var range = page.getMaxRange(book, true);

                    if (lPos.getX() - range <= pos.getX() && lPos.getY() - range <= pos.getY() && lPos.getZ() - range <= pos.getZ() &&
                            lPos.getX() + range >= pos.getX() && lPos.getY() + range >= pos.getY() && lPos.getZ() + range >= pos.getZ()) {

                        page.getLecternIndex(lectern).add(interactable, particles ? WorldEffects::registrationParticles : i -> { });
                    }
                }
            });
        }
    }

    public static void unRegisterFromLectern(IndexInteractable interactable) {
        forEachIndexingLectern((book, lectern, page) -> {
            if (page.hasRangedLinking(book)) {
                page.getLecternIndex(lectern).interactables.remove(interactable);
            }
        });
    }

    private static void forEachIndexingLectern(TriConsumer<ItemStack, MysticalLecternBlockEntity, IndexingTypePage> consumer) {
        for (MysticalLecternBlockEntity lectern : indexLecterns) {
            var book = lectern.getBook();
            if (((MysticalBookItem) book.getItem()).getTypePage(book) instanceof IndexingTypePage page) {
                consumer.accept(book, lectern, page);
            }
        }
    }
}
