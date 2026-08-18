package net.messer.mystical_index;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.registry.ReloadListenerRegistry;
import net.messer.mystical_index.block.ModBlocks;
import net.messer.mystical_index.block.entity.ModBlockEntities;
import net.messer.mystical_index.events.PlayerKillEvent;
import net.messer.mystical_index.item.ModItems;
import net.messer.mystical_index.network.LecternNetworking;
import net.messer.mystical_index.recipe.ModRecipe;
import net.messer.mystical_index.recipe.PistonRecipeInitializer;
import net.messer.mystical_index.screen.ModScreenHandlers;
import net.messer.util.MysticalUtil;
import net.minecraft.server.packs.PackType;
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
		// MidnightConfig.init is deliberately NOT called here. It writes into static state shared by
		// every mod using the library, and NeoForge constructs mods in parallel on worker threads,
		// so calling it from shared construction raced another mod's registration and threw a
		// ConcurrentModificationException out of the library's own entry map. Each loader now runs
		// it at a phase where that cannot happen - see the two entrypoints.

		// Book inventories serialize nested item stacks, which needs a registry lookup that the
		// call sites (tooltips, glint checks) have no way to reach on their own.
		LifecycleEvent.SERVER_BEFORE_START.register(server -> MysticalUtil.setRegistryLookup(server.registryAccess()));

		ReloadListenerRegistry.register(PackType.SERVER_DATA,
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
