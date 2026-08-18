package net.messer.util.fabric;

import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;

public class ServerPacketSenderImpl {

    public static void send(ServerPlayer player, Packet<?> packet) {
        player.connection.send(packet);
    }
}
