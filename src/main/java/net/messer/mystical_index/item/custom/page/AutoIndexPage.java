package net.messer.mystical_index.item.custom.page;

import net.messer.mystical_index.item.custom.PageItem;
import net.messer.mystical_index.item.custom.book.CustomIndexBook;
import net.messer.mystical_index.util.Colors;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Rarity;

import java.util.List;

import static net.messer.mystical_index.item.ModItems.CUSTOM_INDEX;

public class AutoIndexPage extends PageItem {
    @Override
    public ItemStack onCraftToBook(ItemStack page, ItemStack book) {
        var indexBook = new ItemStack(CUSTOM_INDEX);
        var nbt = book.getOrCreateNbt();
        indexBook.setNbt(nbt);
        return indexBook;
    }

    @Override
    public int getColor() {
        return 11745593;
    }

    @Override
    public void appendProperties(ItemStack book, List<Text> properties) {
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
    public boolean bookCanHaveMultiple(ItemStack page) {
        return false;
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return Rarity.RARE;
    }
}
