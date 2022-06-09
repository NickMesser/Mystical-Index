package net.messer.mystical_index.mixin;

import net.messer.mystical_index.block.ModBlocks;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.ShearsItem;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShearsItem.class)
public class ShearsItemMixin {
    @Inject(method = "useOnBlock", at = @At(value = "HEAD"), cancellable = true)
    public void modifyUseOnBlock(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        var world = context.getWorld();
        var pos = context.getBlockPos();

        if (world.getBlockState(pos).isOf(Blocks.BOOKSHELF)) {
            world.setBlockState(pos, ModBlocks.LIBRARY.getDefaultState());
            cir.setReturnValue(ActionResult.success(true));
        }
    }
}
