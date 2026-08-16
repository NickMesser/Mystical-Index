package net.messer.util.neoforge;

import net.minecraft.network.packet.Packet;
import net.minecraft.server.network.ServerPlayerEntity;

public class ServerPacketSenderImpl {

    public static void send(ServerPlayerEntity player, Packet<?> packet) {
        // Same method Fabric reaches through sendPacket: the layered NeoForge mapping keeps it
        // under its Mojang name.
        player.networkHandler.send(packet);
    }
}
