package net.messer.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Opens up {@link ItemEntity}'s private {@code pickupDelay} counter.
 *
 * <p>The public API only offers {@code hasPickUpDelay()}, which is {@code pickupDelay > 0} and so
 * cannot tell a freshly dropped item (a 40 tick countdown that expires on its own) apart from one
 * marked by {@code setNeverPickUp()}, which parks the field at 32767 forever. Blocks in this mod
 * collect on hopper terms - a countdown is no reason to ignore an item - but "never pick this up"
 * still has to be honoured, and that distinction is invisible without reading the raw value.
 *
 * <p>Vanilla hoppers do not draw the line at all: {@code HopperBlockEntity.getItemsAtAndAbove}
 * filters solely on {@code EntitySelector.ENTITY_STILL_ALIVE}, so a hopper will happily swallow an
 * item another mod marked never-pick-up. We deliberately do not copy that.
 */
@Mixin(ItemEntity.class)
public interface ItemEntityAccessor {

    @Accessor("pickupDelay")
    int getPickupDelay();
}
