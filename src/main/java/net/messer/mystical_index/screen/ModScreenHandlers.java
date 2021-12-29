package net.messer.mystical_index.screen;

import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.messer.mystical_index.MysticalIndex;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public class ModScreenHandlers {
    public static ScreenHandlerType<LibraryInventoryScreenHandler> LIBRARY_INVENTORY_SCREEN_HANDLER =
            ScreenHandlerRegistry.registerSimple(new Identifier(MysticalIndex.MOD_ID, "library"),
                    LibraryInventoryScreenHandler::new);

    public static ScreenHandlerType<MagicalIndexScreenHandler> MAGICAL_INDEX_SCREEN_HANDLER =
            ScreenHandlerRegistry.registerSimple(new Identifier(MysticalIndex.MOD_ID, "magical_index"),
                    MagicalIndexScreenHandler::new);
}
