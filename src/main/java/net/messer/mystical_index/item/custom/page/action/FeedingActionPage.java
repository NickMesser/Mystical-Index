package net.messer.mystical_index.item.custom.page.action;

import net.messer.mystical_index.item.ModItems;
import net.messer.mystical_index.item.custom.book.MysticalBookItem;
import net.messer.mystical_index.item.custom.page.ActionPageItem;
import net.messer.mystical_index.item.custom.page.TypePageItem;
import net.messer.mystical_index.item.custom.page.type.FoodStorageTypePage;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

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

    @Nullable
    @Override
    public ItemStack book$finishUsing(ItemStack book, World world, LivingEntity user) {
        if(world.isClient)
            return super.book$finishUsing(book, world, user);

        var usedBook = (MysticalBookItem) book.getItem();

        if (usedBook.getTypePage(book) instanceof FoodStorageTypePage foodPage) {
            var food = foodPage.removeFirstStack(book, 1);
            if (food.isPresent()) {
                user.eatFood(world, food.get());
                return null;
            }
        }
        return super.book$finishUsing(book, world, user);
    }

    @Override
    public UseAction book$getUseAction(ItemStack book) {
        return UseAction.EAT;
    }

    @Override
    public int book$getMaxUseTime(ItemStack book) {
        return 32;
    }
}
