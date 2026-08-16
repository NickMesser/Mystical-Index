package net.messer.mystical_index.events;

import net.messer.util.FakePlayerAccess;
import net.messer.config.ModConfig;
import net.messer.mystical_index.item.ModItems;
import net.messer.mystical_index.item.custom.HostileBook;
import net.messer.mystical_index.recipe.PistonRecipeInitializer;
import net.messer.util.MysticalUtil;
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
                if(stack.getItem() instanceof HostileBook hostileBook){
                    if(!MysticalUtil.hasCustomData(stack))
                    {
                        String entityId = "";
                        for (var stack2: itemStacks){
                            if(stack2.getItem() == ModItems.ENTITY_PAPER.get()){
                                var compound = MysticalUtil.getCustomData(stack2);
                                if(compound == null)
                                    continue;
                                entityId = compound.getString("entity");
                            }
                        }
                        if(!entityId.equals("")){
                            hostileBook.addEntityToBook(stack, entityId, world);
                            // Credit one kill per paper item consumed (capped), never destroying
                            // papers we did not credit.
                            if(chargeBookWithPapers(hostileBook, stack, entityId, itemStacks) > 0)
                                playChargeEffects(world, blockEntity.getPos());
                            return;
                        }
                    } else {
                        var compound = MysticalUtil.getCustomData(stack);
                        if(compound != null && compound.contains("storedEntityId")){
                            var storedEntityId = compound.getString("storedEntityId");
                            // pushEntities can fire several times per piston cycle; re-fires find the
                            // matching papers already at count 0 and so credit nothing.
                            if(chargeBookWithPapers(hostileBook, stack, storedEntityId, itemStacks) > 0){
                                playChargeEffects(world, blockEntity.getPos());
                                return;
                            }
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
                itemEntry.nbt.ifPresent(nbt -> MysticalUtil.setCustomData(itemStack, nbt.copy()));
                itemStack.onCraftByPlayer(world, FakePlayerAccess.get((ServerWorld) world), itemStack.getCount());
                var itemEntity = new ItemEntity(world, itemPos.getX(), itemPos.getY(), itemPos.getZ(), itemStack);
                world.spawnEntity(itemEntity);
                world.playSound(null, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), SoundEvents.BLOCK_SLIME_BLOCK_PLACE, SoundCategory.BLOCKS, 2f, 2f);
                ((ServerWorld) world).spawnParticles(ParticleTypes.FLASH, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), 1, 0.5, 0.5, 0.5, 0.1);
            }
        }
    }

    // Credits one kill per matching entity-paper item (counting item counts, not stacks), clamped
    // to ModConfig.HostileBookMaxKills via HostileBook.increaseKills, and decrements exactly the
    // papers that were credited. Papers already at count 0 (from an earlier fire of the same piston
    // cycle) are skipped, so re-fires credit nothing. Returns the number of kills credited.
    private static int chargeBookWithPapers(HostileBook book, ItemStack bookStack, String storedEntityId, List<ItemStack> itemStacks){
        var compound = MysticalUtil.getCustomData(bookStack);
        if(compound == null)
            return 0;

        int remaining = ModConfig.HostileBookMaxKills - compound.getInt("numberOfKills");
        if(remaining <= 0)
            return 0;

        int credited = 0;
        for(var item : itemStacks){
            if(remaining <= 0)
                break;
            if(item.getItem() != ModItems.ENTITY_PAPER.get())
                continue;
            if(item.getCount() <= 0)
                continue;

            var paperNbt = MysticalUtil.getCustomData(item);
            if(paperNbt == null || !paperNbt.getString("entity").equals(storedEntityId))
                continue;

            int take = Math.min(item.getCount(), remaining);
            item.decrement(take);
            remaining -= take;
            credited += take;
        }

        if(credited > 0)
            book.increaseKills(bookStack, credited);

        return credited;
    }

    private static void playChargeEffects(World world, BlockPos pos){
        world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.BLOCK_SLIME_BLOCK_PLACE, SoundCategory.BLOCKS, 2f, 2f);
        ((ServerWorld) world).spawnParticles(ParticleTypes.FLASH, pos.getX(), pos.getY(), pos.getZ(), 1, 0.5, 0.5, 0.5, 0.1);
    }
}
