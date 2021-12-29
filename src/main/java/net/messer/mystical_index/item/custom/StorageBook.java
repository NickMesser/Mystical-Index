package net.messer.mystical_index.item.custom;

import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.item.inventory.ItemInventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;


public class StorageBook extends Item  {

    public StorageBook(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if(world.isClient)
            return super.use(world, user, hand);
        ItemStack stack = user.getStackInHand(hand);
        ItemInventory storedItems = new ItemInventory(stack, 5);

        ItemStack newItems = new ItemStack(Items.STONE, 10);
        storedItems.addStack(newItems);

        for(ItemStack items : storedItems.items)
        {
            MysticalIndex.LOGGER.info(items.getCount() + "x " + items.getItem().toString());
            //user.sendMessage(new LiteralText(items.getItem().toString()),false);
        }

        return TypedActionResult.success(user.getStackInHand(hand));
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }
}
