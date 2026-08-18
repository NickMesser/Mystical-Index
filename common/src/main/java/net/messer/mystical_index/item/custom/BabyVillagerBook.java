package net.messer.mystical_index.item.custom;

import net.minecraft.resources.ResourceKey;
import net.messer.mystical_index.item.ModItems;
import net.messer.util.MysticalUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import net.minecraft.world.entity.EquipmentSlot;

import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;



import net.messer.util.GlintingBook;

public class BabyVillagerBook extends Item implements GlintingBook {
    public BabyVillagerBook(Item.Properties settings) {
        super(settings);
    }

    @Override
    public void onCraftedBy(ItemStack stack, Player player) {
        Level world = player.level();
        if(world.isClientSide())
            return;

        createAndAddBabyVillagerToBook(stack, (ServerLevel) world);
        super.onCraftedBy(stack, player);
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        world.playSound(null, user.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.AMBIENT, 1f, 1.5f);
        user.getCooldowns().addCooldown(user.getItemInHand(hand), 40);
        return super.use(world, user, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, EquipmentSlot slot) {
        if(world.isClientSide() || !MysticalUtil.hasCustomData(stack))
            return;

        CompoundTag nbt = MysticalUtil.getCustomData(stack);

        // getLong returns 0 for a missing key, so without this guard a book carrying unrelated
        // stored data would "mature" on the next tick.
        if(!nbt.contains("timeUntilAdult"))
            return;

        // Growth is driven entirely by the stored timestamp. Loading the villager here just to
        // bump a breeding age that was never written back allocated an entity every tick.
        var timeUntilAdult = nbt.getLongOr("timeUntilAdult", 0L);
        if(world.getGameTime() > timeUntilAdult && entity instanceof Player player)
        {
            ItemStack newVillagerStack = new ItemStack(ModItems.VILLAGER_BOOK.get());
            // Carry the captured villager over (biome type, name, trades, xp) instead of spawning
            // a fresh plains villager, and clear the breeding age so the matured book holds an
            // adult. This mirrors how addVillagerToBook stores the villager under "Entity".
            CompoundTag entityNbt = nbt.getCompoundOrEmpty("Entity").copy();
            entityNbt.putInt("Age", 0);
            CompoundTag newNbt = MysticalUtil.getOrCreateCustomData(newVillagerStack);
            newNbt.put("Entity", entityNbt);
            MysticalUtil.setCustomData(newVillagerStack, newNbt);
            newVillagerStack.set(DataComponents.CUSTOM_NAME, Component.translatable("item.mystical_index.villager_book"));
            // getSlotWithStack matches on item type only, so it could return a different baby
            // book's slot, or -1 when the book is not in the main inventory.
            stack.shrink(1);
            player.getInventory().placeItemBackInInventory(newVillagerStack);
            return;
        }

        super.inventoryTick(stack, world, entity, slot);
    }
    @Override
    public boolean shouldGlint(ItemStack stack) {
        return MysticalUtil.hasCustomData(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag type) {
        CompoundTag nbt = MysticalUtil.getCustomData(stack);

        if(nbt == null) {
            tooltip.accept(Component.translatable("tooltip.mystical_index.baby_villager_book.craft"));
            return;
        }

        var world = MysticalUtil.tooltipWorld();
        if (world == null)
            return;

        // Only the remaining time is needed, so read it straight from NBT instead of
        // deserialising a villager on every tooltip frame.
        var timeUntilAdult = Math.max(0, nbt.getLongOr("timeUntilAdult", 0L) - world.getGameTime());
        var secondsUntilAdult = timeUntilAdult / 20;
        var minutesUntilAdult = secondsUntilAdult / 60;
        tooltip.accept(Component.translatable("tooltip.mystical_index.baby_villager_book.time_until_adult", minutesUntilAdult, secondsUntilAdult % 60));
        super.appendHoverText(stack, context, display, tooltip, type);
    }
    public Villager createChild(ServerLevel serverWorld) {
        ResourceKey<VillagerType> villagerType = VillagerType.PLAINS;
        Villager villagerEntity = new Villager(EntityType.VILLAGER, serverWorld, villagerType);
        villagerEntity.finalizeSpawn(serverWorld, serverWorld.getCurrentDifficultyAt(villagerEntity.blockPosition()), EntitySpawnReason.BREEDING, null);
        return villagerEntity;
    }


    public void createAndAddBabyVillagerToBook(ItemStack stack, ServerLevel world) {
        CompoundTag nbt = MysticalUtil.getOrCreateCustomData(stack);
        var child = createChild(world);
        var currentTime = world.getGameTime();
        var timeUntilAdult = currentTime + 24000;
        child.setAge(-24000);
        CompoundTag entityNbt = MysticalUtil.saveEntityWithId(child);
        if (entityNbt == null)
            return;

        nbt.put("Entity", entityNbt);
        nbt.putLong("timeUntilAdult", timeUntilAdult);
        MysticalUtil.setCustomData(stack, nbt);
        child.remove(Entity.RemovalReason.DISCARDED);
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable("item.mystical_index.baby_villager_book"));
    }

    public void addBabyVillagerToBook(ItemStack stack, Villager villagerEntity) {
        var world = villagerEntity.level();
        var timeUntilAdult = world.getGameTime() + -villagerEntity.getAge();
        CompoundTag nbt = MysticalUtil.getOrCreateCustomData(stack);
        CompoundTag entityNbt = MysticalUtil.saveEntityWithId(villagerEntity);
        if (entityNbt == null)
            return;

        nbt.put("Entity", entityNbt);
        nbt.putLong("timeUntilAdult", timeUntilAdult);
        MysticalUtil.setCustomData(stack, nbt);
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable("item.mystical_index.baby_villager_book"));
    }
}
