package net.messer.mystical_index;

import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.registry.client.gui.ClientTooltipComponentRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.messer.mystical_index.item.inventory.BookContentsTooltipData;
import net.messer.mystical_index.item.provider.PaperColorProvider;
import net.messer.mystical_index.network.LecternClientNetworking;
import net.messer.mystical_index.screen.BookContentsTooltipComponent;
import net.messer.mystical_index.screen.LibraryInventoryScreen;
import net.messer.mystical_index.screen.ModScreenHandlers;
import net.messer.mystical_index.screen.MysticalLecternScreen;
import net.messer.util.MysticalUtil;
import net.minecraft.client.MinecraftClient;

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
                MenuRegistry.registerScreenFactory(type, LibraryInventoryScreen::new));
        ModScreenHandlers.MYSTICAL_LECTERN_SCREEN_HANDLER.listen(type ->
                MenuRegistry.registerScreenFactory(type, MysticalLecternScreen::new));

        // Vanilla's TooltipComponent.of throws on a TooltipData it does not know, so this mapping
        // is what keeps hovering any of the mod's books from crashing.
        ClientTooltipComponentRegistry.register(BookContentsTooltipData.class, BookContentsTooltipComponent::new);

        // appendTooltip no longer receives a World, and item stacks nested in a book need a
        // registry lookup to decode. Both come off the client's world once it is loaded.
        ClientLifecycleEvent.CLIENT_LEVEL_LOAD.register(world -> MysticalUtil.setRegistryLookup(world.getRegistryManager()));
        MysticalUtil.setTooltipWorldSupplier(() -> MinecraftClient.getInstance().world);

        LecternClientNetworking.registerClientReceivers();

        // Item colour providers live on the client only; registering here keeps a dedicated
        // server from touching the client-only colour registry.
        PaperColorProvider.register();
    }
}
