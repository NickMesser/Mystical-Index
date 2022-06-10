package net.messer.mystical_index.item.custom.page.type;

import net.messer.mystical_index.item.custom.book.CustomIndexBook;
import net.messer.mystical_index.item.custom.page.AttributePageItem;
import net.messer.mystical_index.item.custom.page.PageItem;
import net.messer.mystical_index.item.custom.page.TypePageItem;
import net.messer.mystical_index.util.Colors;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Rarity;

import java.util.List;

import static net.messer.mystical_index.item.ModItems.*;

public class IndexingTypePage extends TypePageItem {
    public static final String MAX_RANGE_TAG = "max_range";
    public static final String MAX_LINKS_TAG = "max_links";
    public static final String MAX_RANGE_LINKED_TAG = "max_range_linked";
    public static final String LINKED_BLOCKS_TAG = "linked_blocks";

    @Override
    public void onCraftToBook(ItemStack page, ItemStack book) {
        var indexBook = new ItemStack(CUSTOM_INDEX);
        var nbt = book.getOrCreateNbt();
        indexBook.setNbt(nbt);
    }

    @Override
    public int getColor() {
        return 11745593;
    }

    @Override
    public void bookAppendTooltip(ItemStack book, List<Text> properties) {
        properties.add(new TranslatableText("item.mystical_index.custom_index.tooltip.automatic")
                .formatted(Formatting.BLUE));

        if (book != null) {
            var bookItem = (CustomIndexBook) book.getItem();

            var linksUsed = bookItem.getLinks(book);
            var linksMax = bookItem.getMaxLinks(book, true);
            double linksUsedRatio = (double) linksUsed / linksMax;

            properties.add(new TranslatableText("item.mystical_index.custom_index.tooltip.links",
                    linksUsed, linksMax)
                    .formatted(Colors.colorByRatio(linksUsedRatio)));
            properties.add(new TranslatableText("item.mystical_index.custom_index.tooltip.link_range",
                    bookItem.getMaxRange(book, false))
                    .formatted(Formatting.YELLOW));
            properties.add(new TranslatableText("item.mystical_index.custom_index.tooltip.range",
                    bookItem.getMaxRange(book, true))
                    .formatted(Formatting.YELLOW));
        }
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return Rarity.RARE;
    }

    public static abstract class IndexingAttributePage extends AttributePageItem {
        @Override
        public List<TypePageItem> getCompatibleTypes(ItemStack page) {
            return List.of((TypePageItem) INDEXING_TYPE_PAGE);
        }

        public int getRangeIncrease(ItemStack page, boolean linked) {
            return 0;
        }

        public int getLinksIncrease(ItemStack page) {
            return 0;
        }

        @Override
        public void appendAttributes(ItemStack page, NbtCompound nbt) {
            increaseIntAttribute(nbt, MAX_RANGE_TAG, getRangeIncrease(page, false));
            increaseIntAttribute(nbt, MAX_LINKS_TAG, getLinksIncrease(page));
            increaseIntAttribute(nbt, MAX_RANGE_LINKED_TAG, getRangeIncrease(page, true));
        }
    }
}
