package net.messer.mystical_index.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.block.entity.ModBlockEntities;
import net.messer.mystical_index.fabric.storage.LibraryItemStorage;

public class MysticalIndexFabric implements ModInitializer {

	@Override
	public void onInitialize() {
		MysticalIndex.init();

		// Deferred until the block entity type actually exists. The storage view is created once per
		// library and then kept on the block entity, which rebuilds it whenever its books change.
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
