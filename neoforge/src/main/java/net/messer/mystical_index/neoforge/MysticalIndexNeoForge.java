package net.messer.mystical_index.neoforge;

import eu.midnightdust.lib.config.MidnightConfig;
import net.messer.config.ModConfig;
import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.block.entity.ModBlockEntities;
import net.messer.mystical_index.neoforge.storage.LibraryItemHandler;
import net.messer.mystical_index.neoforge.storage.ScriptoriumItemHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@Mod(MysticalIndex.MOD_ID)
public class MysticalIndexNeoForge {

    public MysticalIndexNeoForge(IEventBus modBus, ModContainer container) {
        MysticalIndex.init();

        // MidnightConfig keeps one static entry map for every mod that uses it and iterates it while
        // loading. Mod constructors run in parallel here, so doing this during construction raced
        // another mod doing the same and threw a ConcurrentModificationException out of the library.
        // Common setup is both late enough to be off the construction threads and early enough for
        // MidnightLib itself, which first reads the registered configs in FMLClientSetupEvent;
        // enqueueWork then puts the call on the main thread, so it cannot race at all.
        modBus.addListener(FMLCommonSetupEvent.class, event ->
                event.enqueueWork(() -> MidnightConfig.init(MysticalIndex.MOD_ID, ModConfig.class)));

        modBus.addListener(RegisterCapabilitiesEvent.class, MysticalIndexNeoForge::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // The handler is created once per library and kept on the block entity, which rebuilds it
        // whenever its books change, so a neighbouring hopper is not handed a fresh wrapper on
        // every query.
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.SCRIPTORIUM_BLOCK_ENTITY.get(),
                (scriptorium, direction) -> {
                    if (scriptorium.getStorageView() instanceof ScriptoriumItemHandler existing)
                        return existing;

                    var handler = new ScriptoriumItemHandler(scriptorium);
                    scriptorium.setStorageView(handler);
                    return handler;
                });

        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.LIBRARY_BLOCK_ENTITY.get(),
                (library, direction) -> {
                    if (library.getStorageView() instanceof LibraryItemHandler existing)
                        return existing;

                    var handler = new LibraryItemHandler(library);
                    library.setStorageView(handler);
                    return handler;
                });
    }
}
