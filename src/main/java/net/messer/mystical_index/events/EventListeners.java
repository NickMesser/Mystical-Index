package net.messer.mystical_index.events;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.messer.mystical_index.block.entity.IndexLecternBlockEntity;
import net.messer.mystical_index.block.entity.LibraryBlockEntity;
import net.messer.mystical_index.util.LecternTracker;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;

public class EventListeners {
    public static void register() {
        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register(EventListeners::onLoadBlockEntity);
        ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register(EventListeners::onUnloadBlockEntity);
    }

    private static void onLoadBlockEntity(BlockEntity blockEntity, ServerWorld world) {
        if (blockEntity instanceof LibraryBlockEntity library) {
            LecternTracker.tryRegisterToLectern(library, false);
        }
    }

    private static void onUnloadBlockEntity(BlockEntity blockEntity, ServerWorld world) {
        if (blockEntity instanceof IndexLecternBlockEntity lectern) {
            LecternTracker.removeIndexLectern(lectern);
        }
    }
}
