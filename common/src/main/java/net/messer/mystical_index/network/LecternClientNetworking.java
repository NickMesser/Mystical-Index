package net.messer.mystical_index.network;

import dev.architectury.networking.NetworkManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.messer.mystical_index.item.inventory.BookItemVariant;
import net.messer.mystical_index.screen.MysticalLecternScreenHandler;

@Environment(EnvType.CLIENT)
public class LecternClientNetworking {

    public static void registerClientReceivers() {
        // Registers the clientbound payload type and its handler in one call; the dedicated server
        // registers the type on its own from LecternNetworking.registerPayloads().
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, LecternNetworking.CONTENTS,
                LecternNetworking.ContentsPayload.CODEC, (payload, context) -> {
                    var player = context.getPlayer();
                    if (player == null)
                        return;

                    if (player.currentScreenHandler instanceof MysticalLecternScreenHandler lectern
                            && lectern.syncId == payload.syncId()) {
                        lectern.setClientEntries(payload.entries());
                    }
                });
    }

    public static void sendExtract(int syncId, BookItemVariant variant, int button, boolean toInventory) {
        NetworkManager.sendToServer(new LecternNetworking.ActionPayload(
                syncId, LecternNetworking.ACTION_EXTRACT, variant, button, toInventory));
    }

    public static void sendInsert(int syncId, int button) {
        NetworkManager.sendToServer(new LecternNetworking.ActionPayload(
                syncId, LecternNetworking.ACTION_INSERT, BookItemVariant.blank(), button, false));
    }
}
