package net.messer.mystical_index.item.inventory;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;

import java.util.List;

// Properly typed so the insert override reads ItemVariant/long directly instead of casting to a
// Fabric impl class and truncating the amount. The real, transaction-aware insert lives in the
// anonymous subclass created by LibraryBlockEntity; on its own this delegates to its parts.
public class LibraryCombinedStorage extends CombinedStorage<ItemVariant, Storage<ItemVariant>> {
    public LibraryCombinedStorage(List<Storage<ItemVariant>> parts) {
        super(parts);
    }
}
