package net.messer.mystical_index.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import io.netty.handler.codec.DecoderException;
import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.item.inventory.BookItemVariant;
import net.messer.mystical_index.item.inventory.LibraryNetwork;
import net.messer.mystical_index.screen.MysticalLecternScreenHandler;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class LecternNetworking {

    public static final CustomPayload.Id<ContentsPayload> CONTENTS =
            new CustomPayload.Id<>(Identifier.of(MysticalIndex.MOD_ID, "lectern_contents"));
    public static final CustomPayload.Id<ActionPayload> ACTION =
            new CustomPayload.Id<>(Identifier.of(MysticalIndex.MOD_ID, "lectern_action"));
    public static final CustomPayload.Id<FillRecipePayload> FILL_RECIPE =
            new CustomPayload.Id<>(Identifier.of(MysticalIndex.MOD_ID, "lectern_fill_recipe"));

    public static final int ACTION_EXTRACT = 0;
    public static final int ACTION_INSERT = 1;

    public static final int RECIPE_GRID_SIZE = 9;
    public static final int MAX_RECIPE_CANDIDATES = 8;

    // No real library network holds this many distinct variants; a bigger claim is a hostile peer
    // trying to make the reader allocate or spin before anything has been validated.
    public static final int MAX_CONTENTS_ENTRIES = 65536;
    private static final int MAX_CONTENTS_PRESIZE = 4096;

    // The codecs are written by hand rather than composed out of tuples: the action packet's shape
    // depends on its action byte, and the recipe packet caps candidates on read while still
    // draining everything that was written. Both wire formats are unchanged.
    public record ContentsPayload(int syncId, List<LibraryNetwork.Entry> entries) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, ContentsPayload> CODEC =
                CustomPayload.codecOf(ContentsPayload::write, ContentsPayload::read);

        private void write(RegistryByteBuf buf) {
            buf.writeVarInt(syncId);
            buf.writeVarInt(entries.size());
            for (var entry : entries) {
                BookItemVariant.PACKET_CODEC.encode(buf, entry.variant());
                buf.writeVarLong(entry.count());
            }
        }

        private static ContentsPayload read(RegistryByteBuf buf) {
            int syncId = buf.readVarInt();
            int size = buf.readVarInt();
            // A hostile server can claim a huge size to make the client allocate and spin before
            // anything is validated. Reject a nonsense length outright, and only pre-size the list
            // within a sane bound.
            if (size < 0 || size > MAX_CONTENTS_ENTRIES)
                throw new DecoderException("Lectern contents length out of range: " + size);

            List<LibraryNetwork.Entry> entries = new ArrayList<>(Math.min(size, MAX_CONTENTS_PRESIZE));
            for (int i = 0; i < size; i++) {
                var variant = BookItemVariant.PACKET_CODEC.decode(buf);
                entries.add(new LibraryNetwork.Entry(variant, buf.readVarLong()));
            }

            return new ContentsPayload(syncId, entries);
        }

        @Override
        public Id<ContentsPayload> getId() {
            return CONTENTS;
        }
    }

    public record ActionPayload(int syncId, int action, BookItemVariant variant, int button, boolean toInventory)
            implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, ActionPayload> CODEC =
                CustomPayload.codecOf(ActionPayload::write, ActionPayload::read);

        private void write(RegistryByteBuf buf) {
            buf.writeVarInt(syncId);
            buf.writeByte(action);

            if (action == ACTION_EXTRACT)
                BookItemVariant.PACKET_CODEC.encode(buf, variant);

            buf.writeByte(button);

            if (action == ACTION_EXTRACT)
                buf.writeBoolean(toInventory);
        }

        private static ActionPayload read(RegistryByteBuf buf) {
            int syncId = buf.readVarInt();
            int action = buf.readByte();
            // Reject any unknown opcode outright. Otherwise it falls through and is executed as an
            // insert, but read with whatever layout the sender chose, corrupting the rest.
            if (action != ACTION_EXTRACT && action != ACTION_INSERT)
                throw new DecoderException("Unknown lectern action: " + action);

            BookItemVariant variant = BookItemVariant.blank();
            if (action == ACTION_EXTRACT)
                variant = BookItemVariant.PACKET_CODEC.decode(buf);

            int button = buf.readByte();

            boolean toInventory = false;
            if (action == ACTION_EXTRACT)
                toInventory = buf.readBoolean();

            return new ActionPayload(syncId, action, variant, button, toInventory);
        }

        @Override
        public Id<ActionPayload> getId() {
            return ACTION;
        }
    }

    public record FillRecipePayload(int syncId, List<List<BookItemVariant>> slotCandidates) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, FillRecipePayload> CODEC =
                CustomPayload.codecOf(FillRecipePayload::write, FillRecipePayload::read);

        private void write(RegistryByteBuf buf) {
            buf.writeVarInt(syncId);
            for (var candidates : slotCandidates) {
                buf.writeVarInt(candidates.size());
                for (var variant : candidates)
                    BookItemVariant.PACKET_CODEC.encode(buf, variant);
            }
        }

        private static FillRecipePayload read(RegistryByteBuf buf) {
            int syncId = buf.readVarInt();

            List<List<BookItemVariant>> slotCandidates = new ArrayList<>(RECIPE_GRID_SIZE);
            for (int slot = 0; slot < RECIPE_GRID_SIZE; slot++) {
                int count = buf.readVarInt();
                // A hostile client can claim a huge candidate count to drive millions of variant
                // reads on the netty thread. Reject the whole packet rather than reading past the
                // cap to "stay aligned": the payload is discarded either way, so alignment stops
                // mattering. The transfer handler never sends more than the cap.
                if (count < 0 || count > MAX_RECIPE_CANDIDATES)
                    throw new DecoderException("Lectern recipe candidate count out of range: " + count);

                List<BookItemVariant> candidates = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    var variant = BookItemVariant.PACKET_CODEC.decode(buf);
                    if (!variant.isBlank())
                        candidates.add(variant);
                }

                slotCandidates.add(candidates);
            }

            return new FillRecipePayload(syncId, slotCandidates);
        }

        @Override
        public Id<FillRecipePayload> getId() {
            return FILL_RECIPE;
        }
    }

    /**
     * Registers the payload types and the server-bound handlers. Runs from common init on both
     * physical sides.
     *
     * <p>Architectury splits clientbound registration in two: the client calls
     * {@code registerReceiver(S2C, ...)} (which registers the type and the handler together) and a
     * dedicated server calls {@code registerS2CPayloadType} for the type alone. Both register the
     * same payload id underneath, so exactly one of them may run per physical side - doing both on
     * a client is a duplicate registration.
     */
    public static void registerPayloads() {
        // Handlers run on the game thread on both loaders, so nothing has to be drained ahead of a
        // scheduled task.
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, ACTION, ActionPayload.CODEC, (payload, context) -> {
            var player = context.getPlayer();
            if (!(player.currentScreenHandler instanceof MysticalLecternScreenHandler lectern)
                    || lectern.syncId != payload.syncId() || !lectern.canUse(player))
                return;

            switch (payload.action()) {
                case ACTION_EXTRACT -> lectern.handleExtract(payload.variant(), payload.button(), payload.toInventory());
                case ACTION_INSERT -> lectern.handleInsert(payload.button());
            }
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, FILL_RECIPE, FillRecipePayload.CODEC, (payload, context) -> {
            var player = context.getPlayer();
            if (!(player.currentScreenHandler instanceof MysticalLecternScreenHandler lectern)
                    || lectern.syncId != payload.syncId() || !lectern.canUse(player))
                return;

            lectern.handleFillRecipe(payload.slotCandidates());
        });

        if (Platform.getEnvironment() == Env.SERVER)
            NetworkManager.registerS2CPayloadType(CONTENTS, ContentsPayload.CODEC);
    }

    public static void sendContents(ServerPlayerEntity player, int syncId, List<LibraryNetwork.Entry> entries) {
        NetworkManager.sendToPlayer(player, new ContentsPayload(syncId, entries));
    }
}
