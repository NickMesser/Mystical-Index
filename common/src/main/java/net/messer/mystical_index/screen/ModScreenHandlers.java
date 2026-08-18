package net.messer.mystical_index.screen;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.messer.mystical_index.MysticalIndex;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;


public class ModScreenHandlers {

    public static final DeferredRegister<MenuType<?>> SCREEN_HANDLERS =
            DeferredRegister.create(MysticalIndex.MOD_ID, Registries.MENU);

    public static final RegistrySupplier<MenuType<LibraryInventoryScreenHandler>> LIBRARY_INVENTORY_SCREEN_HANDLER =
            SCREEN_HANDLERS.register("library_inventory", () ->
                    new MenuType<>(LibraryInventoryScreenHandler::new, FeatureFlags.VANILLA_SET));

    public static final RegistrySupplier<MenuType<MysticalLecternScreenHandler>> MYSTICAL_LECTERN_SCREEN_HANDLER =
            SCREEN_HANDLERS.register("mystical_lectern", () ->
                    new MenuType<>(MysticalLecternScreenHandler::new, FeatureFlags.VANILLA_SET));

    public static final RegistrySupplier<MenuType<FarmingBookScreenHandler>> FARMING_BOOK_SCREEN_HANDLER =
            SCREEN_HANDLERS.register("farming_book", () ->
                    new MenuType<>(FarmingBookScreenHandler::new, FeatureFlags.VANILLA_SET));

    public static final RegistrySupplier<MenuType<MagnetismScreenHandler>> MAGNETISM_SCREEN_HANDLER =
            SCREEN_HANDLERS.register("magnetism", () ->
                    new MenuType<>(MagnetismScreenHandler::new, FeatureFlags.VANILLA_SET));

    public static final RegistrySupplier<MenuType<SaturationScreenHandler>> SATURATION_SCREEN_HANDLER =
            SCREEN_HANDLERS.register("saturation", () ->
                    new MenuType<>(SaturationScreenHandler::new, FeatureFlags.VANILLA_SET));

    public static final RegistrySupplier<MenuType<BookSlingScreenHandler>> BOOK_SLING_SCREEN_HANDLER =
            SCREEN_HANDLERS.register("book_sling", () ->
                    new MenuType<>(BookSlingScreenHandler::new, FeatureFlags.VANILLA_SET));

    public static final RegistrySupplier<MenuType<ExperienceScreenHandler>> EXPERIENCE_SCREEN_HANDLER =
            SCREEN_HANDLERS.register("experience", () ->
                    new MenuType<>(ExperienceScreenHandler::new, FeatureFlags.VANILLA_SET));

    public static final RegistrySupplier<MenuType<ScriptoriumScreenHandler>> SCRIPTORIUM_SCREEN_HANDLER =
            SCREEN_HANDLERS.register("scriptorium", () ->
                    new MenuType<>(ScriptoriumScreenHandler::new, FeatureFlags.VANILLA_SET));

    public static void registerScreenHandlers() {
        SCREEN_HANDLERS.register();
    }
}
