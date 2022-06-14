package net.messer.mystical_index.item.custom.book;

import net.messer.mystical_index.block.ModBlocks;
import net.messer.mystical_index.block.custom.MysticalLecternBlock;
import net.messer.mystical_index.block.entity.MysticalLecternBlockEntity;
import net.messer.mystical_index.item.custom.page.ActionPageItem;
import net.messer.mystical_index.item.custom.page.InteractingPage;
import net.messer.mystical_index.item.custom.page.PageItem;
import net.messer.mystical_index.item.custom.page.TypePageItem;
import net.messer.mystical_index.util.PageRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LecternBlock;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtElement;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class MysticalBookItem extends Item {
    public static final String TYPE_PAGE_TAG = "type_page";
    public static final String ATTRIBUTE_PAGES_TAG = "attribute_pages";
    public static final String ACTION_PAGE_TAG = "action_page";
    public static final String COLOR_TAG = "color";

    public MysticalBookItem(Settings settings) {
        super(settings);
    }

    public int getColor(ItemStack stack) {
        return stack.getOrCreateNbt().getInt(COLOR_TAG);
    }

    public void setColor(ItemStack stack, int color) {
        stack.getOrCreateNbt().putInt(COLOR_TAG, color);
    }

    /**
     * Safely get a single page from a tag.
     */
    @Nullable
    public PageItem getPage(ItemStack book, String tag) {
        try {
            return PageRegistry.getPage(new Identifier(book.getOrCreateNbt().getString(tag)));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns the current type page of the book.
     */
    @Nullable
    public TypePageItem getTypePage(ItemStack book) {
        return (TypePageItem) getPage(book, TYPE_PAGE_TAG);
    }

    /**
     * Returns the current action page of the book.
     */
    @Nullable
    public ActionPageItem getActionPage(ItemStack book) {
        return (ActionPageItem) getPage(book, ACTION_PAGE_TAG);
    }

    /**
     * Checks if the type page of this book is of a certain class, and if so, returns it.
     */
    @Nullable
    public static <T extends TypePageItem> T isType(ItemStack book, Class<T> clazz) {
        try {
            return clazz.cast(((MysticalBookItem) book.getItem()).getTypePage(book));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Runs the given consumer on the page in the given tag without a return value.
     */
    private void forPage(ItemStack book, String tag, Consumer<PageItem> consumer) {
        forPage(book, tag, pageItem -> {
            consumer.accept(pageItem);
            return null;
        }, null);
    }

    /**
     * Runs a function for the page with the given tag,
     * providing the returned result or a default value if the page is not found.
     */
    private <R> R forPage(ItemStack book, String tag, Function<PageItem, R> function, R defaultValue) {
        var pageName = book.getOrCreateNbt().getString(tag);
        var pageId = Identifier.tryParse(pageName);
        if (pageId != null) {
            var page = PageRegistry.getPage(pageId);
            if (page != null) {
                return function.apply(page);
            }
        }
        return defaultValue;
    }

    /**
     * Similar to {@link MysticalBookItem#forPage(ItemStack, String, Function, Object)}, runs a function for the page with the given tag,
     * but safely casts it to {@link InteractingPage} first.
     * This opens up page functions that have return values.
     */
    private <R> R forInteractingPage(ItemStack book, String tag, Function<InteractingPage, R> function, R defaultValue) {
        return forPage(book, tag, pageItem -> {
            if (pageItem instanceof InteractingPage) {
                return function.apply((InteractingPage) pageItem);
            }
            return defaultValue;
        }, defaultValue);
    }

    /**
     * <p>
     * Similar to {@link MysticalBookItem#forInteractingPage(ItemStack, String, Function, Object)},
     * but runs the function for all pages supporting interaction. (Namely type pages and action pages)
     * </p>
     * <p>
     * Requires a return condition, which determines whether the value
     * returned by the function lambda is accepted or ignored.
     * If it is ignored, the next page will be run and checked.
     * </p>
     * <p>
     * <b>Once the return value is accepted, the following pages will be ignored.</b>
     * </p>
     */
    private <R> R forInteractingPages(ItemStack book, Predicate<R> returnCondition, Function<InteractingPage, R> function, R defaultValue) {
        for (var tag : new String[]{TYPE_PAGE_TAG, ACTION_PAGE_TAG}) {
            var result = forInteractingPage(book, tag, function, defaultValue);

            if (returnCondition.test(result)) {
                return result;
            }
        }
        return defaultValue;
    }

    /**
     * Runs the given consumer for every page in the book.
     */
    public void forEachPage(ItemStack book, Consumer<PageItem> consumer) {
        forPage(book, TYPE_PAGE_TAG, consumer);

        book.getOrCreateNbt().getList(ATTRIBUTE_PAGES_TAG, NbtElement.STRING_TYPE).forEach(element -> {
            var pageName = element.asString();
            var pageId = Identifier.tryParse(pageName);
            if (pageId != null) {
                var page = PageRegistry.getPage(pageId);
                if (page != null) {
                    consumer.accept(page);
                }
            }
        });

        forPage(book, ACTION_PAGE_TAG, consumer);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return forInteractingPages(user.getStackInHand(hand), result -> result.getResult() != ActionResult.PASS,
                page -> page.book$use(world, user, hand), TypedActionResult.pass(user.getStackInHand(hand)));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        BlockPos blockPos;
        World world = context.getWorld();
        BlockState blockState = world.getBlockState(blockPos = context.getBlockPos());

        // Try to put book on lectern
        if (blockState.isOf(Blocks.LECTERN) && !blockState.get(LecternBlock.HAS_BOOK)) {
            var newState = ModBlocks.MYSTICAL_LECTERN.getStateWithProperties(blockState);

            world.setBlockState(blockPos, newState);
            ItemStack stack = context.getStack().copy();
            context.getStack().decrement(1);

            return MysticalLecternBlock.putBookIfAbsent(
                    context.getPlayer(), world,
                    blockPos, newState,
                    stack
            ) ? ActionResult.success(world.isClient) : ActionResult.PASS;
        }

        return forInteractingPages(context.getStack(), result -> result != ActionResult.PASS,
                page -> page.book$useOnBlock(context), ActionResult.PASS);
    }

    @Override
    public boolean onClicked(ItemStack book, ItemStack otherStack, Slot slot, ClickType clickType, PlayerEntity player, StackReference cursorStackReference) {
        return forInteractingPages(book, result -> result,
                page -> page.book$onClicked(book, otherStack, slot, clickType, player, cursorStackReference), false);
    }

    @Override
    public boolean onStackClicked(ItemStack book, Slot slot, ClickType clickType, PlayerEntity player) {
        return forInteractingPages(book, result -> result,
                page -> page.book$onStackClicked(book, slot, clickType, player), false);
    }

    @Override
    public void inventoryTick(ItemStack book, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(book, world, entity, slot, selected);

        forEachPage(book, page -> page.book$inventoryTick(book, world, entity, slot, selected));
    }

    @Override
    public void appendTooltip(ItemStack book, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(book, world, tooltip, context);

        forEachPage(book, page -> page.book$appendTooltip(book, world, tooltip, context));

        tooltip.add(new LiteralText(""));
        tooltip.add(new TranslatableText("item.mystical_index.mystical_book.tooltip.properties")
                .formatted(Formatting.GRAY));

        forEachPage(book, page -> page.book$appendPropertiesTooltip(book, world, tooltip, context));
    }

    @Override
    public boolean hasGlint(ItemStack book) {
        return forInteractingPages(book, result -> result,
                page -> page.book$hasGlint(book), false);
    }

    public ItemStack getBook(){
        return this.getBook();
    }

    public boolean interceptsChatMessage(ItemStack book, ServerPlayerEntity player, String message) {
        return forInteractingPages(book, result -> result,
                page -> page.book$interceptsChatMessage(book, player, message), false);
    }

    public void onInterceptedChatMessage(ItemStack book, ServerPlayerEntity player, String message) {
        forEachPage(book, page -> page.book$onInterceptedChatMessage(book, player, message));
    }

    public boolean lectern$interceptsChatMessage(MysticalLecternBlockEntity lectern, ServerPlayerEntity player, String message) {
        return forInteractingPages(lectern.getBook(), result -> result,
                page -> page.lectern$interceptsChatMessage(lectern, player, message), false);
    }

    public void lectern$onInterceptedChatMessage(MysticalLecternBlockEntity lectern, ServerPlayerEntity player, String message) {
        forEachPage(lectern.getBook(), page -> page.lectern$onInterceptedChatMessage(lectern, player, message));
    }

    public void lectern$onEntityCollision(MysticalLecternBlockEntity lectern, BlockState state, World world, BlockPos pos, Entity entity) {
        forEachPage(lectern.getBook(), page -> page.lectern$onEntityCollision(lectern, state, world, pos, entity));
    }

    public ActionResult lectern$onUse(MysticalLecternBlockEntity lectern, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        return forInteractingPages(lectern.getBook(), result -> result != ActionResult.PASS,
                page -> page.lectern$onUse(lectern, state, world, pos, player, hand, hit), ActionResult.PASS);
    }

    public void lectern$serverTick(World world, BlockPos pos, BlockState state, MysticalLecternBlockEntity lectern) {
        forEachPage(lectern.getBook(), page -> page.lectern$serverTick(world, pos, state, lectern));
    }

    public void lectern$onPlaced(MysticalLecternBlockEntity lectern) {
        forEachPage(lectern.getBook(), page -> page.lectern$onPlaced(lectern));
    }

    @Override
    public Text getName(ItemStack book) {
        var page = getTypePage(book);
        if (page != null) {
            return page.getBookDisplayName();
        }
        return super.getName(book);
    }

    @Override
    public boolean canBeNested() {
        return false;
    }
}
