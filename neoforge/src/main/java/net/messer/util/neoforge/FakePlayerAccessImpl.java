package net.messer.util.neoforge;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

public class FakePlayerAccessImpl {

    public static ServerPlayer get(ServerLevel world) {
        return FakePlayerFactory.getMinecraft(world);
    }
}
