package net.messer.util.fabric;

import net.minecraft.network.packet.Packet;
import net.minecraft.server.network.ServerPlayerEntity;

public class ServerPacketSenderImpl {

    public static void send(ServerPlayerEntity player, Packet<?> packet) {
        player.networkHandler.sendPacket(packet);
    }
}
