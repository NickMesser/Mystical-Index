package net.messer.mystical_index.item.custom.page.type;

import net.messer.mystical_index.item.custom.page.TypePageItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;

import java.util.List;

import static net.messer.mystical_index.item.ModItems.BLOCK_STORAGE_TYPE_PAGE;
import static net.messer.mystical_index.item.ModItems.FOOD_STORAGE_TYPE_PAGE;

public class BlockStorageTypePage extends ItemStorageTypePage {
    public BlockStorageTypePage(String id) {
        super(id);
    }

    @Override
    public int getColor() {
        return 0x444444;
    }

    @Override
    public boolean mixColor(ItemStack stack) {
        return true;
    }

    @Override
    public MutableText getTypeDisplayName() {
        return super.getTypeDisplayName().fillStyle(Style.EMPTY.withColor(getColor()));
    }

    @Override
    protected boolean canInsert(ItemStack book, ItemStack itemStack) {
        if (!(itemStack.getItem() instanceof BlockItem)) return false;

        return super.canInsert(book, itemStack);
    }

    public static abstract class BlockStorageAttributePage extends ItemStorageAttributePage {
        public BlockStorageAttributePage(String id) {
            super(id);
        }

        @Override
        public List<TypePageItem> getCompatibleTypes(ItemStack page) {
            return List.of(BLOCK_STORAGE_TYPE_PAGE);
        }
    }
}
