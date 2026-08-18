package net.messer.util;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;

/**
 * A stand-in player for actions the mod performs with nobody actually holding the item: crafting
 * results out of a piston, and rolling an entity's loot table for a book that farms it.
 *
 * <p>Both loaders ship one, but under unrelated names ({@code FakePlayer.get} on Fabric,
 * {@code FakePlayerFactory} on NeoForge), so the lookup is resolved per platform.
 */
public class FakePlayerAccess {

    @ExpectPlatform
    public static ServerPlayer get(ServerLevel world) {
        throw new AssertionError();
    }
}
