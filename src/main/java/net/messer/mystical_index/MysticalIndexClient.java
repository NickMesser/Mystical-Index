package net.messer.mystical_index;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;
import net.messer.mystical_index.item.inventory.BookContentsTooltipData;
import net.messer.mystical_index.network.LecternClientNetworking;
import net.messer.mystical_index.screen.BookContentsTooltipComponent;
import net.messer.mystical_index.screen.LibraryInventoryScreen;
import net.messer.mystical_index.screen.ModScreenHandlers;
import net.messer.mystical_index.screen.MysticalLecternScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

@Environment(EnvType.CLIENT)
public class MysticalIndexClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(ModScreenHandlers.LIBRARY_INVENTORY_SCREEN_HANDLER, LibraryInventoryScreen::new);
        HandledScreens.register(ModScreenHandlers.MYSTICAL_LECTERN_SCREEN_HANDLER, MysticalLecternScreen::new);

        // Vanilla's TooltipComponent.of throws on a TooltipData it does not know, so this mapping
        // is what keeps hovering any of the mod's books from crashing.
        TooltipComponentCallback.EVENT.register(data ->
                data instanceof BookContentsTooltipData contents ? new BookContentsTooltipComponent(contents) : null);

        LecternClientNetworking.registerClientReceivers();
    }
}
