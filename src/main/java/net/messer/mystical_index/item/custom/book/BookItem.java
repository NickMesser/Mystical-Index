package net.messer.mystical_index.item.custom.book;

import net.messer.mystical_index.item.custom.page.InteractingPage;
import net.messer.mystical_index.item.custom.page.PageItem;
import net.messer.mystical_index.util.PageRegistry;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.*;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class BookItem extends Item {
    public static final String TYPE_PAGE_TAG = "type_page";
    public static final String ATTRIBUTE_PAGES_TAG = "attribute_pages";
    public static final String ACTION_PAGE_TAG = "action_page";

    public BookItem(Settings settings) {
        super(settings);
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
     * Similar to {@link BookItem#forPage(ItemStack, String, Function, Object)}, runs a function for the page with the given tag,
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
     * Similar to {@link BookItem#forInteractingPage(ItemStack, String, Function, Object)},
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
        return forInteractingPages(context.getStack(), result -> result != ActionResult.PASS,
                page -> page.book$useOnBlock(context), ActionResult.PASS);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);

        forEachPage(stack, page -> page.book$inventoryTick(stack, world, entity, slot, selected));
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);

        tooltip.add(new LiteralText(""));
        tooltip.add(new TranslatableText("item.mystical_index.custom_book.tooltip.capacity")
                .formatted(Formatting.GRAY));

        forEachPage(stack, page -> page.book$appendTooltip(stack, world, tooltip, context));
    }

    @Override
    public boolean canBeNested() {
        return false;
    }
}
