package net.messer.mystical_index.block.entity;

import eu.pb4.polymer.api.utils.PolymerObject;
import net.messer.mystical_index.item.custom.InventoryBookItem;
import net.messer.mystical_index.item.inventory.ILibraryInventory;
import net.messer.mystical_index.screen.LibraryInventoryScreenHandler;
import net.messer.mystical_index.util.ContentsIndex;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class LibraryBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, ILibraryInventory, PolymerObject {
    private final DefaultedList<ItemStack> storedBooks = DefaultedList.ofSize(5, ItemStack.EMPTY);

    @Override
    public DefaultedList<ItemStack> getItems() {
        return storedBooks;
    }

    public ContentsIndex getContents() {
        ContentsIndex contents = new ContentsIndex();
        for (ItemStack book : storedBooks) {
            if (book.getItem() instanceof InventoryBookItem inventoryBookItem)
                contents.merge(inventoryBookItem.getContents(book));
        }
        return contents;
    }

    public LibraryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LIBRARY_BLOCK_ENTITY,pos, state);
    }

    @Override
    public Text getDisplayName() {
        return new LiteralText("Library");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new LibraryInventoryScreenHandler(syncId,inv,this);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        Inventories.writeNbt(nbt, storedBooks);
        super.writeNbt(nbt);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        Inventories.readNbt(nbt, storedBooks);
    }
}
