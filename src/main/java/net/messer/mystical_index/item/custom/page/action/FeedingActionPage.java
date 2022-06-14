package net.messer.mystical_index.item.custom.page.action;

import net.messer.mystical_index.item.ModItems;
import net.messer.mystical_index.item.custom.book.MysticalBookItem;
import net.messer.mystical_index.item.custom.page.ActionPageItem;
import net.messer.mystical_index.item.custom.page.TypePageItem;
import net.messer.mystical_index.item.custom.page.type.FoodStorageTypePage;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

public class FeedingActionPage extends ActionPageItem {
    public FeedingActionPage(String id) {
        super(id);
    }

    @Override
    public int getColor() {
        return 0xff55dd;
    }

    @Override
    public MutableText getActionDisplayName() {
        return super.getActionDisplayName().formatted(Formatting.LIGHT_PURPLE);
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
        var usedBook = (MysticalBookItem) bookStack.getItem();

        if (usedBook.getTypePage(bookStack) instanceof FoodStorageTypePage foodPage) {
            var food = foodPage.removeFirstStack(bookStack, 1);
            if (food.isPresent()) {
                user.eatFood(world, food.get());
                return TypedActionResult.success(bookStack);
            }
        }
        return super.book$use(world, user, hand);
    }
}
