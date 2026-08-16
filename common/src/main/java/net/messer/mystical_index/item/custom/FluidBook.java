package net.messer.mystical_index.item.custom;

import net.messer.mystical_index.item.inventory.BookFluidTank;
import net.messer.mystical_index.item.inventory.SingleFluidStackingInventory;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.*;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FluidBook extends Item {
    public FluidBook(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if(world.isClient)
            return super.use(world, user, hand);

        ItemStack itemStack = user.getStackInHand(hand);
        var fluidInventory = new SingleFluidStackingInventory(itemStack);
        var fluidStorage = fluidInventory.fluidStorage;
        BlockHitResult blockHitResult = raycast(world, user, fluidStorage.amount != fluidStorage.getCapacity() ? RaycastContext.FluidHandling.SOURCE_ONLY : RaycastContext.FluidHandling.NONE);
        if (blockHitResult.getType() == HitResult.Type.MISS) {
            return TypedActionResult.pass(itemStack);
        } else if (blockHitResult.getType() != HitResult.Type.BLOCK) {
            return TypedActionResult.pass(itemStack);
        } else {
            BlockPos blockPos = blockHitResult.getBlockPos();
            Direction direction = blockHitResult.getSide();
            BlockPos blockPos2 = blockPos.offset(direction);
            if (world.canPlayerModifyAt(user, blockPos) && user.canPlaceOn(blockPos2, direction, itemStack)) {
                var blockState = world.getBlockState(blockPos);
                if (fluidStorage.fluid == Fluids.EMPTY || fluidStorage.amount + BookFluidTank.BUCKET <= fluidStorage.getCapacity() && blockState.getBlock() instanceof FluidDrainable) {
                    if (blockState.getBlock() instanceof FluidDrainable) {
                        FluidDrainable fluidDrainable = (FluidDrainable)blockState.getBlock();
                        Fluid variant = BookFluidTank.normalize(blockState.getFluidState().getFluid());


                        if(variant != fluidStorage.fluid && fluidStorage.fluid != Fluids.EMPTY)
                            return super.use(world, user, hand);

                        // Check there is room *before* draining: tryDrainFluid removes the source
                        // block, so a rejected insert afterwards would destroy the fluid.
                        if (fluidStorage.simulateInsert(variant, BookFluidTank.BUCKET) < BookFluidTank.BUCKET)
                            return TypedActionResult.fail(itemStack);

                        ItemStack itemStack2 = fluidDrainable.tryDrainFluid(user, world, blockPos, blockState);
                        if (!itemStack2.isEmpty()) {
                            fluidDrainable.getBucketFillSound().ifPresent((sound) -> {
                                user.playSound(sound, 1.0F, 1.0F);
                            });
                            world.emitGameEvent(user, GameEvent.FLUID_PICKUP, blockPos);

                            fluidStorage.insert(variant, BookFluidTank.BUCKET);
                            // Name the book after the fluid it now holds (previously a side effect
                            // of reading stored data, which ran every render frame).
                            updateStoredFluidName(itemStack, fluidStorage);
                            return super.use(world, user, hand);
                        }
                    }

                    return TypedActionResult.fail(itemStack);
                } else {
                    blockState = world.getBlockState(blockPos);
                    BlockPos fluidDrainable = blockState.getBlock() instanceof FluidFillable && fluidStorage.fluid == Fluids.WATER ? blockPos : blockPos2;
                    if (this.placeFluid(user, world, fluidDrainable, blockHitResult, itemStack)) {
                        fluidStorage.extract(fluidStorage.fluid, BookFluidTank.BUCKET);
                        // Refresh the book's name, clearing it once the last bucket is drained.
                        updateStoredFluidName(itemStack, fluidStorage);

                        if (user instanceof ServerPlayerEntity) {
                            Criteria.PLACED_BLOCK.trigger((ServerPlayerEntity)user, fluidDrainable, itemStack);
                        }

                        return super.use(world, user, hand);
                    } else {
                        return TypedActionResult.fail(itemStack);
                    }
                }
            } else {
                return TypedActionResult.fail(itemStack);
            }
        }
    }

    public boolean placeFluid(@Nullable PlayerEntity player, World world, BlockPos pos, @Nullable BlockHitResult hitResult, ItemStack fluidBook) {
        var fluidInventory = new SingleFluidStackingInventory(fluidBook);
        var fluidStorage = fluidInventory.fluidStorage;
        if (!(fluidStorage.fluid instanceof FlowableFluid)) {
            return false;
        } else {
            BlockState blockState = world.getBlockState(pos);
            Block block = blockState.getBlock();
            boolean bl = blockState.canBucketPlace(fluidStorage.fluid);
            boolean bl2 = blockState.isAir() || bl || block instanceof FluidFillable && ((FluidFillable)block).canFillWithFluid(player, world, pos, blockState, fluidStorage.fluid);
            if (!bl2) {
                return hitResult != null && this.placeFluid(player, world, hitResult.getBlockPos().offset(hitResult.getSide()), (BlockHitResult)null, fluidBook);
            } else if (world.getDimension().ultrawarm() && fluidStorage.fluid.isIn(FluidTags.WATER)) {
                int i = pos.getX();
                int j = pos.getY();
                int k = pos.getZ();
                world.playSound(player, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5F, 2.6F + (world.random.nextFloat() - world.random.nextFloat()) * 0.8F);

                for(int l = 0; l < 8; ++l) {
                    world.addParticle(ParticleTypes.LARGE_SMOKE, (double)i + Math.random(), (double)j + Math.random(), (double)k + Math.random(), 0.0D, 0.0D, 0.0D);
                }

                return true;
            } else if (block instanceof FluidFillable && fluidStorage.fluid == Fluids.WATER) {
                ((FluidFillable)block).tryFillWithFluid(world, pos, blockState, ((FlowableFluid)fluidStorage.fluid).getStill(false));
                this.playEmptyingSound(player, world, pos, fluidStorage);
                return true;
            } else {
                if (!world.isClient && bl && !blockState.isLiquid()) {
                    world.breakBlock(pos, true);
                }

                if (!world.setBlockState(pos, fluidStorage.fluid.getDefaultState().getBlockState(), 11) && !blockState.getFluidState().isStill()) {
                    return false;
                } else {
                    this.playEmptyingSound(player, world, pos, fluidStorage);
                    return true;
                }
            }
        }
    }


    protected void playEmptyingSound(@Nullable PlayerEntity player, WorldAccess world, BlockPos pos, BookFluidTank fluidStorage) {
        SoundEvent soundEvent = fluidStorage.fluid.isIn(FluidTags.LAVA) ? SoundEvents.ITEM_BUCKET_EMPTY_LAVA : SoundEvents.ITEM_BUCKET_EMPTY;
        world.playSound(player, pos, soundEvent, SoundCategory.BLOCKS, 1.0F, 1.0F);
        world.emitGameEvent(player, GameEvent.FLUID_PLACE, pos);
    }

    // Keeps the book's display name in sync with its contents. Cleared when empty so the item
    // falls back to its default name instead of showing a stale fluid.
    private void updateStoredFluidName(ItemStack stack, BookFluidTank fluidStorage) {
        if(fluidStorage.amount == 0 || fluidStorage.fluid == Fluids.EMPTY){
            stack.remove(DataComponentTypes.CUSTOM_NAME);
            return;
        }

        stack.set(DataComponentTypes.CUSTOM_NAME,
                Text.translatable("item.mystical_index.fluid_book.named", BookFluidTank.getName(fluidStorage.fluid)));
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        var fluidInventory = new SingleFluidStackingInventory(stack);
        return fluidInventory.hasFluid();
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        if(stack.hasGlint()){
            SingleFluidStackingInventory inventory = new SingleFluidStackingInventory(stack);

            tooltip.add(Text.translatable("tooltip.mystical_index.fluid_book.contents",
                    inventory.fluidStorage.amount / BookFluidTank.BUCKET,
                    BookFluidTank.getName(inventory.fluidStorage.fluid)));
            tooltip.add(Text.literal(""));
        }

        tooltip.add(Text.translatable("tooltip.mystical_index.fluid_book"));
        super.appendTooltip(stack, context, tooltip, type);
    }
}
