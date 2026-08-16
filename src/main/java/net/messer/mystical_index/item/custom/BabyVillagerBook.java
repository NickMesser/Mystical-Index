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
    public void onCraft(ItemStack stack, World world, PlayerEntity player) {
        if(world.isClient())
            return;

        createAndAddBabyVillagerToBook(stack, (ServerWorld) world);
        super.onCraft(stack, world, player);
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

        // getLong returns 0 for a missing key, so without this guard a book carrying unrelated
        // NBT would "mature" on the next tick.
        if(!nbt.contains("timeUntilAdult"))
            return;

        // Growth is driven entirely by the stored timestamp. Loading the villager here just to
        // bump a breeding age that was never written back allocated an entity every tick.
        var timeUntilAdult = nbt.getLong("timeUntilAdult");
        if(world.getTime() > timeUntilAdult && entity instanceof PlayerEntity player)
        {
            ItemStack newVillagerStack = new ItemStack(ModItems.VILLAGER_BOOK);
            // Carry the captured villager over (biome type, name, trades, xp) instead of spawning
            // a fresh plains villager, and clear the breeding age so the matured book holds an
            // adult. This mirrors how addVillagerToBook stores the villager under "Entity".
            NbtCompound entityNbt = nbt.getCompound("Entity").copy();
            entityNbt.putInt("Age", 0);
            newVillagerStack.getOrCreateNbt().put("Entity", entityNbt);
            newVillagerStack.setCustomName(Text.translatable("item.mystical_index.villager_book"));
            // getSlotWithStack matches on item type only, so it could return a different baby
            // book's slot, or -1 when the book is not in the main inventory.
            stack.decrement(1);
            player.getInventory().offerOrDrop(newVillagerStack);
            return;
        }

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
            tooltip.add(Text.translatable("tooltip.mystical_index.baby_villager_book.craft"));
            return;
        }

        if (world == null)
            return;

        // Only the remaining time is needed, so read it straight from NBT instead of
        // deserialising a villager on every tooltip frame.
        var timeUntilAdult = Math.max(0, nbt.getLong("timeUntilAdult") - world.getTime());
        var secondsUntilAdult = timeUntilAdult / 20;
        var minutesUntilAdult = secondsUntilAdult / 60;
        tooltip.add(Text.translatable("tooltip.mystical_index.baby_villager_book.time_until_adult", minutesUntilAdult, secondsUntilAdult % 60));
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
        stack.setCustomName(Text.translatable("item.mystical_index.baby_villager_book"));
    }

    public void addBabyVillagerToBook(ItemStack stack, VillagerEntity villagerEntity) {
        var world = villagerEntity.getEntityWorld();
        var timeUntilAdult = world.getTime() + -villagerEntity.getBreedingAge();
        NbtCompound nbt = stack.getOrCreateNbt();
        NbtCompound entityNbt = new NbtCompound();
        villagerEntity.saveSelfNbt(entityNbt);
        nbt.put("Entity", entityNbt);
        nbt.putLong("timeUntilAdult", timeUntilAdult);
        stack.setCustomName(Text.translatable("item.mystical_index.baby_villager_book"));
    }
}
