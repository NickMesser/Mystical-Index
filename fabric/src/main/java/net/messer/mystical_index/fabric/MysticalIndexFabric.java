package net.messer.mystical_index.fabric;

import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.messer.config.ModConfig;
import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.block.entity.ModBlockEntities;
import net.messer.mystical_index.fabric.storage.LibraryItemStorage;
import net.messer.mystical_index.fabric.storage.ScriptoriumItemStorage;

public class MysticalIndexFabric implements ModInitializer {

	@Override
	public void onInitialize() {
		// Fabric initializes mods one at a time on a single thread, so the library's shared static
		// state cannot be raced here. This is where it has always run.
		MidnightConfig.init(MysticalIndex.MOD_ID, ModConfig.class);

		MysticalIndex.init();

		// Deferred until the block entity type actually exists. The storage view is created once per
		// library and then kept on the block entity, which rebuilds it whenever its books change.
		ModBlockEntities.SCRIPTORIUM_BLOCK_ENTITY.listen(type ->
				ItemStorage.SIDED.registerForBlockEntity((scriptorium, direction) -> {
					if (scriptorium.getStorageView() instanceof ScriptoriumItemStorage existing)
						return existing.storage();

					var storage = new ScriptoriumItemStorage(scriptorium);
					scriptorium.setStorageView(storage);
					return storage.storage();
				}, type));

		ModBlockEntities.LIBRARY_BLOCK_ENTITY.listen(type ->
				ItemStorage.SIDED.registerForBlockEntity((library, direction) -> {
					if (library.getStorageView() instanceof LibraryItemStorage existing)
						return existing.storage();

					var storage = new LibraryItemStorage(library);
					library.setStorageView(storage);
					return storage.storage();
				}, type));
	}
}
