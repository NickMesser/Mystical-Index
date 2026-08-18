package net.messer.mystical_index.item.custom;

import net.messer.mystical_index.screen.SaturationScreenHandler;
import net.messer.util.SelfUpdatingBook;
import net.messer.config.ModConfig;
import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.item.ModItems;
import net.messer.mystical_index.item.inventory.SingleItemStackingInventory;
import net.minecraft.core.component.DataComponents;
import net.minecraft.client.gui.screens.Screen;
import net.messer.mystical_index.item.inventory.BookContentsTooltipData;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

import net.minecraft.client.Minecraft;

import net.minecraft.server.level.ServerLevel;

import net.minecraft.world.entity.EquipmentSlot;

import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

import net.messer.util.GlintingBook;

public class SaturationBook extends Item implements GlintingBook, SelfUpdatingBook {
    public SaturationBook(Item.Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        if(world.isClientSide())
            return super.use(world, player, hand);

        ItemStack stack = player.getItemInHand(hand);
        if(player.isShiftKeyDown()){
            var hitResult = player.pick(10, 0, false);
            if (hitResult.getType() == HitResult.Type.MISS)
                return InteractionResult.PASS;

            var inventory = new SingleItemStackingInventory(stack, ModConfig.SaturationBookMaxStacks);
            if(inventory.isEmpty()){
                var box = AABB.unitCubeFromLowerCorner(hitResult.getLocation()).inflate(.5);

                for(Entity entity : world.getEntitiesOfClass(ItemEntity.class, box)){
                    ItemEntity item = (ItemEntity) entity;
                    var hitStack = item.getItem();
                    if(hitStack.has(DataComponents.FOOD)){
                        inventory.setCurrentlyStoredItem(hitStack.getItem());
                        return InteractionResult.PASS;
                    }
                }
            }
            else{
                player.sendOverlayMessage(Component.translatable("message.mystical_index.empty_first"));
            }

            return super.use(world, player, hand);
        }

        // Plain right-click opens the food slot. The sneak gesture above is untouched: it still
        // binds a food type off a dropped item, which is the no-UI way to load the book.
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new net.minecraft.world.SimpleMenuProvider(
                    (syncId, inventory, viewer) -> new SaturationScreenHandler(syncId, inventory),
                    Component.translatable("container.mystical_index.saturation")));
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, EquipmentSlot slot) {
        if(entity instanceof Player player && player.canEat(false) &&
                !player.getCooldowns().isOnCooldown(stack) &&
                !player.isCreative() &&
                !world.isClientSide()){

            var inventory = new SingleItemStackingInventory(stack,ModConfig.SaturationBookMaxStacks);
            var foodStack = inventory.getFirstItemStack();
            var foodComponent = foodStack.get(DataComponents.FOOD);
            if(!foodStack.isEmpty() && foodComponent != null){
                player.getFoodData().eat(foodComponent);
                foodStack.shrink(1);
                inventory.setChanged();
                player.getCooldowns().addCooldown(stack, ModConfig.SaturationBookTimeBetweenFeedings * 20);
            }
        }

        super.inventoryTick(stack, world, entity, slot);
    }
    @Override
    public boolean shouldGlint(ItemStack stack) {
        var storageInventory = new SingleItemStackingInventory(stack, ModConfig.SaturationBookMaxStacks);
        return !storageInventory.isEmpty();
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        var storageInventory = new SingleItemStackingInventory(stack, ModConfig.SaturationBookMaxStacks);
        if(storageInventory.isEmpty())
            return Optional.empty();


        return Optional.of(BookContentsTooltipData.fromInventory(storageInventory));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag type) {
        if(Minecraft.getInstance().hasShiftDown()){
            tooltip.accept(Component.translatable("tooltip.mystical_index.saturation_book_shift0"));
            tooltip.accept(Component.translatable("tooltip.mystical_index.saturation_book_shift1"));
        } else {
            tooltip.accept(Component.translatable("tooltip.mystical_index.saturation_book"));
        }

        super.appendHoverText(stack, context, display, tooltip, type);
    }
}
