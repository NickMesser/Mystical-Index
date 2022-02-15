package net.messer.mystical_index.item.custom;

import eu.pb4.polymer.api.item.PolymerItem;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.messer.mystical_index.item.inventory.SingleFluidStackingInventory;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.*;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.tag.FluidTags;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
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

public class FluidBook extends BookItem {
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
                if (fluidStorage.variant.getFluid() == Fluids.EMPTY || fluidStorage.amount <= fluidStorage.getCapacity() && blockState.getBlock() instanceof FluidDrainable) {
                    if (blockState.getBlock() instanceof FluidDrainable) {
                        FluidDrainable fluidDrainable = (FluidDrainable)blockState.getBlock();
                        FluidVariant variant = FluidVariant.of(blockState.getFluidState().getFluid());


                        if(variant != fluidStorage.variant && fluidStorage.variant.getFluid() != Fluids.EMPTY)
                            return super.use(world, user, hand);

                        ItemStack itemStack2 = fluidDrainable.tryDrainFluid(world, blockPos, blockState);
                        if (!itemStack2.isEmpty()) {
                            fluidDrainable.getBucketFillSound().ifPresent((sound) -> {
                                user.playSound(sound, 1.0F, 1.0F);
                            });
                            world.emitGameEvent(user, GameEvent.FLUID_PICKUP, blockPos);

                            try (Transaction transaction = Transaction.openOuter()){
                                fluidStorage.insert(variant, FluidConstants.BUCKET, transaction);
                                transaction.commit();
                            }
                            return super.use(world, user, hand);
                        }
                    }

                    return TypedActionResult.fail(itemStack);
                } else {
                    blockState = world.getBlockState(blockPos);
                    BlockPos fluidDrainable = blockState.getBlock() instanceof FluidFillable && fluidStorage.variant.getFluid() == Fluids.WATER ? blockPos : blockPos2;
                    if (this.placeFluid(user, world, fluidDrainable, blockHitResult, itemStack)) {
                        try(Transaction transaction = Transaction.openOuter()){
                            fluidStorage.extract(fluidStorage.variant, FluidConstants.BUCKET, transaction);
                            transaction.commit();
                        }

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
        if (!(fluidStorage.variant.getFluid() instanceof FlowableFluid)) {
            return false;
        } else {
            BlockState blockState = world.getBlockState(pos);
            Block block = blockState.getBlock();
            Material material = blockState.getMaterial();
            boolean bl = blockState.canBucketPlace(fluidStorage.variant.getFluid());
            boolean bl2 = blockState.isAir() || bl || block instanceof FluidFillable && ((FluidFillable)block).canFillWithFluid(world, pos, blockState, fluidStorage.variant.getFluid());
            if (!bl2) {
                return hitResult != null && this.placeFluid(player, world, hitResult.getBlockPos().offset(hitResult.getSide()), (BlockHitResult)null, fluidBook);
            } else if (world.getDimension().isUltrawarm() && fluidStorage.variant.getFluid().isIn(FluidTags.WATER)) {
                int i = pos.getX();
                int j = pos.getY();
                int k = pos.getZ();
                world.playSound(player, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5F, 2.6F + (world.random.nextFloat() - world.random.nextFloat()) * 0.8F);

                for(int l = 0; l < 8; ++l) {
                    world.addParticle(ParticleTypes.LARGE_SMOKE, (double)i + Math.random(), (double)j + Math.random(), (double)k + Math.random(), 0.0D, 0.0D, 0.0D);
                }

                return true;
            } else if (block instanceof FluidFillable && fluidStorage.variant.getFluid() == Fluids.WATER) {
                ((FluidFillable)block).tryFillWithFluid(world, pos, blockState, ((FlowableFluid)fluidStorage.variant.getFluid()).getStill(false));
                this.playEmptyingSound(player, world, pos, fluidStorage);
                return true;
            } else {
                if (!world.isClient && bl && !material.isLiquid()) {
                    world.breakBlock(pos, true);
                }

                if (!world.setBlockState(pos, fluidStorage.variant.getFluid().getDefaultState().getBlockState(), 11) && !blockState.getFluidState().isStill()) {
                    return false;
                } else {
                    this.playEmptyingSound(player, world, pos, fluidStorage);
                    return true;
                }
            }
        }
    }


    protected void playEmptyingSound(@Nullable PlayerEntity player, WorldAccess world, BlockPos pos, SingleVariantStorage<FluidVariant> fluidStorage) {
        SoundEvent soundEvent = fluidStorage.variant.getFluid().isIn(FluidTags.LAVA) ? SoundEvents.ITEM_BUCKET_EMPTY_LAVA : SoundEvents.ITEM_BUCKET_EMPTY;
        world.playSound(player, pos, soundEvent, SoundCategory.BLOCKS, 1.0F, 1.0F);
        world.emitGameEvent(player, GameEvent.FLUID_PLACE, pos);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        var fluidInventory = new SingleFluidStackingInventory(stack);
        return fluidInventory.IsFluidEmpty();
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        return Items.ENCHANTED_BOOK;
    }
}
