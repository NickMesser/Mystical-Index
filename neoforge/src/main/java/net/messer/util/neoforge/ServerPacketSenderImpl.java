package net.messer.util.neoforge;

import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;

public class ServerPacketSenderImpl {

    public static void send(ServerPlayer player, Packet<?> packet) {
        // Same method Fabric reaches through sendPacket: the layered NeoForge mapping keeps it
        // under its Mojang name.
        player.connection.send(packet);
    }
}
