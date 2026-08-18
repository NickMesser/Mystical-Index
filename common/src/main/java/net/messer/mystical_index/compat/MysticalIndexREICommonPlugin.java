package net.messer.mystical_index.compat;

import me.shedaniel.rei.api.common.display.DisplaySerializerRegistry;
import me.shedaniel.rei.api.common.plugins.REICommonPlugin;

/**
 * Display serializers are a common-side concern, and {@code REIClientPlugin} does not extend
 * {@code REICommonPlugin}, so the one registration the mod needs gets its own plugin rather than
 * being smuggled into the client one.
 */
public class MysticalIndexREICommonPlugin implements REICommonPlugin {

    @Override
    public void registerDisplaySerializer(DisplaySerializerRegistry registry) {
        registry.register(PistonCraftingDisplay.SERIALIZER_ID, PistonCraftingDisplay.SERIALIZER);
    }
}
