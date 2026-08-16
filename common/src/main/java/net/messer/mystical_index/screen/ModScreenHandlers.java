package net.messer.mystical_index.screen;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.messer.mystical_index.MysticalIndex;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;


public class ModScreenHandlers {

    public static final DeferredRegister<ScreenHandlerType<?>> SCREEN_HANDLERS =
            DeferredRegister.create(MysticalIndex.MOD_ID, RegistryKeys.SCREEN_HANDLER);

    public static final RegistrySupplier<ScreenHandlerType<LibraryInventoryScreenHandler>> LIBRARY_INVENTORY_SCREEN_HANDLER =
            SCREEN_HANDLERS.register("library_inventory", () ->
                    new ScreenHandlerType<>(LibraryInventoryScreenHandler::new, FeatureFlags.VANILLA_FEATURES));

    public static final RegistrySupplier<ScreenHandlerType<MysticalLecternScreenHandler>> MYSTICAL_LECTERN_SCREEN_HANDLER =
            SCREEN_HANDLERS.register("mystical_lectern", () ->
                    new ScreenHandlerType<>(MysticalLecternScreenHandler::new, FeatureFlags.VANILLA_FEATURES));

    public static void registerScreenHandlers() {
        SCREEN_HANDLERS.register();
    }
}
