package net.messer.mystical_index.screen;

import net.messer.mystical_index.MysticalIndex;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;


public class ModScreenHandlers {

    public static ScreenHandlerType<LibraryInventoryScreenHandler> LIBRARY_INVENTORY_SCREEN_HANDLER;
    public static ScreenHandlerType<MysticalLecternScreenHandler> MYSTICAL_LECTERN_SCREEN_HANDLER;
    public static void registerScreenHandlers() {
        LIBRARY_INVENTORY_SCREEN_HANDLER = Registry.register(Registries.SCREEN_HANDLER,
                Identifier.of(MysticalIndex.MOD_ID, "library_inventory"),
                new ScreenHandlerType<>(LibraryInventoryScreenHandler::new, FeatureFlags.VANILLA_FEATURES));
        MYSTICAL_LECTERN_SCREEN_HANDLER = Registry.register(Registries.SCREEN_HANDLER,
                Identifier.of(MysticalIndex.MOD_ID, "mystical_lectern"),
                new ScreenHandlerType<>(MysticalLecternScreenHandler::new, FeatureFlags.VANILLA_FEATURES));
    }
}
