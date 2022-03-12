package net.messer.mystical_index.item.custom;

import eu.pb4.polymer.api.item.PolymerItem;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.messer.mystical_index.item.custom.book.CustomInventoryBook;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Rarity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class PageItem extends Item implements PolymerItem {
    public PageItem() {
        super(new FabricItemSettings().group(ItemGroup.TOOLS));
    }

    public int getStacksIncrease(ItemStack page) {
        return 0;
    }

    public int getTypesIncrease(ItemStack page) {
        return 0;
    }

    public ItemStack onCraftToBook(ItemStack page, ItemStack book) {
        var nbt = book.getOrCreateNbt();
        nbt.putInt(CustomInventoryBook.MAX_STACKS_TAG,
                nbt.getInt(CustomInventoryBook.MAX_STACKS_TAG) + getStacksIncrease(page));
        nbt.putInt(CustomInventoryBook.MAX_TYPES_TAG,
                nbt.getInt(CustomInventoryBook.MAX_TYPES_TAG) + getTypesIncrease(page));
        return book;
    }

    public void bookInventoryTick(ItemStack book, World world, Entity entity, int slot, boolean selected) {
    }

    public void appendProperties(ItemStack stack, List<Text> properties) {
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return Rarity.UNCOMMON;
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        return Items.FILLED_MAP;
    }

    public abstract int getColor();

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        var returnStack = PolymerItem.super.getPolymerItemStack(itemStack, player);
        returnStack.getOrCreateSubNbt("display").putInt("MapColor", getColor());
        return returnStack;
    }

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

        appendProperties(stack, tooltip);
    }
}
