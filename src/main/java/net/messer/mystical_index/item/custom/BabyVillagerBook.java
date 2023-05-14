package net.messer.mystical_index.item.custom;

import net.messer.mixin.PassiveEntityAccessor;
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
import net.minecraft.text.Text;
import net.minecraft.village.VillagerType;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BabyVillagerBook extends Item {
    public BabyVillagerBook(Settings settings) {
        super(settings);
    }

    @Override
    public void onCraft(ItemStack stack, World world, PlayerEntity player) {
        if(world.isClient())
            return;

        NbtCompound compound = stack.getOrCreateNbt();
        var child = createChild((ServerWorld) world);
        child.setBreedingAge(-24000);
        NbtCompound entityNbt = new NbtCompound();
        child.saveSelfNbt(entityNbt);
        compound.put("Entity", entityNbt);
        child.remove(Entity.RemovalReason.DISCARDED);
        super.onCraft(stack, world, player);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if(world.isClient() || !stack.hasNbt())
            return;

        NbtCompound nbt = stack.getNbt();
        VillagerEntity villagerEntity = (VillagerEntity) EntityType.loadEntityWithPassengers(nbt.getCompound("Entity"), world, (entityx) -> {entityx.refreshPositionAndAngles(entity.getX(), entity.getY(), entity.getZ(), entityx.getYaw(), entityx.getPitch());
            return entityx;
        });

        if(villagerEntity == null)
            return;

        villagerEntity.setBreedingAge(villagerEntity.getBreedingAge() + 1);

        if(!villagerEntity.isBaby())
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

        NbtCompound entityNbt = new NbtCompound();
        villagerEntity.saveSelfNbt(entityNbt);
        nbt.remove("Entity");
        nbt.put("Entity", entityNbt);
        villagerEntity.remove(Entity.RemovalReason.DISCARDED);

        super.inventoryTick(stack, world, entity, slot, selected);
    }

    public VillagerEntity createChild(ServerWorld serverWorld) {
        VillagerType villagerType = VillagerType.PLAINS;
        VillagerEntity villagerEntity = new VillagerEntity(EntityType.VILLAGER, serverWorld, villagerType);
        villagerEntity.initialize(serverWorld, serverWorld.getLocalDifficulty(villagerEntity.getBlockPos()), SpawnReason.BREEDING, null, null);
        return villagerEntity;
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return stack.hasNbt();
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        NbtCompound nbt = stack.getNbt();

        if(nbt == null) {
            tooltip.add(Text.of("Craft to create a baby villager!"));
            return;
        }

        VillagerEntity villagerEntity = (VillagerEntity) EntityType.loadEntityWithPassengers(nbt.getCompound("Entity"), world, (entityx) -> {
            return entityx;
        });
        if (villagerEntity == null)
            return;

        PassiveEntityAccessor passiveEntityAccessor = (PassiveEntityAccessor) villagerEntity;


        var timeUntilAdult = -passiveEntityAccessor.getBreedingAgeNumber();
        var secondsUntilAdult = timeUntilAdult / 20;
        var minutesUntilAdult = secondsUntilAdult / 60;
        tooltip.add(Text.of("Time until adult: " + minutesUntilAdult + " minutes" + " " + secondsUntilAdult % 60 + " seconds"));
        villagerEntity.remove(Entity.RemovalReason.DISCARDED);
        super.appendTooltip(stack, world, tooltip, context);
    }
}
