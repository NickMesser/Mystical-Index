package net.messer.firstmod.screen;

import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.messer.firstmod.FirstMod;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public class ModScreenHandlers {
    public static ScreenHandlerType<LibraryInventoryScreenHandler> LIBRARY_INVENTORY_SCREEN_HANDLER =
            ScreenHandlerRegistry.registerSimple(new Identifier(FirstMod.MOD_ID, "library"),
                    LibraryInventoryScreenHandler::new);
}
