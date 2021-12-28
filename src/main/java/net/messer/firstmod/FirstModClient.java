package net.messer.firstmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;
import net.messer.firstmod.screen.LibraryInventoryScreen;
import net.messer.firstmod.screen.ModScreenHandlers;

@Environment(EnvType.CLIENT)
public class FirstModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ScreenRegistry.register(ModScreenHandlers.LIBRARY_INVENTORY_SCREEN_HANDLER, LibraryInventoryScreen::new);
    }
}
