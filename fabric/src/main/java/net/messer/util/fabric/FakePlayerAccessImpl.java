package net.messer.util.fabric;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public class FakePlayerAccessImpl {

    public static ServerPlayerEntity get(ServerWorld world) {
        return FakePlayer.get(world);
    }
}
