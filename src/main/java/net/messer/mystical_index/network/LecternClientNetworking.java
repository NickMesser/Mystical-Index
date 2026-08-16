package net.messer.mystical_index.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.messer.mystical_index.item.inventory.LibraryNetwork;
import net.messer.mystical_index.screen.MysticalLecternScreenHandler;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class LecternClientNetworking {

    public static void registerClientReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(LecternNetworking.CONTENTS, (client, handler, buf, responseSender) -> {
            int syncId = buf.readVarInt();
            int size = buf.readVarInt();

            List<LibraryNetwork.Entry> entries = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                var variant = ItemVariant.fromPacket(buf);
                entries.add(new LibraryNetwork.Entry(variant, buf.readVarLong()));
            }

            client.execute(() -> {
                if (client.player == null)
                    return;

                if (client.player.currentScreenHandler instanceof MysticalLecternScreenHandler lectern
                        && lectern.syncId == syncId) {
                    lectern.setClientEntries(entries);
                }
            });
        });
    }

    public static void sendExtract(int syncId, ItemVariant variant, int button, boolean toInventory) {
        var buf = PacketByteBufs.create();
        buf.writeVarInt(syncId);
        buf.writeByte(LecternNetworking.ACTION_EXTRACT);
        variant.toPacket(buf);
        buf.writeByte(button);
        buf.writeBoolean(toInventory);

        ClientPlayNetworking.send(LecternNetworking.ACTION, buf);
    }

    public static void sendInsert(int syncId, int button) {
        var buf = PacketByteBufs.create();
        buf.writeVarInt(syncId);
        buf.writeByte(LecternNetworking.ACTION_INSERT);
        buf.writeByte(button);

        ClientPlayNetworking.send(LecternNetworking.ACTION, buf);
    }
}
