package net.messer.mystical_index.neoforge;

import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.block.entity.ModBlockEntities;
import net.messer.mystical_index.neoforge.storage.LibraryItemHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@Mod(MysticalIndex.MOD_ID)
public class MysticalIndexNeoForge {

    public MysticalIndexNeoForge(IEventBus modBus, ModContainer container) {
        MysticalIndex.init();

        modBus.addListener(RegisterCapabilitiesEvent.class, MysticalIndexNeoForge::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // The handler is created once per library and kept on the block entity, which rebuilds it
        // whenever its books change, so a neighbouring hopper is not handed a fresh wrapper on
        // every query.
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.LIBRARY_BLOCK_ENTITY.get(),
                (library, direction) -> {
                    if (library.getStorageView() instanceof LibraryItemHandler existing)
                        return existing;

                    var handler = new LibraryItemHandler(library);
                    library.setStorageView(handler);
                    return handler;
                });
    }
}
