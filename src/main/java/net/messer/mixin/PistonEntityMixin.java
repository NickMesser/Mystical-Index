package net.messer.mixin;

import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.item.ModItems;
import net.messer.mystical_index.recipe.PistonRecipeInitializer;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.PistonBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
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

import java.util.ArrayList;
import java.util.List;

@Mixin(PistonBlockEntity.class)
public class PistonEntityMixin {
    @Inject(method = "pushEntities", at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private static void tryCrafting(World world, BlockPos pos, float f, PistonBlockEntity blockEntity, CallbackInfo ci, Direction direction, double d, VoxelShape voxelShape, Box box, List list)
    {
        var otherBlock = world.getBlockState(blockEntity.getPos().add(direction.getVector()));
        if(direction == Direction.DOWN && otherBlock.getBlock() == Blocks.IRON_BLOCK){
            var itemEntityList = list.stream()
                    .filter(ItemEntity.class::isInstance)
                    .map(ItemEntity.class::cast)
                    .toList();

            if(itemEntityList.size() == 0) return;

            List<ItemStack> itemStacks = new ArrayList<>();
            for(var entity: itemEntityList){
                var itemEntity = (ItemEntity) entity;
                var itemStack = itemEntity.getStack();
                itemStacks.add(itemStack);
            }

            var itemPos = blockEntity.getPos().up();
            var recipe = PistonRecipeInitializer.getInstance().getRecipe(itemStacks);
            if(recipe == null) return;

            // Consume inputs
            var inputs = recipe.getInputs();
            for(var input: inputs.keySet()){
                var count = inputs.get(input);
                for(int i = 0; i < count; i++){
                    for(var entity: itemEntityList){
                        var itemEntity = (ItemEntity) entity;
                        var itemStack = itemEntity.getStack();
                        if(itemStack.getItem() == input){
                            itemStack.decrement(count);
                            break;
                        }
                    }
                }
            }

            // Output crafted items
            var craftedItems = recipe.getOutputs();
            for(var craftedItem: craftedItems.keySet()){
                var count = craftedItems.get(craftedItem);
                for(int i = 0; i < count; i++){
                    world.spawnEntity(new ItemEntity(world, itemPos.getX(), itemPos.getY(), itemPos.getZ(), new ItemStack(craftedItem)));
                }
            }
        }
    }

}
