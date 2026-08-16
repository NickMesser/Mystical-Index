package net.messer.util;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Sends a packet straight to one player's connection.
 *
 * <p>This exists because the two loaders' dev mappings disagree on the name of a single method.
 * The send lives on {@code ServerCommonNetworkHandler}, which yarn calls {@code sendPacket} but the
 * layered yarn-plus-NeoForge mapping leaves under its Mojang name {@code send}. Shared code
 * compiled against either name therefore cannot resolve on the other loader - and because the
 * mismatch is a method name rather than a class, the remapper does not catch it: it rewrites the
 * owner to {@code ServerGamePacketListenerImpl}, keeps the name {@code sendPacket}, and the result
 * only fails when the call is actually reached at runtime.
 *
 * <p>So the call is made once per loader, each compiled against the name its own mapping provides.
 * Both resolve to exactly the same vanilla method, so the packet and its timing are unchanged.
 */
public class ServerPacketSender {

    @ExpectPlatform
    public static void send(ServerPlayerEntity player, Packet<?> packet) {
        throw new AssertionError();
    }
}
