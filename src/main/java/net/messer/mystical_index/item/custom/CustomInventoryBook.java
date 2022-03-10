package net.messer.mystical_index.item.custom;

import net.messer.mystical_index.util.parse.BookParser;
import net.messer.mystical_index.util.parse.IntPart;
import net.messer.mystical_index.util.parse.TypePart;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

public class CustomInventoryBook extends InventoryBookItem {
    public static final BookParser PARSER = new BookParser(
            new TypePart("\\btabularium libri\\b", "type", "items").required(), // Start definition
            new IntPart("\\bacervos (?<maxstacks>\\d+)\\b")
                    .map("maxstacks", "max_stacks").required(), // Stacks
            new IntPart("\\bagraphum (?<maxtypes>\\d+)\\b")
                    .map("maxtypes", "max_types").required() // Types
    );

//    private static final String MAX_STACKS_TAG = toTag("max_stacks");
//    private static final String MAX_TYPES_TAG = toTag("max_types");
//
//    private static String toTag(String string) {
//        return new Identifier(MysticalIndex.MOD_ID, string).toString();
//    }

    public CustomInventoryBook(Settings settings) {
        super(settings);
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        return Items.BOOK;
    }

    @Override
    public int getMaxTypes(ItemStack book) {
        return getModTag(book).getInt("max_types");
    }

    @Override
    public int getMaxStack(ItemStack book) {
        return getModTag(book).getInt("max_stacks");
    }
}
