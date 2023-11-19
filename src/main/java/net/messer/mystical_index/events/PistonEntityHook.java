package net.messer.mystical_index.events;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.messer.mystical_index.recipe.PistonRecipeInitializer;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.PistonBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

public class PistonEntityHook {
    public static void tryCrafting(World world, BlockPos pos, float f, PistonBlockEntity blockEntity, CallbackInfo ci, Direction direction, double d, VoxelShape voxelShape, Box box, List list) {
        if(world.isClient)
            return;

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
                var itemEntry = inputs.get(input);
                for(var entity: itemEntityList){
                    var itemEntity = (ItemEntity) entity;
                    var itemStack = itemEntity.getStack();
                    if(itemStack.getItem() == input){
                        itemStack.decrement(itemEntry.count);
                        break;
                    }
                }
            }

            // Output crafted items
            var craftedItems = recipe.getOutputs();
            for(var craftedItem: craftedItems.keySet()){
                var itemEntry = craftedItems.get(craftedItem);
                var itemStack = new ItemStack(craftedItem, itemEntry.count);
                itemStack.setNbt(itemEntry.nbt.orElse(null));
                itemStack.onCraft(world, FakePlayer.get((ServerWorld) world), itemStack.getCount());
                var itemEntity = new ItemEntity(world, itemPos.getX(), itemPos.getY(), itemPos.getZ(), itemStack);
                world.spawnEntity(itemEntity);
                world.playSound(null, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), SoundEvents.BLOCK_SLIME_BLOCK_PLACE, SoundCategory.BLOCKS, 2f, 2f);
                ((ServerWorld) world).spawnParticles(ParticleTypes.FLASH, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), 1, 0.5, 0.5, 0.5, 0.1);
            }
        }
    }
}
