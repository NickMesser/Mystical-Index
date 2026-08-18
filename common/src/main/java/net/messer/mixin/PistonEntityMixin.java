package net.messer.mixin;

import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.events.PistonEntityHook;
import net.messer.mystical_index.item.ModItems;
import net.messer.mystical_index.recipe.PistonRecipeInitializer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayList;
import java.util.List;

@Mixin(PistonMovingBlockEntity.class)
public class PistonEntityMixin {
    /**
     * Mojang name: {@code moveCollidedEntities(Level, BlockPos, float, PistonMovingBlockEntity)}.
     *
     * <p>The captured locals are the ones live at the single {@code List.isEmpty()} call in that
     * method - slots 0-3 are the parameters, then movement, deltaProgress (a double, so it takes
     * two slots), shape, aabb and the entity list. The two locals declared after this point
     * (shapeAabbs, causeBounce) are not yet in scope and so are not captured.
     */
    @Inject(method = "moveCollidedEntities", at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private static void pushEntities(Level world, BlockPos pos, float f, PistonMovingBlockEntity blockEntity, CallbackInfo ci, Direction direction, double d, VoxelShape voxelShape, AABB box, List list) {
        PistonEntityHook.tryCrafting(world, pos, f, blockEntity, ci, direction, d, voxelShape, box, list);
    }
}
