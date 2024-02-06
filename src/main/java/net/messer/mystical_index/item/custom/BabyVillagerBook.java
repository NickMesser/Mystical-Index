package net.messer.mystical_index.item.custom;

import net.messer.mystical_index.item.ModItems;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.village.VillagerType;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BabyVillagerBook extends Item {
    public BabyVillagerBook(Settings settings) {
        super(settings);
    }

    @Override
    public void onCraft(ItemStack stack, World world) {
        if(world.isClient())
            return;

        createAndAddBabyVillagerToBook(stack, (ServerWorld) world);
        super.onCraft(stack, world);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_VILLAGER_YES, SoundCategory.AMBIENT, 1f, 1.5f);
        user.getItemCooldownManager().set(this, 40);
        return super.use(world, user, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if(world.isClient() || !stack.hasNbt())
            return;

        NbtCompound nbt = stack.getNbt();
        VillagerEntity villagerEntity = (VillagerEntity) EntityType.loadEntityWithPassengers(nbt.getCompound("Entity"), world, (entityx) -> entityx);

        if(villagerEntity == null)
            return;

        villagerEntity.setBreedingAge(villagerEntity.getBreedingAge() + 1);
        var timeUntilAdult = nbt.getLong("timeUntilAdult");
        if(world.getTime() > timeUntilAdult)
        {
            if(entity instanceof PlayerEntity player)
            {
                var inventorySlot = player.getInventory().getSlotWithStack(stack);
                player.getInventory().setStack(inventorySlot, ItemStack.EMPTY);

                ItemStack newVillagerStack = new ItemStack(ModItems.VILLAGER_BOOK);
                var villagerBook = (VillagerBook) newVillagerStack.getItem();
                villagerBook.createAndAddVillager(newVillagerStack,(ServerWorld) world);
                player.getInventory().setStack(inventorySlot, newVillagerStack);
                stack.decrement(1);
                villagerEntity.remove(Entity.RemovalReason.DISCARDED);
                return;
            }
        }

        villagerEntity.remove(Entity.RemovalReason.DISCARDED);

        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return stack.hasNbt();
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        NbtCompound nbt = stack.getNbt();

        if(nbt == null) {
            tooltip.add(Text.of("§aCraft to create a baby villager!"));
            return;
        }

        VillagerEntity villagerEntity = (VillagerEntity) EntityType.loadEntityWithPassengers(nbt.getCompound("Entity"), world, (entityx) -> entityx);
        if (villagerEntity == null)
            return;

        var timeUntilAdult = nbt.getLong("timeUntilAdult") - villagerEntity.getEntityWorld().getTime();
        var secondsUntilAdult = timeUntilAdult / 20;
        var minutesUntilAdult = secondsUntilAdult / 60;
        tooltip.add(Text.of("§6Time until adult:§6 " + minutesUntilAdult + " minutes" + " " + secondsUntilAdult % 60 + " seconds"));
        villagerEntity.remove(Entity.RemovalReason.DISCARDED);
        super.appendTooltip(stack, world, tooltip, context);
    }
    public VillagerEntity createChild(ServerWorld serverWorld) {
        VillagerType villagerType = VillagerType.PLAINS;
        VillagerEntity villagerEntity = new VillagerEntity(EntityType.VILLAGER, serverWorld, villagerType);
        villagerEntity.initialize(serverWorld, serverWorld.getLocalDifficulty(villagerEntity.getBlockPos()), SpawnReason.BREEDING, null, null);
        return villagerEntity;
    }


    public void createAndAddBabyVillagerToBook(ItemStack stack, ServerWorld world) {
        NbtCompound nbt = stack.getOrCreateNbt();
        var child = createChild(world);
        var currentTime = world.getTime();
        var timeUntilAdult = currentTime + 24000;
        child.setBreedingAge(-24000);
        NbtCompound entityNbt = new NbtCompound();
        child.saveSelfNbt(entityNbt);
        nbt.put("Entity", entityNbt);
        nbt.putLong("timeUntilAdult", timeUntilAdult);
        child.remove(Entity.RemovalReason.DISCARDED);
        stack.setCustomName(Text.of("Baby Villager Book"));
    }

    public void addBabyVillagerToBook(ItemStack stack, VillagerEntity villagerEntity) {
        var world = villagerEntity.getEntityWorld();
        var timeUntilAdult = world.getTime() + -villagerEntity.getBreedingAge();
        NbtCompound nbt = stack.getOrCreateNbt();
        NbtCompound entityNbt = new NbtCompound();
        villagerEntity.saveSelfNbt(entityNbt);
        nbt.put("Entity", entityNbt);
        nbt.putLong("timeUntilAdult", timeUntilAdult);
        stack.setCustomName(Text.of("Baby Villager Book"));
    }
}
