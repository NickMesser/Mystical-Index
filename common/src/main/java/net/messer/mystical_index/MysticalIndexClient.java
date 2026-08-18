package net.messer.mystical_index;

import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import net.messer.mystical_index.client.LecternRangeVisualizer;
import dev.architectury.registry.client.gui.ClientTooltipComponentRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.messer.mystical_index.item.inventory.BookContentsTooltipData;
import net.messer.mystical_index.network.LecternClientNetworking;
import net.messer.mystical_index.screen.BookContentsTooltipComponent;
import net.messer.mystical_index.screen.FarmingBookScreen;
import net.messer.mystical_index.screen.BookSlingScreen;
import net.messer.mystical_index.screen.ExperienceScreen;
import net.messer.mystical_index.screen.ScriptoriumScreen;
import net.messer.mystical_index.screen.MagnetismScreen;
import net.messer.mystical_index.screen.SaturationScreen;
import net.messer.mystical_index.screen.LibraryInventoryScreen;
import net.messer.mystical_index.screen.ModScreenHandlers;
import net.messer.mystical_index.screen.MysticalLecternScreen;
import net.messer.util.MenuScreenRegistry;
import net.messer.util.MysticalUtil;
import net.minecraft.client.Minecraft;

/**
 * Shared client init, called from each loader's client entrypoint during mod construction.
 *
 * <p>The screen factories go through {@code listen} rather than reading the handler types straight
 * out of the suppliers: this runs while the registries are still being filled on NeoForge, so the
 * types do not exist yet, and the callback simply fires the moment they do.
 */
@Environment(EnvType.CLIENT)
public class MysticalIndexClient {

    public static void init() {
        ModScreenHandlers.LIBRARY_INVENTORY_SCREEN_HANDLER.listen(type ->
                MenuScreenRegistry.register(type, LibraryInventoryScreen::new));
        ModScreenHandlers.MYSTICAL_LECTERN_SCREEN_HANDLER.listen(type ->
                MenuScreenRegistry.register(type, MysticalLecternScreen::new));
        ModScreenHandlers.FARMING_BOOK_SCREEN_HANDLER.listen(type ->
                MenuScreenRegistry.register(type, FarmingBookScreen::new));
        ModScreenHandlers.MAGNETISM_SCREEN_HANDLER.listen(type ->
                MenuScreenRegistry.register(type, MagnetismScreen::new));
        ModScreenHandlers.BOOK_SLING_SCREEN_HANDLER.listen(type ->
                MenuScreenRegistry.register(type, BookSlingScreen::new));
        ModScreenHandlers.EXPERIENCE_SCREEN_HANDLER.listen(type ->
                MenuScreenRegistry.register(type, ExperienceScreen::new));
        ModScreenHandlers.SCRIPTORIUM_SCREEN_HANDLER.listen(type ->
                MenuScreenRegistry.register(type, ScriptoriumScreen::new));
        ModScreenHandlers.SATURATION_SCREEN_HANDLER.listen(type ->
                MenuScreenRegistry.register(type, SaturationScreen::new));

        // Vanilla's TooltipComponent.of throws on a TooltipData it does not know, so this mapping
        // is what keeps hovering any of the mod's books from crashing.
        ClientTooltipComponentRegistry.register(BookContentsTooltipData.class, BookContentsTooltipComponent::new);

        // appendTooltip no longer receives a World, and item stacks nested in a book need a
        // registry lookup to decode. Both come off the client's world once it is loaded.
        ClientLifecycleEvent.CLIENT_LEVEL_LOAD.register(world -> MysticalUtil.setRegistryLookup(world.registryAccess()));

        // Drives the lectern range overlay. CLIENT_LEVEL_POST carries the ClientLevel, which the
        // visualizer needs both to spawn particles and to notice a lectern that is no longer there.
        ClientTickEvent.CLIENT_LEVEL_POST.register(LecternRangeVisualizer::tick);
        MysticalUtil.setTooltipWorldSupplier(() -> Minecraft.getInstance().level);

        LecternClientNetworking.registerClientReceivers();

        // Item colour providers live on the client only; registering here keeps a dedicated
        // server from touching the client-only colour registry.
    }
}
