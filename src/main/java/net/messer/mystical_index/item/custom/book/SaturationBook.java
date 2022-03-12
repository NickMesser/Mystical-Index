package net.messer.mystical_index.item.custom.book;

import net.messer.mystical_index.MysticalIndex;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class SaturationBook extends InventoryBookItem {
    public SaturationBook(Settings settings) {
        super(settings);
    }

    @Override
    public int getMaxTypes(ItemStack book) {
        return 16;
    }

    @Override
    public int getMaxStack(ItemStack book) {
        return 2;
    }

    @Override
    protected boolean canInsert(Item item) {
        return item.isFood() && super.canInsert(item);
    }

    @Override
    public void inventoryTick(ItemStack book, World world, Entity entity, int slot, boolean selected) {
        if (entity instanceof PlayerEntity player && player.canConsume(false) &&
                !player.getItemCooldownManager().isCoolingDown(this) &&
                !player.isCreative() &&
                !world.isClient) {

            Optional<ItemStack> foodStack = removeFirstStack(book, 1);
            if (foodStack.isPresent() && foodStack.get().isFood()) {
                tryAddItem(book, foodStack.get().finishUsing(world, player));
                player.getItemCooldownManager().set(this, MysticalIndex.CONFIG.BookOfSaturation.TimeBetweenFeedings * 20);
            }
        }

        super.inventoryTick(book, world, entity, slot, selected);
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        return Items.KNOWLEDGE_BOOK;
    }
}
