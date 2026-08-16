package net.messer.mystical_index.neoforge.compat;

import me.shedaniel.rei.forge.REIPluginClient;
import net.messer.mystical_index.compat.MysticalIndexREIClientPlugin;

/**
 * REI finds client plugins on NeoForge by scanning FML's annotation data for {@code @REIPluginClient},
 * where Fabric uses the {@code rei_client} entrypoint. The plugin itself is shared; this subclass
 * exists only to carry the annotation, which lives in a NeoForge-only REI package.
 */
@REIPluginClient
public class MysticalIndexREIClientPluginNeoForge extends MysticalIndexREIClientPlugin {
}
