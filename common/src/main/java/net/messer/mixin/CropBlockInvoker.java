package net.messer.mixin;

import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Opens up {@link CropBlock#getBaseSeedId()}, which is protected.
 *
 * <p>The legacy migration needs it: an old Book of Farming stored only the crop BLOCK it was bound
 * to, while the reworked one stores a seed ITEM in its planting slot. Only the crop itself knows
 * which item plants it - the mapping is not derivable from the block id (beetroots plant from
 * beetroot_seeds, carrots from carrots, nether wart from nether_wart) - so the conversion asks the
 * block rather than guessing.
 */
@Mixin(CropBlock.class)
public interface CropBlockInvoker {

    @Invoker("getBaseSeedId")
    ItemLike invokeGetBaseSeedId();
}
