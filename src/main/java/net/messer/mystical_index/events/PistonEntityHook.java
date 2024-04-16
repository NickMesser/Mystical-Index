package net.messer.mystical_index.events;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.messer.mystical_index.item.ModItems;
import net.messer.mystical_index.item.custom.HostileBook;
import net.messer.mystical_index.item.custom.base_books.BaseGeneratingBook;
import net.messer.mystical_index.recipe.PistonRecipeInitializer;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.PistonBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
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
            List<ItemEntity> itemEntityList = new ArrayList(list.stream()
                    .filter(ItemEntity.class::isInstance)
                    .map(ItemEntity.class::cast)
                    .toList());

            if(itemEntityList.size() == 0) return;

            List<ItemStack> itemStacks = new ArrayList<>();

            // add items to stacks list
            for(var entity: itemEntityList){
                var itemStack = entity.getStack();
                itemStacks.add(itemStack);
            }

            for(var stack: itemStacks){
                if(stack.getItem() instanceof BaseGeneratingBook){
                    if(!stack.hasNbt())
                    {
                        String entityId = "";
                        for (var stack2: itemStacks){
                            if(stack2.getItem() == ModItems.ENTITY_PAPER){
                                var compound = stack2.getNbt();
                                if(compound == null)
                                    continue;
                                entityId = compound.getString("entity");
                            }
                        }
                        if(stack.getItem() instanceof HostileBook hostileBook && !entityId.equals("")){
                            hostileBook.addEntityToBook(stack, entityId);
                            hostileBook.increaseKills(stack, 1);
                            for(var item : itemStacks){
                                if(item.getItem() != ModItems.ENTITY_PAPER)
                                    continue;

                                item.setCount(0);
                            }
                            return;
                        }
                    }

                    var compound = stack.getNbt();
                    if(compound.contains("storedEntityId")){
                        var storedEntityId = compound.getString("storedEntityId");
                        // Check if other input items have matching nbt
                        boolean allNbtMatch = true;
                        int matchingItems = 0;
                        for(var item : itemStacks){
                            if(item.getItem() != ModItems.ENTITY_PAPER)
                                continue;

                            if(!item.hasNbt())
                                allNbtMatch = false;

                            if(!item.getNbt().getString("entity").equals(storedEntityId))
                                allNbtMatch = false;

                            matchingItems++;
                        }
                        if(allNbtMatch & compound.contains("numberOfKills")){
                            var numberOfKills = compound.getInt("numberOfKills");
                            compound.putInt("numberOfKills", numberOfKills + matchingItems);
                            for(var item : itemStacks){
                                if(item.getItem() != ModItems.ENTITY_PAPER)
                                    continue;

                                item.setCount(0);
                            }
                            world.playSound(null, blockEntity.getPos().getX(), blockEntity.getPos().getY(), blockEntity.getPos().getZ(), SoundEvents.BLOCK_SLIME_BLOCK_PLACE, SoundCategory.BLOCKS, 2f, 2f);
                            ((ServerWorld) world).spawnParticles(ParticleTypes.FLASH, blockEntity.getPos().getX(), blockEntity.getPos().getY(), blockEntity.getPos().getZ(), 1, 0.5, 0.5, 0.5, 0.1);
                            return;
                        }
                    }
                }
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
