package net.messer.mystical_index.mixin;

import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.item.ModItems;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.PistonBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
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

import java.util.List;

@Mixin(PistonBlockEntity.class)
public class PistonEntityMixin {
    @Inject(method = "pushEntities", at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private static void tryCrafting(World world, BlockPos pos, float f, PistonBlockEntity blockEntity, CallbackInfo ci, Direction direction, double d, VoxelShape voxelShape, Box box, List<Entity> list){
        var otherBlock = world.getBlockState(blockEntity.getPos().add(direction.getVector()));
        if(direction == Direction.DOWN && otherBlock.getBlock() == Blocks.SANDSTONE_SLAB){
            var itemList = list.stream()
                    .filter(ItemEntity.class::isInstance)
                    .map(ItemEntity.class::cast)
                    .toList();

            if(itemList.size() != 0) {
                var newStack = new ItemStack(ModItems.FOOD_STORAGE_TYPE_PAGE, 1);
                var itemPos = itemList.get(0).getPos();
                for (var item :
                        itemList) {
                    item.getStack().setCount(0);
                }
                world.spawnEntity(new ItemEntity(world,itemPos.x, itemPos.y, itemPos.z, newStack));
                MysticalIndex.LOGGER.info("YAY!");
            }
        }
        MysticalIndex.LOGGER.info(direction);
    }


//    @Inject(at = @At("HEAD"), method = "pushEntities")
//    public void tryCraft(World world, BlockPos pos, float f, PistonBlockEntity blockEntity, CallbackInfo ci){
//        Direction direction = blockEntity.getMovementDirection();
//        double d = (double)(f - blockEntity.progress);
//        this.getHeadBlockState();
//        VoxelShape voxelShape = this.getHeadBlockState().getCollisionShape(world, pos);
//    }
}
