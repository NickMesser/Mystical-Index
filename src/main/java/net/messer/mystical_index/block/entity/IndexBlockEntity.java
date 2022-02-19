package net.messer.mystical_index.block.entity;

import eu.pb4.polymer.api.utils.PolymerObject;
import eu.pb4.sgui.api.elements.BookElementBuilder;
import eu.pb4.sgui.api.gui.BookGui;
import eu.pb4.sgui.virtual.book.BookScreenHandler;
import net.messer.mystical_index.util.BigStack;
import net.messer.mystical_index.util.ContentsIndex;
import net.messer.mystical_index.util.LibraryIndex;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

public class IndexBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, PolymerObject {
    private static final int LINES_PER_PAGE = 8;

    public IndexBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LIBRARY_BLOCK_ENTITY,pos, state);
    }

    private LibraryIndex getIndex() {
        return LibraryIndex.get(getWorld(), getPos());
    }

    private ContentsIndex getIndexContents() {
        return getIndex().getItems();
    }

    @Override
    public Text getDisplayName() {
        return new LiteralText("Index");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        // ContentsIndex index = getIndexContents();
        List<Text> entries = getIndexContents().getTextList(Comparator.comparingInt(BigStack::getAmount));
        BookElementBuilder bookBuilder = new BookElementBuilder().signed();

        entries.addAll(0, List.of(new Text[]{
                new LiteralText("Stored items:"),
                new LiteralText("")
        }));

        // Comparator.comparingInt(BigStack::getAmount)

        for (int current = 0; current < entries.size(); current += LINES_PER_PAGE) {
            MutableText text = new LiteralText("");

            for (int line = 0; line < LINES_PER_PAGE; line++) {
                int position = current + line;

                text.append(entries.get(position).copy().append("\n"));
            }

            bookBuilder.addPage(text);
        }

        // TODO iterate over index with i / maxpages and mb add title or smt

        BookGui gui = new BookGui((ServerPlayerEntity) player, bookBuilder);
        return new BookScreenHandler(syncId, gui, player);
    }
}
