package net.messer.mixin;

import net.messer.mystical_index.events.MixinHooks;
import net.messer.mystical_index.item.custom.Index;
import net.messer.mystical_index.util.LecternTracker;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.LecternBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LecternBlock.class)
public abstract class LecternBlockMixin extends BlockWithEntity {
    protected LecternBlockMixin(Settings settings) {
        super(settings);
    }

    @ModifyVariable(
            method = "dropBook",
            at = @At(value = "STORE")
    )
    public ItemStack modifyDroppedItem(ItemStack i) {
        return Index.getFromLecternBook(i);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return checkType(type, BlockEntityType.LECTERN, MixinHooks::lecternTick);
    }

    @Override
    public void afterBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack stack) {
        if (blockEntity instanceof LecternBlockEntity lectern)
            LecternTracker.removeIndexLectern(lectern);
        super.afterBreak(world, player, pos, state, blockEntity, stack);
    }
}
