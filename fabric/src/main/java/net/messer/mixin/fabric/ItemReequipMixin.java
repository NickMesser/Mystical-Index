package net.messer.mixin.fabric;

import net.fabricmc.fabric.api.item.v1.FabricItem;
import net.messer.util.SelfUpdatingBook;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Stops our self-updating books from re-playing the equip animation every time they write.
 *
 * <p>Fabric injects {@link FabricItem} into {@code Item}, so the veto has to be added at that level;
 * a mod class in common cannot see the interface at all. Targeting {@code Item} rather than our own
 * base class is deliberate - the books do not share one base (several extend {@code Item} directly)
 * and the marker test is a single instanceof, so nothing else is affected.
 */
@Mixin(Item.class)
public abstract class ItemReequipMixin implements FabricItem {

    @Override
    public boolean allowComponentsUpdateAnimation(Player player, InteractionHand hand,
                                                  ItemStack oldStack, ItemStack newStack) {
        // Only silence the animation when the SAME book is updating itself in place. A different
        // item, or the same item arriving in a different slot, still animates as vanilla intends.
        if (oldStack.getItem() instanceof SelfUpdatingBook && oldStack.getItem() == newStack.getItem())
            return false;

        return true;
    }
}
