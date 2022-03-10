package net.messer.mystical_index;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class MysticalIndexClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        //ScreenRegistry.register(ModScreenHandlers.LIBRARY_INVENTORY_SCREEN_HANDLER, LibraryInventoryScreen::new);
    }
}
