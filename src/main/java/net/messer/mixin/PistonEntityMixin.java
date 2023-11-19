package net.messer.mixin;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.events.PistonEntityHook;
import net.messer.mystical_index.item.ModItems;
import net.messer.mystical_index.recipe.PistonRecipeInitializer;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.PistonBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayList;
import java.util.List;

@Mixin(PistonBlockEntity.class)
public class PistonEntityMixin {
    @Inject(method = "pushEntities", at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private static void pushEntities(World world, BlockPos pos, float f, PistonBlockEntity blockEntity, CallbackInfo ci, Direction direction, double d, VoxelShape voxelShape, Box box, List list) {
        PistonEntityHook.tryCrafting(world, pos, f, blockEntity, ci, direction, d, voxelShape, box, list);
    }
}
