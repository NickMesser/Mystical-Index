package net.messer.mystical_index.item.custom;

import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.item.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.village.VillagerType;
import net.minecraft.world.World;

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
        child.setBreedingAge(-100);
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
        VillagerEntity villagerEntity = (VillagerEntity) EntityType.loadEntityWithPassengers(nbt.getCompound("Entity"), world, (entityx) -> {
            entityx.refreshPositionAndAngles(entity.getX(), entity.getY(), entity.getZ(), entityx.getYaw(), entityx.getPitch());
            return entityx;
        });

        if(villagerEntity == null)
            return;

        villagerEntity.setBreedingAge(villagerEntity.getBreedingAge() + 1);

        if(!villagerEntity.isBaby())
        {
            if(entity instanceof PlayerEntity player)
            {
                ItemStack newVillagerStack = new ItemStack(ModItems.VILLAGER_BOOK);
                var villagerBook = (VillagerBook) newVillagerStack.getItem();
                villagerBook.createAndAddVillager(newVillagerStack,(ServerWorld) world);
                player.giveItemStack(newVillagerStack);
                stack.decrement(1);
                villagerEntity.remove(Entity.RemovalReason.DISCARDED);
                return;
            }
        }

        NbtCompound entityNbt = new NbtCompound();
        villagerEntity.saveSelfNbt(entityNbt);
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
}
