package net.messer.mystical_index.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.messer.mystical_index.MysticalIndex;
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
                ItemVariant.PACKET_CODEC.encode(buf, entry.variant());
                buf.writeVarLong(entry.count());
            }
        }

        private static ContentsPayload read(RegistryByteBuf buf) {
            int syncId = buf.readVarInt();
            int size = buf.readVarInt();

            List<LibraryNetwork.Entry> entries = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                var variant = ItemVariant.PACKET_CODEC.decode(buf);
                entries.add(new LibraryNetwork.Entry(variant, buf.readVarLong()));
            }

            return new ContentsPayload(syncId, entries);
        }

        @Override
        public Id<ContentsPayload> getId() {
            return CONTENTS;
        }
    }

    public record ActionPayload(int syncId, int action, ItemVariant variant, int button, boolean toInventory)
            implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, ActionPayload> CODEC =
                CustomPayload.codecOf(ActionPayload::write, ActionPayload::read);

        private void write(RegistryByteBuf buf) {
            buf.writeVarInt(syncId);
            buf.writeByte(action);

            if (action == ACTION_EXTRACT)
                ItemVariant.PACKET_CODEC.encode(buf, variant);

            buf.writeByte(button);

            if (action == ACTION_EXTRACT)
                buf.writeBoolean(toInventory);
        }

        private static ActionPayload read(RegistryByteBuf buf) {
            int syncId = buf.readVarInt();
            int action = buf.readByte();

            ItemVariant variant = ItemVariant.blank();
            if (action == ACTION_EXTRACT)
                variant = ItemVariant.PACKET_CODEC.decode(buf);

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

    public record FillRecipePayload(int syncId, List<List<ItemVariant>> slotCandidates) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, FillRecipePayload> CODEC =
                CustomPayload.codecOf(FillRecipePayload::write, FillRecipePayload::read);

        private void write(RegistryByteBuf buf) {
            buf.writeVarInt(syncId);
            for (var candidates : slotCandidates) {
                buf.writeVarInt(candidates.size());
                for (var variant : candidates)
                    ItemVariant.PACKET_CODEC.encode(buf, variant);
            }
        }

        private static FillRecipePayload read(RegistryByteBuf buf) {
            int syncId = buf.readVarInt();

            List<List<ItemVariant>> slotCandidates = new ArrayList<>(RECIPE_GRID_SIZE);
            for (int slot = 0; slot < RECIPE_GRID_SIZE; slot++) {
                int count = buf.readVarInt();
                List<ItemVariant> candidates = new ArrayList<>();

                for (int i = 0; i < count; i++) {
                    // Everything written has to be read even past the cap, or the rest of the
                    // packet is misaligned. The surplus is simply dropped.
                    var variant = ItemVariant.PACKET_CODEC.decode(buf);
                    if (i < MAX_RECIPE_CANDIDATES && !variant.isBlank())
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

    public static void registerPayloads() {
        PayloadTypeRegistry.playS2C().register(CONTENTS, ContentsPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ACTION, ActionPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(FILL_RECIPE, FillRecipePayload.CODEC);
    }

    public static void registerServerReceivers() {
        // Payload handlers already run on the server thread, so nothing has to be drained ahead
        // of a scheduled task any more.
        ServerPlayNetworking.registerGlobalReceiver(ACTION, (payload, context) -> {
            var player = context.player();
            if (!(player.currentScreenHandler instanceof MysticalLecternScreenHandler lectern)
                    || lectern.syncId != payload.syncId() || !lectern.canUse(player))
                return;

            if (payload.action() == ACTION_EXTRACT)
                lectern.handleExtract(payload.variant(), payload.button(), payload.toInventory());
            else
                lectern.handleInsert(payload.button());
        });

        ServerPlayNetworking.registerGlobalReceiver(FILL_RECIPE, (payload, context) -> {
            var player = context.player();
            if (!(player.currentScreenHandler instanceof MysticalLecternScreenHandler lectern)
                    || lectern.syncId != payload.syncId() || !lectern.canUse(player))
                return;

            lectern.handleFillRecipe(payload.slotCandidates());
        });
    }

    public static void sendContents(ServerPlayerEntity player, int syncId, List<LibraryNetwork.Entry> entries) {
        ServerPlayNetworking.send(player, new ContentsPayload(syncId, entries));
    }
}
