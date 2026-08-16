package net.messer.util.neoforge;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

public class FakePlayerAccessImpl {

    public static ServerPlayerEntity get(ServerWorld world) {
        return FakePlayerFactory.getMinecraft(world);
    }
}
