package net.messer.mystical_index.neoforge.compat;

import me.shedaniel.rei.forge.REIPluginCommon;
import net.messer.mystical_index.compat.MysticalIndexREICommonPlugin;

/**
 * The common-plugin counterpart of {@code MysticalIndexREIClientPluginNeoForge}: NeoForge discovers
 * REI plugins by annotation, and the common one has its own marker.
 */
@REIPluginCommon
public class MysticalIndexREICommonPluginNeoForge extends MysticalIndexREICommonPlugin {
}
