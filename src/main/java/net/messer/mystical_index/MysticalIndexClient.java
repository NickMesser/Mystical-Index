package net.messer.mystical_index;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;
import net.messer.mystical_index.screen.LibraryInventoryScreen;
import net.messer.mystical_index.screen.MagicalIndexScreen;
import net.messer.mystical_index.screen.ModScreenHandlers;

@Environment(EnvType.CLIENT)
public class MysticalIndexClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ScreenRegistry.register(ModScreenHandlers.LIBRARY_INVENTORY_SCREEN_HANDLER, LibraryInventoryScreen::new);
        ScreenRegistry.register(ModScreenHandlers.MAGICAL_INDEX_SCREEN_HANDLER, MagicalIndexScreen::new);
    }
}
