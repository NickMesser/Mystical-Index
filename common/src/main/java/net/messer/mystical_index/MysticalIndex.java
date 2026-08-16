package net.messer.mystical_index;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.registry.ReloadListenerRegistry;
import eu.midnightdust.lib.config.MidnightConfig;
import net.messer.config.ModConfig;
import net.messer.mystical_index.block.ModBlocks;
import net.messer.mystical_index.block.entity.ModBlockEntities;
import net.messer.mystical_index.events.PlayerKillEvent;
import net.messer.mystical_index.item.ModItems;
import net.messer.mystical_index.network.LecternNetworking;
import net.messer.mystical_index.recipe.ModRecipe;
import net.messer.mystical_index.recipe.PistonRecipeInitializer;
import net.messer.mystical_index.screen.ModScreenHandlers;
import net.messer.util.MysticalUtil;
import net.minecraft.resource.ResourceType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Shared init, called from each loader's entrypoint during mod construction.
 *
 * <p>Everything here has to run before the registries are frozen, because the deferred registers
 * below only queue their entries: NeoForge does not accept a registration outside its own event.
 */
public class MysticalIndex {

	public static final Logger LOGGER = LogManager.getLogger("mystical_index");
	public static final String MOD_ID = "mystical_index";

	public static void init() {
		MidnightConfig.init(MOD_ID, ModConfig.class);

		// Book inventories serialize nested item stacks, which needs a registry lookup that the
		// call sites (tooltips, glint checks) have no way to reach on their own.
		LifecycleEvent.SERVER_BEFORE_START.register(server -> MysticalUtil.setRegistryLookup(server.getRegistryManager()));

		ReloadListenerRegistry.register(ResourceType.SERVER_DATA,
				PistonRecipeInitializer.getInstance(), PistonRecipeInitializer.ID);

		ModItems.registerModItems();
		ModBlocks.registerModBlocks();

		PlayerKillEvent.init();

		ModBlockEntities.registerBlockEntities();
		ModRecipe.registerRecipes();

		ModScreenHandlers.registerScreenHandlers();
		LecternNetworking.registerPayloads();
	}
}
