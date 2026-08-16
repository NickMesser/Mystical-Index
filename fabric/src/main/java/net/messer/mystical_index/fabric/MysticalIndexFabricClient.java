package net.messer.mystical_index.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.messer.mystical_index.MysticalIndexClient;

@Environment(EnvType.CLIENT)
public class MysticalIndexFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        MysticalIndexClient.init();
    }
}
