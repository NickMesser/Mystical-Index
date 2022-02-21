package net.messer.mixin;

import net.messer.mystical_index.item.ModItems;
import net.messer.mystical_index.item.custom.Index;
import net.minecraft.block.BlockState;
import net.minecraft.block.LecternBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LecternBlock.class)
public class LecternBlockMixin {
    @Inject(method = "onUse", at = @At(value = "INVOKE", target = "Ljava/lang/Boolean;booleanValue()Z", shift = At.Shift.AFTER), cancellable = true)
    public void onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        if (!world.isClient && world.getBlockEntity(pos) instanceof LecternBlockEntity blockEntity && blockEntity.getBook().getItem() == ModItems.INDEX) {
            ((Index) ModItems.INDEX).createMenu((ServerPlayerEntity) player, pos).open();
            player.incrementStat(Stats.INTERACT_WITH_LECTERN);
            cir.setReturnValue(ActionResult.success(false));
        }
    }
}
