package net.messer.mystical_index.item.custom;

import net.messer.util.SelfUpdatingBook;
import net.messer.mystical_index.item.inventory.BookFluidTank;
import net.messer.mystical_index.item.inventory.SingleFluidStackingInventory;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

import net.messer.util.GlintingBook;

public class FluidBook extends Item implements GlintingBook, SelfUpdatingBook {
    public FluidBook(Item.Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        if(world.isClientSide())
            return super.use(world, user, hand);

        ItemStack itemStack = user.getItemInHand(hand);
        var fluidInventory = new SingleFluidStackingInventory(itemStack);
        var fluidStorage = fluidInventory.fluidStorage;
        BlockHitResult blockHitResult = getPlayerPOVHitResult(world, user, fluidStorage.amount != fluidStorage.getCapacity() ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE);
        if (blockHitResult.getType() == HitResult.Type.MISS) {
            return InteractionResult.PASS;
        } else if (blockHitResult.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.PASS;
        } else {
            BlockPos blockPos = blockHitResult.getBlockPos();
            Direction direction = blockHitResult.getDirection();
            BlockPos blockPos2 = blockPos.relative(direction);
            if (world.mayInteract(user, blockPos) && user.mayUseItemAt(blockPos2, direction, itemStack)) {
                var blockState = world.getBlockState(blockPos);
                if (fluidStorage.fluid == Fluids.EMPTY || fluidStorage.amount + BookFluidTank.BUCKET <= fluidStorage.getCapacity() && blockState.getBlock() instanceof BucketPickup) {
                    if (blockState.getBlock() instanceof BucketPickup) {
                        BucketPickup fluidDrainable = (BucketPickup)blockState.getBlock();
                        Fluid variant = BookFluidTank.normalize(blockState.getFluidState().getType());


                        if(variant != fluidStorage.fluid && fluidStorage.fluid != Fluids.EMPTY)
                            return super.use(world, user, hand);

                        // Check there is room *before* draining: tryDrainFluid removes the source
                        // block, so a rejected insert afterwards would destroy the fluid.
                        if (fluidStorage.simulateInsert(variant, BookFluidTank.BUCKET) < BookFluidTank.BUCKET)
                            return InteractionResult.FAIL;

                        ItemStack itemStack2 = fluidDrainable.pickupBlock(user, world, blockPos, blockState);
                        if (!itemStack2.isEmpty()) {
                            fluidDrainable.getPickupSound().ifPresent((sound) -> {
                                user.playSound(sound, 1.0F, 1.0F);
                            });
                            world.gameEvent(user, GameEvent.FLUID_PICKUP, blockPos);

                            fluidStorage.insert(variant, BookFluidTank.BUCKET);
                            // Name the book after the fluid it now holds (previously a side effect
                            // of reading stored data, which ran every render frame).
                            updateStoredFluidName(itemStack, fluidStorage);
                            return super.use(world, user, hand);
                        }
                    }

                    return InteractionResult.FAIL;
                } else {
                    blockState = world.getBlockState(blockPos);
                    BlockPos fluidDrainable = blockState.getBlock() instanceof LiquidBlockContainer && fluidStorage.fluid == Fluids.WATER ? blockPos : blockPos2;
                    if (this.placeFluid(user, world, fluidDrainable, blockHitResult, itemStack)) {
                        fluidStorage.extract(fluidStorage.fluid, BookFluidTank.BUCKET);
                        // Refresh the book's name, clearing it once the last bucket is drained.
                        updateStoredFluidName(itemStack, fluidStorage);

                        if (user instanceof ServerPlayer) {
                            CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayer)user, fluidDrainable, itemStack);
                        }

                        return super.use(world, user, hand);
                    } else {
                        return InteractionResult.FAIL;
                    }
                }
            } else {
                return InteractionResult.FAIL;
            }
        }
    }

    public boolean placeFluid(@Nullable Player player, Level world, BlockPos pos, @Nullable BlockHitResult hitResult, ItemStack fluidBook) {
        var fluidInventory = new SingleFluidStackingInventory(fluidBook);
        var fluidStorage = fluidInventory.fluidStorage;
        if (!(fluidStorage.fluid instanceof FlowingFluid)) {
            return false;
        } else {
            BlockState blockState = world.getBlockState(pos);
            Block block = blockState.getBlock();
            boolean bl = blockState.canBeReplaced(fluidStorage.fluid);
            boolean bl2 = blockState.isAir() || bl || block instanceof LiquidBlockContainer && ((LiquidBlockContainer)block).canPlaceLiquid(player, world, pos, blockState, fluidStorage.fluid);
            if (!bl2) {
                return hitResult != null && this.placeFluid(player, world, hitResult.getBlockPos().relative(hitResult.getDirection()), (BlockHitResult)null, fluidBook);
            // The dimension's "ultrawarm" flag is gone; whether water boils off is now an
            // environment attribute read per position, which is the same condition vanilla's
            // bucket checks before evaporating water in the Nether.
            } else if (world.environmentAttributes().getValue(EnvironmentAttributes.WATER_EVAPORATES, pos)
                    && fluidStorage.fluid.is(FluidTags.WATER)) {
                int i = pos.getX();
                int j = pos.getY();
                int k = pos.getZ();
                world.playSound(player, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F + (world.getRandom().nextFloat() - world.getRandom().nextFloat()) * 0.8F);

                for(int l = 0; l < 8; ++l) {
                    world.addParticle(ParticleTypes.LARGE_SMOKE, (double)i + Math.random(), (double)j + Math.random(), (double)k + Math.random(), 0.0D, 0.0D, 0.0D);
                }

                return true;
            } else if (block instanceof LiquidBlockContainer && fluidStorage.fluid == Fluids.WATER) {
                ((LiquidBlockContainer)block).placeLiquid(world, pos, blockState, ((FlowingFluid) fluidStorage.fluid).getSource(false));
                this.playEmptyingSound(player, world, pos, fluidStorage);
                return true;
            } else {
                if (!world.isClientSide() && bl && !blockState.liquid()) {
                    world.destroyBlock(pos, true);
                }

                if (!world.setBlock(pos, fluidStorage.fluid.defaultFluidState().createLegacyBlock(), 11) && !blockState.getFluidState().isSource()) {
                    return false;
                } else {
                    this.playEmptyingSound(player, world, pos, fluidStorage);
                    return true;
                }
            }
        }
    }


    protected void playEmptyingSound(@Nullable Player player, LevelAccessor world, BlockPos pos, BookFluidTank fluidStorage) {
        SoundEvent soundEvent = fluidStorage.fluid.is(FluidTags.LAVA) ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY;
        world.playSound(player, pos, soundEvent, SoundSource.BLOCKS, 1.0F, 1.0F);
        world.gameEvent(player, GameEvent.FLUID_PLACE, pos);
    }

    // Keeps the book's display name in sync with its contents. Cleared when empty so the item
    // falls back to its default name instead of showing a stale fluid.
    private void updateStoredFluidName(ItemStack stack, BookFluidTank fluidStorage) {
        if(fluidStorage.amount == 0 || fluidStorage.fluid == Fluids.EMPTY){
            stack.remove(DataComponents.CUSTOM_NAME);
            return;
        }

        stack.set(DataComponents.CUSTOM_NAME,
                Component.translatable("item.mystical_index.fluid_book.named", BookFluidTank.getName(fluidStorage.fluid)));
    }
    @Override
    public boolean shouldGlint(ItemStack stack) {
        var fluidInventory = new SingleFluidStackingInventory(stack);
        return fluidInventory.hasFluid();
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag type) {
        if(stack.hasFoil()){
            SingleFluidStackingInventory inventory = new SingleFluidStackingInventory(stack);

            tooltip.accept(Component.translatable("tooltip.mystical_index.fluid_book.contents",
                    inventory.fluidStorage.amount / BookFluidTank.BUCKET,
                    BookFluidTank.getName(inventory.fluidStorage.fluid)));
            tooltip.accept(Component.literal(""));
        }

        tooltip.accept(Component.translatable("tooltip.mystical_index.fluid_book"));
        super.appendHoverText(stack, context, display, tooltip, type);
    }
}
