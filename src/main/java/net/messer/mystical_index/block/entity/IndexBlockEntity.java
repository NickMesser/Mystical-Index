package net.messer.mystical_index.block.entity;

import eu.pb4.polymer.api.utils.PolymerObject;
import eu.pb4.sgui.api.elements.BookElementBuilder;
import eu.pb4.sgui.api.gui.BookGui;
import eu.pb4.sgui.virtual.book.BookScreenHandler;
import net.messer.mystical_index.screen.LibraryInventoryScreenHandler;
import net.messer.mystical_index.util.LibraryIndex;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.LecternScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.CallbackI;

public class IndexBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, PolymerObject {

    public IndexBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LIBRARY_BLOCK_ENTITY,pos, state);
    }

    private LibraryIndex getIndex() {
        return LibraryIndex.get(getWorld(), getPos());
    }

    @Override
    public Text getDisplayName() {
        return new LiteralText("Index");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        BookElementBuilder bookBuilder = new BookElementBuilder().signed();
        // TODO iterate over index with i / maxpages and mb add title or smt

        BookGui gui = new BookGui(player);

        return new BookScreenHandler()
        //return new LibraryInventoryScreenHandler(syncId,inv,this); // WIP
    }
}
