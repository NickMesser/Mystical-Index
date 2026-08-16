package net.messer.mystical_index.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.item.inventory.LibraryNetwork;
import net.messer.mystical_index.screen.MysticalLecternScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class LecternNetworking {

    public static final Identifier CONTENTS = new Identifier(MysticalIndex.MOD_ID, "lectern_contents");
    public static final Identifier ACTION = new Identifier(MysticalIndex.MOD_ID, "lectern_action");
    public static final Identifier FILL_RECIPE = new Identifier(MysticalIndex.MOD_ID, "lectern_fill_recipe");

    public static final int ACTION_EXTRACT = 0;
    public static final int ACTION_INSERT = 1;

    public static final int RECIPE_GRID_SIZE = 9;
    public static final int MAX_RECIPE_CANDIDATES = 8;

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(ACTION, (server, player, handler, buf, responseSender) -> {
            // The buffer is recycled as soon as this returns, so it has to be drained here on the
            // network thread rather than inside the scheduled task.
            int syncId = buf.readVarInt();
            int action = buf.readByte();
            // Reject any unknown opcode outright. Otherwise it falls through and is executed as an
            // insert, but read with whatever layout the sender chose, corrupting the rest.
            if (action != ACTION_EXTRACT && action != ACTION_INSERT)
                return;

            ItemVariant readVariant = ItemVariant.blank();
            if (action == ACTION_EXTRACT)
                readVariant = ItemVariant.fromPacket(buf);

            int button = buf.readByte();

            boolean readToInventory = false;
            if (action == ACTION_EXTRACT)
                readToInventory = buf.readBoolean();

            final ItemVariant variant = readVariant;
            final boolean toInventory = readToInventory;

            server.execute(() -> {
                if (!(player.currentScreenHandler instanceof MysticalLecternScreenHandler lectern)
                        || lectern.syncId != syncId || !lectern.canUse(player))
                    return;

                switch (action) {
                    case ACTION_EXTRACT -> lectern.handleExtract(variant, button, toInventory);
                    case ACTION_INSERT -> lectern.handleInsert(button);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(FILL_RECIPE, (server, player, handler, buf, responseSender) -> {
            int syncId = buf.readVarInt();

            // Drained here on the network thread, same as above.
            List<List<ItemVariant>> slotCandidates = new ArrayList<>(RECIPE_GRID_SIZE);
            for (int slot = 0; slot < RECIPE_GRID_SIZE; slot++) {
                int count = buf.readVarInt();
                // A hostile client can claim a huge candidate count to drive millions of variant
                // reads on the netty thread. Drop the whole packet rather than reading past the cap
                // to "stay aligned": the buffer is discarded on return, so alignment stops mattering.
                if (count < 0 || count > MAX_RECIPE_CANDIDATES)
                    return;

                List<ItemVariant> candidates = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    var variant = ItemVariant.fromPacket(buf);
                    if (!variant.isBlank())
                        candidates.add(variant);
                }

                slotCandidates.add(candidates);
            }

            server.execute(() -> {
                if (!(player.currentScreenHandler instanceof MysticalLecternScreenHandler lectern)
                        || lectern.syncId != syncId || !lectern.canUse(player))
                    return;

                lectern.handleFillRecipe(slotCandidates);
            });
        });
    }

    public static void sendContents(ServerPlayerEntity player, int syncId, List<LibraryNetwork.Entry> entries) {
        var buf = PacketByteBufs.create();
        buf.writeVarInt(syncId);
        buf.writeVarInt(entries.size());
        for (var entry : entries) {
            entry.variant().toPacket(buf);
            buf.writeVarLong(entry.count());
        }

        ServerPlayNetworking.send(player, CONTENTS, buf);
    }
}
