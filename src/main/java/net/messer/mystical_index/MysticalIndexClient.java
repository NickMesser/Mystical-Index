package net.messer.mystical_index;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;
import net.messer.mystical_index.screen.LibraryInventoryScreen;
import net.messer.mystical_index.screen.ModScreenHandlers;
import net.messer.mystical_index.screen.TestBlockGuiDescription;
import net.messer.mystical_index.screen.TestBlockScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

@Environment(EnvType.CLIENT)
public class MysticalIndexClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(ModScreenHandlers.LIBRARY_INVENTORY_SCREEN_HANDLER, LibraryInventoryScreen::new);
        HandledScreens.<TestBlockGuiDescription, TestBlockScreen>register(ModScreenHandlers.TEST_BLOCK_SCREEN_HANDLER, (gui, inventory, title) -> new TestBlockScreen(gui, inventory.player, title));
    }
}
