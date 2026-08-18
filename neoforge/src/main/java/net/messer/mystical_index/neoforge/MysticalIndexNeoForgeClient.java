package net.messer.mystical_index.neoforge;

import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.MysticalIndexClient;
import net.messer.util.neoforge.MenuScreenRegistryImpl;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * The client half of the mod. A second {@code @Mod} entrypoint restricted to the client dist is the
 * NeoForge counterpart of Fabric's client entrypoint: it runs during mod construction, which is
 * where the client registries Architectury wraps all want to be told about their listeners.
 */
@Mod(value = MysticalIndex.MOD_ID, dist = Dist.CLIENT)
public class MysticalIndexNeoForgeClient {

    public MysticalIndexNeoForgeClient(IEventBus modEventBus) {
        MysticalIndexClient.init();

        // Screen bindings requested during the init above are parked until this event, which is the
        // only place NeoForge accepts them.
        modEventBus.addListener(RegisterMenuScreensEvent.class, MenuScreenRegistryImpl::drain);
    }
}
