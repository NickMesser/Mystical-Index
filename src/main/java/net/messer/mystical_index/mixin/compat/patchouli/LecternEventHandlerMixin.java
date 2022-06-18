package net.messer.mystical_index.mixin.compat.patchouli;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vazkii.patchouli.common.handler.LecternEventHandler;

import static net.messer.mystical_index.block.ModBlocks.MYSTICAL_LECTERN;

@Mixin(LecternEventHandler.class)
public abstract class LecternEventHandlerMixin {
    @Inject(
            method = "rightClick",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private static void plsDontOverrideOurOnUseFunctionThanks(PlayerEntity player, World world, Hand hand, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        if (world.getBlockState(hit.getBlockPos()).isOf(MYSTICAL_LECTERN)) {
            cir.setReturnValue(ActionResult.PASS);
        }
    }
}
