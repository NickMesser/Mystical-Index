package net.messer.mystical_index;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.messer.config.MysticalConfig;
import net.messer.mystical_index.block.ModBlocks;
import net.messer.mystical_index.block.entity.ModBlockEntities;
import net.messer.mystical_index.item.ModItems;
import net.messer.mystical_index.item.ModRecipes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MysticalIndex implements ModInitializer {

	public static final Logger LOGGER = LogManager.getLogger("mystical_index");
	public static final String MOD_ID = "mystical_index";

	public static MysticalConfig CONFIG;

	@Override
	public void onInitialize() {
		AutoConfig.register(MysticalConfig.class, JanksonConfigSerializer::new);
		CONFIG = AutoConfig.getConfigHolder(MysticalConfig.class).getConfig();

		ModItems.registerModItems();
		ModBlocks.registerModBlocks();
		ModRecipes.registerModRecipes();

		ModBlockEntities.registerBlockEntities();
	}
}
