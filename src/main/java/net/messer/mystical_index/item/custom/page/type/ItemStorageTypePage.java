package net.messer.mystical_index.item.custom.page.type;

import net.messer.mystical_index.item.custom.book.CustomInventoryBook;
import net.messer.mystical_index.item.custom.page.AttributePageItem;
import net.messer.mystical_index.item.custom.page.TypePageItem;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static net.messer.mystical_index.item.ModItems.ITEM_STORAGE_TYPE_PAGE;

public class ItemStorageTypePage extends TypePageItem {
    public static final String MAX_STACKS_TAG = "max_stacks";
    public static final String MAX_TYPES_TAG = "max_types";

    @Override
    public int getColor() {
        return 0;
    }

    public static abstract class ItemStorageAttributePage extends AttributePageItem {
        @Override
        public List<TypePageItem> getCompatibleTypes(ItemStack page) {
            return List.of((TypePageItem) ITEM_STORAGE_TYPE_PAGE);
        }

        public int getStacksIncrease(ItemStack page) {
            return 0;
        }

        public int getTypesIncrease(ItemStack page) {
            return 0;
        }

        @Override
        public void appendAttributes(ItemStack page, NbtCompound nbt) {
            increaseIntAttribute(nbt, MAX_STACKS_TAG, getStacksIncrease(page));
            increaseIntAttribute(nbt, MAX_TYPES_TAG, getTypesIncrease(page));
        }

        @Override
        public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
            super.appendTooltip(stack, world, tooltip, context);

            var stacks = getStacksIncrease(stack);
            var types = getTypesIncrease(stack);

            if (stacks > 0) tooltip.add(new TranslatableText("item.mystical_index.page.tooltip.stacks", stacks * 64)
                    .formatted(Formatting.DARK_GREEN));
            if (types > 0) tooltip.add(new TranslatableText("item.mystical_index.page.tooltip.types", types)
                    .formatted(Formatting.DARK_GREEN));
        }
    }
}
