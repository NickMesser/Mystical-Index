package net.messer.mystical_index.item.custom.book;

import net.messer.mystical_index.item.custom.PageItem;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

public class CustomInventoryBook extends InventoryBookItem {
    public static final String MAX_STACKS_TAG = "max_stacks";
    public static final String MAX_TYPES_TAG = "max_types";
    public static final String PAGES_TAG = "mystical_pages";

    private static final HashMap<Identifier, PageItem> REGISTERED_PAGES = new HashMap<>();

    public static void registerPage(Identifier itemId, Item item) {
        if (item instanceof PageItem pageItem)
            REGISTERED_PAGES.put(itemId, pageItem);
    }

    public CustomInventoryBook(Settings settings) {
        super(settings);
    }

    public void forEachPageType(ItemStack book, Consumer<PageItem> consumer) {
        var pages = book.getOrCreateNbt().getList(PAGES_TAG, NbtElement.STRING_TYPE);
        REGISTERED_PAGES.forEach((identifier, item) -> {
            if (pages.contains(NbtString.of(identifier.toString())))
                consumer.accept(item);
        });
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        forEachPageType(stack, pageItem -> pageItem.bookInventoryTick(stack, world, entity, slot, selected));

        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack book, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(book, world, tooltip, context);

        forEachPageType(book, pageItem -> pageItem.appendProperties(book, tooltip));
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
