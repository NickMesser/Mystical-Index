package net.messer.mixin;

import eu.pb4.sgui.api.gui.BookGui;
import net.messer.mystical_index.item.ModItems;
import net.messer.mystical_index.item.custom.Index;
import net.minecraft.block.BlockState;
import net.minecraft.block.LecternBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
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
//            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
            new BookGui((ServerPlayerEntity) player, ((Index) ModItems.INDEX).getMenuItem((ServerPlayerEntity) player, pos)) {
                @Override
                public void onTakeBookButton() {
                    if (!player.canModifyBlocks()) {
                        return;
                    }

                    ItemStack book = blockEntity.getBook();
                    blockEntity.setBook(ItemStack.EMPTY);
//                    blockEntity.markDirty();

                    if (!player.getInventory().insertStack(book)) {
                        player.dropItem(book, false);
                    }
                }
            }.open();
            player.incrementStat(Stats.INTERACT_WITH_LECTERN);
            cir.setReturnValue(ActionResult.success(false));
        }
    }
}
