package net.messer.util.fabric;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;

public class FakePlayerAccessImpl {

    public static ServerPlayer get(ServerLevel world) {
        return FakePlayer.get(world);
    }
}
