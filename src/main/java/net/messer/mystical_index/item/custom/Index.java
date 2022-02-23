package net.messer.mystical_index.item.custom;

import eu.pb4.polymer.api.item.PolymerItem;
import eu.pb4.sgui.api.elements.BookElementBuilder;
import eu.pb4.sgui.api.gui.BookGui;
import net.messer.mystical_index.util.BigStack;
import net.messer.mystical_index.util.ContentsIndex;
import net.messer.mystical_index.util.ParticleSystem;
import net.messer.mystical_index.util.request.InsertionRequest;
import net.messer.mystical_index.util.request.LibraryIndex;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LecternBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ClickType;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

public class Index extends Item implements PolymerItem {
    private static final int LINES_PER_PAGE = 12;

    public Index(Settings settings) {
        super(settings);
    }

    @Override
    public boolean onStackClicked(ItemStack book, Slot slot, ClickType clickType, PlayerEntity player) {
        if (clickType != ClickType.RIGHT || !slot.hasStack()) {
            return false;
        }
        tryInsertItemStack(slot.getStack(), player);
        return true;
    }

    @Override
    public boolean onClicked(ItemStack book, ItemStack cursorStack, Slot slot, ClickType clickType, PlayerEntity player, StackReference cursorStackReference) {
        if (clickType != ClickType.RIGHT || cursorStack.isEmpty() || !slot.canTakePartial(player)) {
            return false;
        }
        tryInsertItemStack(cursorStack, player);
        return true;
    }

    public void tryInsertItemStack(ItemStack itemStack, PlayerEntity player) {
        LibraryIndex index = LibraryIndex.get(player.getWorld(), player.getBlockPos());

        InsertionRequest request = new InsertionRequest(itemStack);
        request.setSourcePosition(player.getPos());
        request.setBlockAffectedCallback(ParticleSystem::insertionParticles);

        index.insertStack(request);
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
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);
        if (player instanceof ServerPlayerEntity serverPlayer) {
            createMenu(serverPlayer, serverPlayer.getBlockPos()).open();
        }
        return TypedActionResult.success(itemStack, world.isClient());
    }

    public BookElementBuilder getMenuItem(ServerPlayerEntity player, BlockPos pos) {
        LibraryIndex index = LibraryIndex.get(player.getWorld(), pos);
        List<Text> entries = index.getContents().getTextList(Comparator.comparingInt(BigStack::getAmount).reversed());
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

        return bookBuilder; // Preferably i'd have this return an ItemStack, but due to a bug in sgui it's easier like this.
    }

    public BookGui createMenu(ServerPlayerEntity player, BlockPos pos) {
        return new BookGui(player, getMenuItem(player, pos));
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        return Items.WRITTEN_BOOK;
    }
}
