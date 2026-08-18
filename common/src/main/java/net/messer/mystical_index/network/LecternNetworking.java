package net.messer.mystical_index.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import io.netty.handler.codec.DecoderException;
import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.item.inventory.BookItemVariant;
import net.messer.mystical_index.item.inventory.LibraryNetwork;
import net.messer.mystical_index.screen.MysticalLecternScreenHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public class LecternNetworking {

    public static final CustomPacketPayload.Type<ContentsPayload> CONTENTS =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MysticalIndex.MOD_ID, "lectern_contents"));
    public static final CustomPacketPayload.Type<ActionPayload> ACTION =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MysticalIndex.MOD_ID, "lectern_action"));
    public static final CustomPacketPayload.Type<LinksPayload> LINKS =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MysticalIndex.MOD_ID, "lectern_links"));
    public static final CustomPacketPayload.Type<FillRecipePayload> FILL_RECIPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MysticalIndex.MOD_ID, "lectern_fill_recipe"));

    public static final int ACTION_EXTRACT = 0;
    public static final int ACTION_INSERT = 1;

    public static final int RECIPE_GRID_SIZE = 9;
    public static final int MAX_RECIPE_CANDIDATES = 8;

    // No real library network holds this many distinct variants; a bigger claim is a hostile peer
    // trying to make the reader allocate or spin before anything has been validated.
    // A lectern's discovery box is bounded by LecternRange, so the linked set is small by
    // construction; anything larger is a hostile peer rather than a big base.
    public static final int MAX_LINKED_LIBRARIES = 4096;

    public static final int MAX_CONTENTS_ENTRIES = 65536;
    private static final int MAX_CONTENTS_PRESIZE = 4096;

    // The codecs are written by hand rather than composed out of tuples: the action packet's shape
    // depends on its action byte, and the recipe packet caps candidates on read while still
    // draining everything that was written. Both wire formats are unchanged.
    public record ContentsPayload(int syncId, List<LibraryNetwork.Entry> entries,
                                  LibraryNetwork.Capacity capacity) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, ContentsPayload> CODEC =
                CustomPacketPayload.codec(ContentsPayload::write, ContentsPayload::read);

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(syncId);
            buf.writeVarInt(entries.size());
            for (var entry : entries) {
                BookItemVariant.PACKET_CODEC.encode(buf, entry.variant());
                buf.writeVarLong(entry.count());
            }
            // Session-local format, so the capacity simply rides along on the end.
            buf.writeVarInt(capacity.used());
            buf.writeVarInt(capacity.total());
            buf.writeVarInt(capacity.books());
            buf.writeVarInt(capacity.libraries());
        }

        private static ContentsPayload read(RegistryFriendlyByteBuf buf) {
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

            var capacity = new LibraryNetwork.Capacity(
                    buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
            return new ContentsPayload(syncId, entries, capacity);
        }

        @Override
        public Type<ContentsPayload> type() {
            return CONTENTS;
        }
    }

    /**
     * Where the lectern's network actually reaches: the libraries the SERVER linked, plus the
     * radius it used. The client renders exactly this and never re-discovers - a client-side scan
     * would drift from the server's answer the moment chunk loading differed.
     */
    public record LinksPayload(int syncId, BlockPos lectern, int radius, List<BlockPos> libraries)
            implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, LinksPayload> CODEC =
                CustomPacketPayload.codec(LinksPayload::write, LinksPayload::read);

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(syncId);
            buf.writeBlockPos(lectern);
            buf.writeVarInt(radius);
            buf.writeVarInt(libraries.size());
            for (var pos : libraries)
                buf.writeBlockPos(pos);
        }

        private static LinksPayload read(RegistryFriendlyByteBuf buf) {
            int syncId = buf.readVarInt();
            BlockPos lectern = buf.readBlockPos();
            int radius = buf.readVarInt();
            int size = buf.readVarInt();
            if (size < 0 || size > MAX_LINKED_LIBRARIES)
                throw new DecoderException("Lectern link count out of range: " + size);

            List<BlockPos> libraries = new ArrayList<>(Math.min(size, 256));
            for (int i = 0; i < size; i++)
                libraries.add(buf.readBlockPos());

            return new LinksPayload(syncId, lectern, radius, libraries);
        }

        @Override
        public Type<LinksPayload> type() {
            return LINKS;
        }
    }

    public record ActionPayload(int syncId, int action, BookItemVariant variant, int button, boolean toInventory)
            implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, ActionPayload> CODEC =
                CustomPacketPayload.codec(ActionPayload::write, ActionPayload::read);

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(syncId);
            buf.writeByte(action);

            if (action == ACTION_EXTRACT)
                BookItemVariant.PACKET_CODEC.encode(buf, variant);

            buf.writeByte(button);

            if (action == ACTION_EXTRACT)
                buf.writeBoolean(toInventory);
        }

        private static ActionPayload read(RegistryFriendlyByteBuf buf) {
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
        public Type<ActionPayload> type() {
            return ACTION;
        }
    }

    public record FillRecipePayload(int syncId, List<List<BookItemVariant>> slotCandidates) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, FillRecipePayload> CODEC =
                CustomPacketPayload.codec(FillRecipePayload::write, FillRecipePayload::read);

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(syncId);
            for (var candidates : slotCandidates) {
                buf.writeVarInt(candidates.size());
                for (var variant : candidates)
                    BookItemVariant.PACKET_CODEC.encode(buf, variant);
            }
        }

        private static FillRecipePayload read(RegistryFriendlyByteBuf buf) {
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
        public Type<FillRecipePayload> type() {
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
            if (!(player.containerMenu instanceof MysticalLecternScreenHandler lectern)
                    || lectern.containerId != payload.syncId() || !lectern.stillValid(player))
                return;

            switch (payload.action()) {
                case ACTION_EXTRACT -> lectern.handleExtract(payload.variant(), payload.button(), payload.toInventory());
                case ACTION_INSERT -> lectern.handleInsert(payload.button());
            }
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, FILL_RECIPE, FillRecipePayload.CODEC, (payload, context) -> {
            var player = context.getPlayer();
            if (!(player.containerMenu instanceof MysticalLecternScreenHandler lectern)
                    || lectern.containerId != payload.syncId() || !lectern.stillValid(player))
                return;

            lectern.handleFillRecipe(payload.slotCandidates());
        });

        if (Platform.getEnvironment() == Env.SERVER) {
            NetworkManager.registerS2CPayloadType(CONTENTS, ContentsPayload.CODEC);
            NetworkManager.registerS2CPayloadType(LINKS, LinksPayload.CODEC);
        }
    }

    public static void sendContents(ServerPlayer player, int syncId, List<LibraryNetwork.Entry> entries,
                                    LibraryNetwork.Capacity capacity) {
        NetworkManager.sendToPlayer(player, new ContentsPayload(syncId, entries, capacity));
    }

    public static void sendLinks(ServerPlayer player, int syncId, BlockPos lectern, int radius, List<BlockPos> libraries) {
        NetworkManager.sendToPlayer(player, new LinksPayload(syncId, lectern, radius, libraries));
    }
}
