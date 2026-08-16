package net.messer.mystical_index.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.messer.mystical_index.screen.MysticalLecternScreenHandler;

@Environment(EnvType.CLIENT)
public class LecternClientNetworking {

    public static void registerClientReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(LecternNetworking.CONTENTS, (payload, context) -> {
            var player = context.player();
            if (player == null)
                return;

            if (player.currentScreenHandler instanceof MysticalLecternScreenHandler lectern
                    && lectern.syncId == payload.syncId()) {
                lectern.setClientEntries(payload.entries());
            }
        });
    }

    public static void sendExtract(int syncId, ItemVariant variant, int button, boolean toInventory) {
        ClientPlayNetworking.send(new LecternNetworking.ActionPayload(
                syncId, LecternNetworking.ACTION_EXTRACT, variant, button, toInventory));
    }

    public static void sendInsert(int syncId, int button) {
        ClientPlayNetworking.send(new LecternNetworking.ActionPayload(
                syncId, LecternNetworking.ACTION_INSERT, ItemVariant.blank(), button, false));
    }
}
