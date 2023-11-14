package net.messer.mystical_index;

import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.messer.config.ModConfig;
import net.messer.mystical_index.block.ModBlocks;
import net.messer.mystical_index.block.entity.ModBlockEntities;
import net.messer.mystical_index.events.PlayerKillEvent;
import net.messer.mystical_index.item.ModItems;
import net.messer.mystical_index.recipe.ModRecipe;
import net.messer.mystical_index.recipe.PistonRecipeInitializer;
import net.messer.mystical_index.screen.ModScreenHandlers;
import net.minecraft.resource.ResourceType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MysticalIndex implements ModInitializer {

	public static final Logger LOGGER = LogManager.getLogger("mystical_index");
	public static final String MOD_ID = "mystical_index";

	@Override
	public void onInitialize() {
		MidnightConfig.init(MOD_ID, ModConfig.class);

		ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(PistonRecipeInitializer.getInstance());

		ModItems.registerModItems();
		ModBlocks.registerModBlocks();

		PlayerKillEvent.init();

		ModBlockEntities.registerBlockEntities();
		ModRecipe.registerRecipes();

		ModScreenHandlers.registerScreenHandlers();
	}
}
