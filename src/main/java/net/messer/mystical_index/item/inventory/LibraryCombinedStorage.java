package net.messer.mystical_index.item.inventory;

import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

import java.util.List;

public class LibraryCombinedStorage extends CombinedStorage {
    public LibraryCombinedStorage(List parts) {
        super(parts);
    }

    @Override
    public long insert(Object resource, long maxAmount, TransactionContext transaction) {
        return 0;
    }

    public boolean tryInsert(){
        return false;
    }
}
