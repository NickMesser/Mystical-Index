package net.messer.mystical_index.item.custom;

import eu.pb4.polymer.api.item.PolymerItem;
import eu.pb4.sgui.api.elements.BookElementBuilder;
import eu.pb4.sgui.api.gui.BookGui;
import eu.pb4.sgui.virtual.book.BookScreenHandler;
import net.messer.mystical_index.util.BigStack;
import net.messer.mystical_index.util.ContentsIndex;
import net.messer.mystical_index.util.LibraryIndex;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LecternBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

public class Index extends Item implements NamedScreenHandlerFactory, PolymerItem {
    private static final int LINES_PER_PAGE = 12;

    public Index(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        BlockPos blockPos;
        World world = context.getWorld();
        BlockState blockState = world.getBlockState(blockPos = context.getBlockPos());
        if (blockState.isOf(Blocks.LECTERN)) {
            return LecternBlock.putBookIfAbsent(context.getPlayer(), world, blockPos, blockState, context.getStack()) ? ActionResult.success(world.isClient) : ActionResult.PASS;
        }
        return ActionResult.PASS;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        user.openHandledScreen(this);
        return TypedActionResult.success(itemStack, world.isClient());
    }

    private LibraryIndex getIndex(PlayerEntity player) {
        return LibraryIndex.get(player.getWorld(), player.getBlockPos());
    }

    private ContentsIndex getIndexContents(PlayerEntity player) {
        return getIndex(player).getItems();
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        List<Text> entries = getIndexContents(player).getTextList(Comparator.comparingInt(BigStack::getAmount).reversed());
        BookElementBuilder bookBuilder = new BookElementBuilder().signed();

        entries.addAll(0, List.of(new Text[]{
                new TranslatableText("gui.mystical_index.index_screen_header"),
                new LiteralText("")
        }));

        int size = entries.size();
        for (int current = 0; current < size; current += LINES_PER_PAGE) {
            MutableText text = new LiteralText("");

            for (int line = 0; line < Math.min(LINES_PER_PAGE, size - current); line++) {
                int position = current + line;

                text.append(entries.get(position).copy().append("\n")); // TODO button to request
            }

            bookBuilder.addPage(text);
        }

        BookGui gui = new BookGui((ServerPlayerEntity) player, bookBuilder);
        return new BookScreenHandler(syncId, gui, player);
    }

    @Override
    public Text getDisplayName() {
        return new TranslatableText("gui.mystical_index.index_screen"); // TODO correct translation key?
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        return Items.WRITTEN_BOOK;
    }
}
