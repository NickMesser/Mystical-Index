package net.messer.mixin.neoforge;

import net.messer.util.SelfUpdatingBook;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.extensions.IItemExtension;
import org.spongepowered.asm.mixin.Mixin;

/**
 * The NeoForge half of the reequip suppression - see the Fabric copy for the reasoning.
 *
 * <p>NeoForge's hook takes the slot-changed flag directly, so the "a move between slots must still
 * animate" rule is expressed by simply honouring it rather than inferred.
 */
@Mixin(Item.class)
// Abstract: IItemExtension is not all-default (isCombineRepairable is abstract), and the
// mixin never needs to be instantiable - it is merged into Item, which supplies the rest.
public abstract class ItemReequipMixin implements IItemExtension {

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        if (!slotChanged
                && oldStack.getItem() instanceof SelfUpdatingBook
                && oldStack.getItem() == newStack.getItem())
            return false;

        return !ItemStack.isSameItemSameComponents(oldStack, newStack) || slotChanged;
    }
}
