package net.messer.mystical_index.item.custom;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.messer.mystical_index.item.custom.book.CustomIndexBook;
import net.messer.mystical_index.item.custom.book.CustomInventoryBook;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Rarity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class PageItem extends Item {
    public PageItem() {
        super(new FabricItemSettings().group(ItemGroup.TOOLS));
    }

    public int getStacksIncrease(ItemStack page) {
        return 0;
    }

    public int getTypesIncrease(ItemStack page) {
        return 0;
    }

    public int getRangeIncrease(ItemStack page, boolean autoIndexing) {
        return 0;
    }

    public int getLinksIncrease(ItemStack page, boolean autoIndexing) {
        return 0;
    }

    public ItemStack onCraftToBook(ItemStack page, ItemStack book) {
        var nbt = book.getOrCreateNbt();

        nbt.putInt(CustomInventoryBook.MAX_STACKS_TAG,
                nbt.getInt(CustomInventoryBook.MAX_STACKS_TAG) + getStacksIncrease(page));
        nbt.putInt(CustomInventoryBook.MAX_TYPES_TAG,
                nbt.getInt(CustomInventoryBook.MAX_TYPES_TAG) + getTypesIncrease(page));

        var autoIndexing = true;
        do {
            autoIndexing = !autoIndexing;
            var subTag = nbt.getCompound(autoIndexing ? CustomIndexBook.AUTO_INDEXING_TAG : CustomIndexBook.MANUAL_INDEXING_TAG);

            subTag.putInt(CustomIndexBook.MAX_RANGE_TAG,
                    subTag.getInt(CustomIndexBook.MAX_RANGE_TAG) + getRangeIncrease(page, autoIndexing));
            subTag.putInt(CustomIndexBook.MAX_LINKS_TAG,
                    subTag.getInt(CustomIndexBook.MAX_LINKS_TAG) + getLinksIncrease(page, autoIndexing));
        } while (!autoIndexing);

        return book;
    }

    public void bookInventoryTick(ItemStack book, World world, Entity entity, int slot, boolean selected) {
    }

    public void appendProperties(ItemStack book, List<Text> properties) {
    }

    public boolean bookCanHaveMultiple(ItemStack page) {
        return true;
    }

    public List<PageItem> incompatiblePages(ItemStack page) {
        return List.of();
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return Rarity.UNCOMMON;
    }

    public abstract int getColor();

    // TODO display color on item texture somehow
//    @Override
//    public ItemStack getPolymerItemStack(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
//        var returnStack = PolymerItem.super.getPolymerItemStack(itemStack, player);
//        returnStack.getOrCreateSubNbt("display").putInt("MapColor", getColor());
//        return returnStack;
//    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);

        var stacks = getStacksIncrease(stack);
        var types = getTypesIncrease(stack);

        tooltip.add(new LiteralText(""));
        tooltip.add(new TranslatableText("item.mystical_index.page.tooltip.when_applied")
                .formatted(Formatting.GRAY));
        if (stacks > 0) tooltip.add(new TranslatableText("item.mystical_index.page.tooltip.stacks", stacks * 64)
                .formatted(Formatting.DARK_GREEN));
        if (types > 0) tooltip.add(new TranslatableText("item.mystical_index.page.tooltip.types", types)
                .formatted(Formatting.DARK_GREEN));

        appendProperties(null, tooltip);
    }
}
