package net.messer.mixin;

import net.messer.mystical_index.item.custom.InventoryBookItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.WrittenBookItem;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.ClickType;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(WrittenBookItem.class)
public abstract class WrittenBookItemMixin extends Item {
    public WrittenBookItemMixin(Settings settings) {
        super(settings);
    }

//    @Override
//    public boolean onStackClicked(ItemStack book, Slot slot, ClickType clickType, PlayerEntity player) {
//        ItemStack itemStack = slot.getStack();
//
//        if (clickType != ClickType.RIGHT || itemStack.isEmpty()) {
//            return false;
//        }
//
//
//        int amount = tryAddItem(book, itemStack);
//        if (amount > 0) {
//            InventoryBookItem.playInsertSound(player);
//            itemStack.decrement(amount);
//        }
//
//        return true;
//    }
//
//    @Override
//    public boolean onClicked(ItemStack book, ItemStack cursorStack, Slot slot, ClickType clickType, PlayerEntity player, StackReference cursorStackReference) {
//        if (clickType != ClickType.RIGHT || !slot.canTakePartial(player)) {
//            return false;
//        }
//        if (cursorStack.isEmpty()) {
//            removeFirstStack(book).ifPresent(itemStack -> {
//                InventoryBookItem.playRemoveOneSound(player);
//                cursorStackReference.set(itemStack);
//            });
//        } else {
//            int amount = tryAddItem(book, cursorStack);
//            if (amount > 0) {
//                InventoryBookItem.playInsertSound(player);
//                cursorStack.decrement(amount);
//            }
//        }
//        return true;
//    }
}
