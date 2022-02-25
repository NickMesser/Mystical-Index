package net.messer.mystical_index.item.custom;

import eu.pb4.polymer.api.item.PolymerItem;
import eu.pb4.sgui.api.elements.BookElementBuilder;
import eu.pb4.sgui.api.gui.BookGui;
import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.item.ModItems;
import net.messer.mystical_index.util.BigStack;
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
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class Index extends Item implements PolymerItem {
    private static final int LINES_PER_PAGE = 12;
    public static final String LECTERN_TAG_NAME = new Identifier(MysticalIndex.MOD_ID, "index_nbt").toString();
    public static final double LECTERN_PICKUP_RADIUS = 2d;
    public static final UUID EXTRACTED_DROP_UUID = UUID.randomUUID();

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
        LibraryIndex index = LibraryIndex.get(player.getWorld(), player.getBlockPos(), LibraryIndex.ITEM_SEARCH_RANGE);

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
            ItemStack stack = context.getStack().copy();
            context.getStack().decrement(1);
            return LecternBlock.putBookIfAbsent(
                    context.getPlayer(), world,
                    blockPos, blockState,
                    toLecternBook(
                            stack,
                            (ServerWorld) world,
                            blockPos
                    )
            ) ? ActionResult.success(world.isClient) : ActionResult.PASS;
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

    public BookElementBuilder getMenuItem(ServerPlayerEntity player, BlockPos pos, int range) {
        return getMenuItem(player.getWorld(), pos, range);
    }

    public static BookElementBuilder getMenuItem(ServerWorld world, BlockPos pos, int range) {
        LibraryIndex index = LibraryIndex.get(world, pos, range);
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
        return new BookGui(player, getMenuItem(player, pos, LibraryIndex.ITEM_SEARCH_RANGE));
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        return Items.WRITTEN_BOOK;
    }

    public static ItemStack getFromLecternBook(ItemStack book) {
        if (book.getOrCreateNbt().contains(Index.LECTERN_TAG_NAME)) {
            ItemStack newItem = new ItemStack(ModItems.INDEX);
            newItem.setNbt(book.getOrCreateNbt().getCompound(Index.LECTERN_TAG_NAME));
            return newItem;
        }
        return book;
    }

    public static ItemStack toLecternBook(ItemStack index, ServerWorld world, BlockPos pos) {
        ItemStack itemStack = getMenuItem(world, pos, LibraryIndex.LECTERN_SEARCH_RANGE).asStack();
        itemStack.getOrCreateNbt().put(
                LECTERN_TAG_NAME,
                index.getOrCreateNbt()
        );
        return itemStack;
    }
}
