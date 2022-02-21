package net.messer.mystical_index.item.custom;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.item.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ActionResult;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class BuildingBook extends InventoryBookItem {
    public BuildingBook(Settings settings) {
        super(settings);
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        return Items.ENCHANTED_BOOK;
    }

    @Override
    public int getMaxTypes() {
        return 1;
    }

    @Override
    public int getMaxStack() {
        return 8;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if(context.getWorld().isClient)
            return super.useOnBlock(context);

        var player = context.getPlayer();
        if (player == null) {
            return super.useOnBlock(context);
        }
        var hand = context.getHand();
        var heldBookStack = player.getStackInHand(hand);

        if (player.isSneaking() || isEmpty(heldBookStack)) {
            return super.useOnBlock(context);
        }

        var stackFromBook = removeFirstStack(heldBookStack, 1);

        if (stackFromBook.isPresent()) { // TODO stack.useOnBlock?
            if (stackFromBook.get().getItem() instanceof BlockItem blockItem) {
                var hitBlockPos = context.getBlockPos();
                var direction = context.getSide();
                var newBlockPos = hitBlockPos.offset(direction);
                var world = context.getWorld();
                if (world.canPlayerModifyAt(player, newBlockPos) && player.canPlaceOn(newBlockPos, direction, stackFromBook.get()) && world.canSetBlock(newBlockPos)) {
                    var soundEvent = blockItem.getBlock().getSoundGroup(null).getPlaceSound();
                    context.getWorld().playSound(null, newBlockPos, soundEvent, SoundCategory.BLOCKS, 1.0f, 1.0f);
                    context.getWorld().setBlockState(newBlockPos, blockItem.getBlock().getDefaultState());
                    player.swingHand(context.getHand(), true);
                }
            } else {
                tryAddItem(heldBookStack, stackFromBook.get());
            }
        }

        return super.useOnBlock(context);
    }
}
