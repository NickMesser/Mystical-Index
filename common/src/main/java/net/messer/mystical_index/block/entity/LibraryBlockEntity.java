package net.messer.mystical_index.block.entity;

import net.messer.mystical_index.item.custom.base_books.BaseStorageBook;
import net.messer.mystical_index.item.inventory.LibraryNetwork;
import net.messer.mystical_index.item.inventory.SimpleBookInventory;
import net.messer.mystical_index.screen.LibraryInventoryScreenHandler;
import net.messer.util.MysticalUtil;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;


import net.minecraft.world.level.storage.ValueInput;

import net.minecraft.world.level.storage.ValueOutput;

public class LibraryBlockEntity extends BlockEntity implements MenuProvider {

    /**
     * The per-loader item-handler view of this library. Fabric hangs a {@code Storage<ItemVariant>}
     * on it and NeoForge an {@code IItemHandler}; both are built over {@link #bookInventories} and
     * are told to rebuild whenever the stored books change, so a query never has to reconstruct the
     * whole view.
     */
    public interface StorageView {
        void rebuild();
    }

    // One mirror per stored book slot, rebuilt on every markDirty. Loader-free: the storage wrapper
    // that used to be baked in here is now built on top of this list by each loader module.
    public final List<SimpleBookInventory> bookInventories = new ArrayList<>();

    @Nullable
    private StorageView storageView;

    public SimpleContainer storedBooks = new SimpleContainer(5) {
        @Override
        public void setChanged() {
            bookInventories.clear();
            for (var itemStack : storedBooks.getItems()) {
                if(itemStack.getItem() instanceof BaseStorageBook storageBook){
                    if(storageBook.getInventory(itemStack).isEmpty())
                        continue;
                }
                bookInventories.add(new SimpleBookInventory(itemStack));
                LibraryBlockEntity.this.setChanged();
            }

            if (storageView != null)
                storageView.rebuild();
        }
    };

    public void setStorageView(StorageView view) {
        this.storageView = view;
        view.rebuild();
    }

    @Nullable
    public StorageView getStorageView() {
        return storageView;
    }

    /**
     * Inserts as much of {@code stack} as the stored books accept, mutating it downward, and
     * returns the amount taken. Books already bound to this item fill first, then an unbound Book
     * of Holding claims the rest.
     */
    public long insert(ItemStack stack) {
        return LibraryNetwork.insertIntoLibrary(this, stack);
    }


    /**
     * Captures every stored book so a rejected insert can put it back exactly as it was.
     *
     * <p>Writes land straight in the books' components, so a transfer API that lets a mod stage an
     * insert and then discard it (both loaders' transactions do exactly that when simulating) would
     * otherwise leave the items written while the source still had them - a dupe worth tens of
     * items a second through a pipe.
     *
     * <p>Whole stacks are copied rather than just custom_data. The write path derives other
     * components from the contents - the enchantment glint override, and the entity paper's dyed
     * colour - so a snapshot narrower than the full component set can restore the items and still
     * leave a book visibly "used" after an aborted insert.
     */
    public ItemStack[] snapshotBooks() {
        var stacks = storedBooks.getItems();
        var snapshot = new ItemStack[stacks.size()];
        for (int i = 0; i < stacks.size(); i++)
            snapshot[i] = stacks.get(i).copy();
        return snapshot;
    }

    public void restoreBooks(ItemStack[] snapshot) {
        var stacks = storedBooks.getItems();
        for (int i = 0; i < snapshot.length && i < stacks.size(); i++)
            stacks.set(i, snapshot[i].copy());

        // The mirrors in bookInventories hold the stack objects that were just replaced, so they
        // have to be rebuilt or the next query reads the rolled-back-over copies.
        storedBooks.setChanged();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        storedBooks.clearContent();
        ContainerHelper.loadAllItems(input, storedBooks.getItems());
        storedBooks.setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, storedBooks.getItems());
    }

    public LibraryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LIBRARY_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Library");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
        return new LibraryInventoryScreenHandler(syncId, inv, storedBooks,
                ContainerLevelAccess.create(getLevel(), getBlockPos()));
    }

    // No tick. A Library is storage only: books do not work while stored in one - the Scriptorium
    // is where they run. Removing the ticker also removed the blanket per-tick setChanged that was
    // silently keeping the transfer mirrors fresh, so every network mutation now marks the library
    // itself (see LibraryNetwork.markLibraryChanged).
}
