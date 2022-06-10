package net.messer.mystical_index.item.custom.book;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Rarity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static net.messer.mystical_index.item.custom.page.type.ItemStorageTypePage.MAX_STACKS_TAG;
import static net.messer.mystical_index.item.custom.page.type.ItemStorageTypePage.MAX_TYPES_TAG;

public class CustomInventoryBook extends InventoryBookItem {
    public CustomInventoryBook(Settings settings) {
        super(settings);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        forEachPageType(stack, pageItem -> pageItem.bookInventoryTick(stack, world, entity, slot, selected));

        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack book, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(book, world, tooltip, context);

        forEachPageType(book, pageItem -> pageItem.bookAppendTooltip(book, tooltip));
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return Rarity.UNCOMMON;
    }

    @Override
    public int getMaxTypes(ItemStack book) {
        return book.getOrCreateNbt().getInt(MAX_TYPES_TAG);
    }

    @Override
    public int getMaxStack(ItemStack book) {
        return book.getOrCreateNbt().getInt(MAX_STACKS_TAG);
    }
}
