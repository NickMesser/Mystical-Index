package net.messer.mystical_index.event;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.messer.mystical_index.block.entity.LibraryBlockEntity;
import net.messer.mystical_index.block.entity.MysticalLecternBlockEntity;
import net.messer.mystical_index.util.LecternTracker;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;

public class ModEvents {
    public static void register() {
        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register(ModEvents::onLoadBlockEntity);
        ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register(ModEvents::onUnloadBlockEntity);
    }

    private static void onLoadBlockEntity(BlockEntity blockEntity, ServerWorld world) {
        if (blockEntity instanceof LibraryBlockEntity library) {
            LecternTracker.tryRegisterToLectern(library, false);
        }
    }

    private static void onUnloadBlockEntity(BlockEntity blockEntity, ServerWorld world) {
        if (blockEntity instanceof MysticalLecternBlockEntity lectern) {
            LecternTracker.removeIndexLectern(lectern);
        }
    }
}
