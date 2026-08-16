package net.messer.mystical_index.block.entity;

import net.messer.mystical_index.item.custom.base_books.BaseStorageBook;
import net.messer.mystical_index.item.inventory.LibraryNetwork;
import net.messer.mystical_index.item.inventory.SimpleBookInventory;
import net.messer.mystical_index.screen.LibraryInventoryScreenHandler;
import net.messer.util.MysticalUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;


public class LibraryBlockEntity extends BlockEntity implements NamedScreenHandlerFactory {

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

    public SimpleInventory storedBooks = new SimpleInventory(5) {
        @Override
        public void markDirty() {
            bookInventories.clear();
            for (var itemStack : storedBooks.getHeldStacks()) {
                if(itemStack.getItem() instanceof BaseStorageBook storageBook){
                    if(storageBook.getInventory(itemStack).isEmpty())
                        continue;
                }
                bookInventories.add(new SimpleBookInventory(itemStack));
                LibraryBlockEntity.this.markDirty();
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
     * How much of {@code stack} an {@link #insert} would take, without touching anything.
     *
     * <p>NeoForge's item handler asks this on every {@code simulate = true} call, and a simulated
     * insert must be side effect free. Rather than duplicating the two-pass fill rules (which would
     * be free to drift out of step with the real ones), the real insert runs against copies of the
     * book stacks: same code, same answer, nothing observable changed.
     */
    public long simulateInsert(ItemStack stack) {
        var copies = new ArrayList<ItemStack>(storedBooks.getHeldStacks().size());
        for (var book : storedBooks.getHeldStacks())
            copies.add(book.copy());

        return LibraryNetwork.insertIntoBooks(copies, stack.copy());
    }

    /**
     * Captures every stored book's custom data so a rejected insert can put it back.
     *
     * <p>Writes land straight in the books' components, so a transfer API that lets a mod stage an
     * insert and then discard it (Fabric's transactions do exactly that when simulating) would
     * otherwise leave the items written while the source still had them - a dupe worth tens of
     * items a second through a pipe.
     */
    public NbtCompound[] snapshotBooks() {
        var stacks = storedBooks.getHeldStacks();
        var snapshot = new NbtCompound[stacks.size()];
        for (int i = 0; i < stacks.size(); i++) {
            var nbt = MysticalUtil.getCustomData(stacks.get(i));
            snapshot[i] = nbt == null ? null : nbt.copy();
        }
        return snapshot;
    }

    public void restoreBooks(NbtCompound[] snapshot) {
        var stacks = storedBooks.getHeldStacks();
        for (int i = 0; i < snapshot.length && i < stacks.size(); i++) {
            var stack = stacks.get(i);
            // A book that carried no component before the write must end up with none again,
            // or "has been used" reads would flip on an aborted insert.
            if (snapshot[i] == null)
                stack.remove(DataComponentTypes.CUSTOM_DATA);
            else
                MysticalUtil.setCustomData(stack, snapshot[i].copy());
        }
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        storedBooks.clear();
        Inventories.readNbt(nbt, storedBooks.getHeldStacks(), registryLookup);
        storedBooks.markDirty();
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        Inventories.writeNbt(nbt, storedBooks.getHeldStacks(), registryLookup);
    }

    public LibraryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LIBRARY_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public Text getDisplayName() {
        return Text.literal("Library");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new LibraryInventoryScreenHandler(syncId, inv, storedBooks,
                ScreenHandlerContext.create(getWorld(), getPos()));
    }

    public static void tick(World world, BlockPos pos, BlockState state, LibraryBlockEntity be) {
        var storedBooks = be.storedBooks;
        for (var book : storedBooks.getHeldStacks()) {
            if (book.getItem() instanceof BaseStorageBook storageBook) {
                storageBook.customBookTick(book, world, be);
            }
        }
        storedBooks.markDirty();
        return;
    }
}
