package net.messer.mystical_index.block.entity;

import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.messer.mystical_index.item.custom.BaseStorageBook;
import net.messer.mystical_index.screen.LibraryInventoryScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.Dictionary;
import java.util.Hashtable;


public class LibraryBlockEntity extends BlockEntity implements NamedScreenHandlerFactory {

    public SimpleInventory storedBooks = new SimpleInventory(5){
        @Override
        public void markDirty() {
            for (var itemStack : storedBooks.stacks) {
                if (itemStack.getItem() instanceof BaseStorageBook storageBook) {
                    var content = storageBook.getInventory(itemStack);
                    if (content.isEmpty())
                        continue;
                    for (int i = 0; i < content.size(); i++) {
                        if (content.getStack(i).isEmpty())
                            continue;
                        contents.setStack(i, content.getStack(i));
                    }
                    LibraryBlockEntity.this.markDirty();
                    break;
                }
            }
            LibraryBlockEntity.this.markDirty();
        }
    };

    public InventoryStorage storedContents(){
        storedBooks.markDirty();
        return InventoryStorage.of(contents, null);
    }

    public SimpleInventory contents = new SimpleInventory(64){
        @Override
        public void markDirty() {
            for (var itemStack: storedBooks.stacks) {
                if(itemStack.getItem() instanceof BaseStorageBook storageBook){
                    var content = storageBook.getInventory(itemStack);
                    if(content.isEmpty())
                        continue;
                    content.clear();
                    for (int i = 0; i < contents.size(); i++) {
                        if(contents.getStack(i).isEmpty())
                            continue;
                        content.setStack(i, contents.getStack(i));
                    }
                    content.markDirty();
                    LibraryBlockEntity.this.markDirty();
                    break;
                }
            }
            LibraryBlockEntity.this.markDirty();
        }
    };

    public final InventoryStorage bookWrapper = InventoryStorage.of(storedBooks, null);
    public final InventoryStorage contentWrapper = InventoryStorage.of(contents, null);


    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        storedBooks.clear();
        Inventories.readNbt(nbt, storedBooks.stacks);
        storedBooks.markDirty();
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        Inventories.writeNbt(nbt, storedBooks.stacks);
    }

    public LibraryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LIBRARY_BLOCK_ENTITY,pos, state);
    }

    @Override
    public Text getDisplayName() {
        return Text.literal("Library");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new LibraryInventoryScreenHandler(syncId,inv, storedBooks);
    }
}
