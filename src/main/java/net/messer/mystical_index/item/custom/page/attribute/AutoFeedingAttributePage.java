package net.messer.mystical_index.item.custom.page.attribute;

import net.messer.mystical_index.item.ModItems;
import net.messer.mystical_index.item.custom.book.MysticalBookItem;
import net.messer.mystical_index.item.custom.page.AttributePageItem;
import net.messer.mystical_index.item.custom.page.TypePageItem;
import net.messer.mystical_index.item.custom.page.type.FoodStorageTypePage;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

public class AutoFeedingAttributePage extends AttributePageItem {
    public AutoFeedingAttributePage(String id) {
        super(id);
    }

    @Override
    public void appendAttributes(ItemStack page, NbtCompound nbt) {
    }

    @Override
    public int getColor() {
        return 0;
    }

    @Override
    public List<TypePageItem> getCompatibleTypes(ItemStack page) {
        return List.of(ModItems.FOOD_STORAGE_TYPE_PAGE);
    }

    @Override
    public void book$inventoryTick(ItemStack book, World world, Entity entity, int slot, boolean selected) {
        super.book$inventoryTick(book, world, entity, slot, selected);

        if(entity instanceof PlayerEntity player){
            var usedBook = (MysticalBookItem) book.getItem();

            if (usedBook.getTypePage(book) instanceof FoodStorageTypePage foodPage) {
                Item foodItem = null;
                try {
                    foodItem = foodPage.getContents(book).getAll().get(0).getItem();
                } catch (IndexOutOfBoundsException e) {
                }
                var foodComponent = foodItem.getFoodComponent();
                if (foodComponent != null && player.canConsume(foodComponent.isAlwaysEdible())){
                    player.setCurrentHand(player.getActiveHand());
                    TypedActionResult.consume(book);
                }
            }
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
    }
}
