package net.messer.mystical_index.item.custom.book;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;

public class CustomInventoryBook extends InventoryBookItem {
    public static final String MAX_STACKS_TAG = "max_stacks";
    public static final String MAX_TYPES_TAG = "max_types";
    public static final String PAGES_TAG = "mystical_pages";

    private static final HashMap<Identifier, Item> PAGES_CACHE = new HashMap<>();

    public CustomInventoryBook(Settings settings) {
        super(settings);
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return Rarity.UNCOMMON;
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        return Items.BOOK;
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
