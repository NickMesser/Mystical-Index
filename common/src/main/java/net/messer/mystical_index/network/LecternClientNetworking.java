package net.messer.mystical_index.network;

import dev.architectury.networking.NetworkManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.messer.mystical_index.item.inventory.BookItemVariant;
import net.messer.mystical_index.client.LecternRangeVisualizer;
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

                    if (player.containerMenu instanceof MysticalLecternScreenHandler lectern
                            && lectern.containerId == payload.syncId()) {
                        lectern.setClientEntries(payload.entries());
                        lectern.clientCapacity = payload.capacity();
                    }
                });

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, LecternNetworking.LINKS,
                LecternNetworking.LinksPayload.CODEC, (payload, context) -> {
                    var links = new LecternRangeVisualizer.Links(
                            payload.lectern(), payload.radius(), payload.libraries());
                    lastLinks = links;
                    // Refresh in place if this lectern is already being shown, so a library
                    // appearing or unloading redraws without the player toggling twice.
                    LecternRangeVisualizer.update(links);
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

    /** Latest server answer for the lectern currently open, for the toggle button to act on. */
    public static LecternRangeVisualizer.Links lastLinks;
}
