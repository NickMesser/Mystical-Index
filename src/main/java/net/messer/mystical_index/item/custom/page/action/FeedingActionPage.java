package net.messer.mystical_index.item.custom.page.action;

import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.item.ModItems;
import net.messer.mystical_index.item.custom.book.MysticalBookItem;
import net.messer.mystical_index.item.custom.page.ActionPageItem;
import net.messer.mystical_index.item.custom.page.TypePageItem;
import net.messer.mystical_index.item.custom.page.type.FoodStorageTypePage;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

public class FeedingActionPage extends ActionPageItem {
    @Override
    public int getColor() {
        return 0x71c293;
    }


    @Override
    public List<TypePageItem> getCompatibleTypes(ItemStack page) {
        return List.of(ModItems.FOOD_STORAGE_TYPE_PAGE);
    }

    @Override
    public TypedActionResult<ItemStack> book$use(World world, PlayerEntity user, Hand hand) {
        if(world.isClient)
            return super.book$use(world, user, hand);

        var bookStack = user.getStackInHand(hand);
        var usedBook = (MysticalBookItem)bookStack.getItem();

        if(usedBook.getTypePage(bookStack) instanceof FoodStorageTypePage foodPage){
            var contents = foodPage.getContents(bookStack).getAll();

            for (var stack : contents) {
                if(stack.getItemStack().isFood()){
                    user.eatFood(world, stack.getItemStack());
                    MysticalIndex.LOGGER.info("Need to implement a way to remote a single item on use from BigStack.");
                    return super.book$use(world, user, hand);
                }
            }
        }
        return super.book$use(world, user, hand);
    }
}
