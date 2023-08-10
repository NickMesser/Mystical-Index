package net.messer.mystical_index;

import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.api.ModInitializer;
import net.messer.config.ModConfig;
import net.messer.mystical_index.block.ModBlocks;
import net.messer.mystical_index.block.entity.ModBlockEntities;
import net.messer.mystical_index.events.PlayerKillEvent;
import net.messer.mystical_index.item.ModItems;
import net.messer.mystical_index.recipe.ModRecipe;
import net.messer.mystical_index.screen.ModScreenHandlers;
import net.messer.mystical_index.screen.TestBlockGuiDescription;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.ScreenHandlerType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MysticalIndex implements ModInitializer {

	public static final Logger LOGGER = LogManager.getLogger("mystical_index");
	public static final String MOD_ID = "mystical_index";

	@Override
	public void onInitialize() {
		MidnightConfig.init(MOD_ID, ModConfig.class);

		ModItems.registerModItems();
		ModBlocks.registerModBlocks();

		PlayerKillEvent.init();

		ModBlockEntities.registerBlockEntities();
		ModRecipe.registerRecipes();

		ModScreenHandlers.registerScreenHandlers();
	}
}
