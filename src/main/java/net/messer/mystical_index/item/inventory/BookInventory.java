package net.messer.mystical_index.item.inventory;

import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.minecraft.inventory.SimpleInventory;

public class BookInventory {
    public final SimpleInventory inventory = new SimpleInventory(64){
        @Override
        public void markDirty() {
            this.markDirty();
        }
    };
    public final InventoryStorage inventoryWrapper = InventoryStorage.of(inventory, null);

    private void markDirty(){
        //inventory.markDirty();
    }

}
