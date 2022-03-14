package net.messer.mystical_index.item.custom.book;

import com.google.common.collect.ImmutableList;
import eu.pb4.sgui.api.elements.BookElementBuilder;
import eu.pb4.sgui.api.gui.BookGui;
import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.block.ModBlocks;
import net.messer.mystical_index.block.custom.IndexLecternBlock;
import net.messer.mystical_index.util.BigStack;
import net.messer.mystical_index.util.ContentsIndex;
import net.messer.mystical_index.util.ParticleSystem;
import net.messer.mystical_index.util.request.InsertionRequest;
import net.messer.mystical_index.util.request.LibraryIndex;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LecternBlock;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.*;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class CustomIndexBook extends CustomInventoryBook {
    private static final int LINES_PER_PAGE = 12;
    public static final double LECTERN_PICKUP_RADIUS = 2d;
    public static final UUID EXTRACTED_DROP_UUID = UUID.randomUUID();

    public static final String MAX_RANGE_TAG = "max_range";
    public static final String MAX_LINKS_TAG = "max_links";
    public static final String IN_INVENTORY_TAG = "in_inventory";
    public static final String ON_LECTERN_TAG = "on_lectern";

    private static final List<Text> HEADER = List.of(new Text[]{
            new TranslatableText("gui.mystical_index.index_screen_header"),
            new LiteralText("")
    });

    public CustomIndexBook(Settings settings) {
        super(settings);
    }

    public int getMaxRange(ItemStack book, boolean lectern) {
        return book.getOrCreateSubNbt(lectern ? ON_LECTERN_TAG : IN_INVENTORY_TAG).getInt(MAX_RANGE_TAG);
    }

    public int getMaxLinks(ItemStack book, boolean lectern) {
        return book.getOrCreateSubNbt(lectern ? ON_LECTERN_TAG : IN_INVENTORY_TAG).getInt(MAX_LINKS_TAG);
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
        LibraryIndex index = LibraryIndex.fromRange(player.getWorld(), player.getBlockPos(), LibraryIndex.ITEM_SEARCH_RANGE);

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
        if (blockState.isOf(Blocks.LECTERN) && !blockState.get(LecternBlock.HAS_BOOK)) {
            var newState = ModBlocks.INDEX_LECTERN.getStateWithProperties(blockState);
            world.setBlockState(blockPos, newState);
            // something runs setblockstate a second time perhaps?
            ItemStack stack = context.getStack().copy();
            context.getStack().decrement(1);
            return IndexLecternBlock.putBookIfAbsent(
                    context.getPlayer(), world,
                    blockPos, newState,
                    stack
            ) ? ActionResult.success(true) : ActionResult.PASS;
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
        LibraryIndex index = LibraryIndex.fromRange(world, pos, range);
        ContentsIndex contents = index.getContents().sorted(Comparator.comparingInt(BigStack::getAmount).reversed());
        List<Text> entries = contents.getTextList();
        BookElementBuilder bookBuilder = new BookElementBuilder().signed();

        int headerSize = HEADER.size();
        entries.addAll(0, HEADER);

        // Construct the short list pages
        int size = entries.size();
        int indexPages = (int) Math.ceil((double) size / LINES_PER_PAGE);
        for (int current = 0; current < size; current += LINES_PER_PAGE) {
            MutableText text = new LiteralText("");

            for (int line = 0; line < Math.min(LINES_PER_PAGE, size - current); line++) {
                int position = current + line;

                if (position < headerSize) {
                    text.append(entries.get(position).copy().append("\n"));
                } else {
                    text.append(entries.get(position).copy().append("\n").styled(style -> style
                            .withColor(Formatting.DARK_GRAY)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.CHANGE_PAGE, String.valueOf(position - headerSize + indexPages + 1)))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TranslatableText("gui.mystical_index.index_list_clickable")))
                    ));
                }
            }

            bookBuilder.addPage(text);
        }

        // Construct the detail pages
        for (BigStack stack : contents) {
            ItemStack itemStack = stack.getItemStack();

            ImmutableList.Builder<Text> builder = ImmutableList.builder();
            builder.add(new LiteralText("<<<\n").styled(style -> style
                    .withColor(Formatting.GREEN)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.CHANGE_PAGE, String.valueOf(1)))
            ));
            builder.add(itemStack.toHoverableText().copy().append("\n"));
            builder.add(new TranslatableText("gui.mystical_index.index_stored_header"));
            int amount = stack.getAmount();
            int stackSize = itemStack.getMaxCount();
            int stacksAmount = Math.floorDiv(amount, stackSize);
            int amountLeftover = amount % stackSize;
            builder.add(new LiteralText(String.valueOf(amount)).styled(style ->
                    style.withColor(Formatting.DARK_GRAY)
            ).append(new LiteralText(" (" + stacksAmount + "*" + stackSize + "+" + amountLeftover + ")").styled(style ->
                    style.withColor(Formatting.GRAY)
            )));

            bookBuilder.addPage(builder.build().toArray(new Text[0]));
        }

        // Preferably I'd have this return an ItemStack, but due to a bug in sgui it's easier like this.
        return bookBuilder;
    }

    public BookGui createMenu(ServerPlayerEntity player, BlockPos pos) {
        return new BookGui(player, getMenuItem(player, pos, LibraryIndex.ITEM_SEARCH_RANGE));
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return Rarity.RARE;
    }

    @Override
    public void appendTooltip(ItemStack book, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        var lectern = true;
        do {
            lectern = !lectern;

            tooltip.add(new LiteralText(""));
            tooltip.add(new TranslatableText(lectern ?
                        "item.mystical_index.custom_index.tooltip.on_lectern" :
                        "item.mystical_index.custom_index.tooltip.in_inventory")
                    .formatted(Formatting.GRAY));
            tooltip.add(lectern ?
                    new TranslatableText("item.mystical_index.custom_index.tooltip.automatic")
                    .formatted(Formatting.GREEN) :
                    new TranslatableText("item.mystical_index.custom_index.tooltip.manual")
                    .formatted(Formatting.RED));
            tooltip.add(new TranslatableText("item.mystical_index.custom_index.tooltip.range",
                        getMaxRange(book, lectern))
                    .formatted(Formatting.GOLD));
            tooltip.add(new TranslatableText(lectern ?
                        "item.mystical_index.custom_index.tooltip.extenders" :
                        "item.mystical_index.custom_index.tooltip.links",
                        getMaxLinks(book, lectern))
                    .formatted(Formatting.GOLD));
        } while (!lectern);

        tooltip.add(new LiteralText(""));
        tooltip.add(new TranslatableText("item.mystical_index.custom_book.tooltip.capacity")
                .formatted(Formatting.GRAY));
        forEachPageType(book, pageItem -> pageItem.appendProperties(book, tooltip));
    }
}
