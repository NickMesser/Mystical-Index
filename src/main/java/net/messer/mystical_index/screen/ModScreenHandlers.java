package net.messer.mystical_index.screen;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.messer.mystical_index.MysticalIndex;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;


public class ModScreenHandlers {

    public static ScreenHandlerType<LibraryInventoryScreenHandler> LIBRARY_INVENTORY_SCREEN_HANDLER;
    public static void registerScreenHandlers() {
        LIBRARY_INVENTORY_SCREEN_HANDLER = ScreenHandlerRegistry.registerSimple(new Identifier(MysticalIndex.MOD_ID, "library_inventory"), LibraryInventoryScreenHandler::new);
    }
}
