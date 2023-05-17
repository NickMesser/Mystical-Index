package net.messer.mystical_index.item.custom;

import net.messer.mixin.VillagerEntityInvoker;
import net.messer.mystical_index.item.ModItems;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.VillagerProfession;
import net.minecraft.village.VillagerType;
import net.minecraft.world.World;
import net.minecraft.world.poi.PointOfInterestType;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class VillagerBook extends Item {
    public VillagerBook(Settings settings) {
        super(settings);
    }
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if(context.getWorld().isClient())
            return super.useOnBlock(context);

        PlayerEntity player = context.getPlayer();
        ItemStack stack = context.getStack();
        NbtCompound nbt = stack.getNbt();
        World world = context.getWorld();

        if(player.isSneaking() && stack.hasNbt()){
            var server = context.getWorld().getServer();
            var jobSiteWorld = server.getWorld(context.getWorld().getRegistryKey());
            var poiType = jobSiteWorld.getPointOfInterestStorage().getType(context.getBlockPos()).orElse(null);
            if(poiType == null){
                var position = context.getBlockPos();
                var targetPos = switch (context.getSide()) {
                    case UP -> position.up();
                    case DOWN -> position.down(2);
                    case NORTH -> position.north();
                    case SOUTH -> position.south();
                    case EAST -> position.east();
                    case WEST -> position.west();
                    default -> position;
                };

                ServerWorld serverWorld = world.getServer().getWorld(world.getRegistryKey());

                Entity entity = EntityType.loadEntityWithPassengers(nbt.getCompound("Entity"), world, (entityx) -> {
                    entityx.refreshPositionAndAngles(targetPos.getX()+.5, targetPos.getY(), targetPos.getZ()+.5, entityx.getYaw(), entityx.getPitch());
                    if (!(serverWorld.tryLoadEntity(entityx)))
                        return null;

                    return entityx;
                });

                if(entity != null)
                    nbt.remove("Entity");

                ItemStack emptyVillagerStack = new ItemStack(ModItems.EMPTY_VILLAGER_BOOK);
                stack.decrement(1);
                player.setStackInHand(context.getHand(), emptyVillagerStack);
                return super.useOnBlock(context);
            }

            var profession = Registries.VILLAGER_PROFESSION.stream().filter(profession1 -> profession1.heldWorkstation().test((RegistryEntry<PointOfInterestType>)poiType)).findFirst().orElse(null);

            if(profession == null)
                return super.useOnBlock(context);

            // Set villager profession if profession is found at block.

            VillagerEntity villagerEntity = (VillagerEntity) EntityType.loadEntityWithPassengers(nbt.getCompound("Entity"), world, (entityx) -> {
                entityx.refreshPositionAndAngles(context.getBlockPos().getX(), context.getBlockPos().getY(), context.getBlockPos().getZ(), entityx.getYaw(), entityx.getPitch());
                return entityx;
            });

            if(villagerEntity.getVillagerData().getProfession() == VillagerProfession.NONE || villagerEntity.getExperience() == 0)
            {
                player.sendMessage(Text.literal("Set villager profession to: " + profession.toString()), true);
                villagerEntity.setVillagerData(villagerEntity.getVillagerData().withProfession(VillagerProfession.NONE));
                villagerEntity.reinitializeBrain((ServerWorld) world);
                villagerEntity.setVillagerData(villagerEntity.getVillagerData().withProfession(profession));
                villagerEntity.reinitializeBrain((ServerWorld) world);
                addVillagerToBook(stack, villagerEntity);
                String professionName = profession.toString().substring(0,1).toUpperCase() + profession.toString().substring(1).toLowerCase();
                stack.setCustomName(Text.of("Book Of " + professionName));
            }
        }


        return super.useOnBlock(context);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {


        if(!stack.hasNbt())
        {
            super.appendTooltip(stack, world, tooltip, context);
            return;
        }

        NbtCompound nbt = stack.getNbt();
        if(nbt == null)
            return;

        VillagerEntity villagerEntity = (VillagerEntity) EntityType.loadEntityWithPassengers(nbt.getCompound("Entity"), world, (entityx) -> {
            return entityx;
        });

        if(villagerEntity == null)
            return;

        var trades = villagerEntity.getOffers();
        for (TradeOffer trade : trades) {
            ItemStack firstBuyItemStack = trade.getOriginalFirstBuyItem();
            ItemStack secondBuyItemStack = trade.getSecondBuyItem();
            ItemStack sellItemStack = trade.getSellItem();

            if(secondBuyItemStack.isEmpty())
                tooltip.add(Text.of(firstBuyItemStack.getCount() + " " + firstBuyItemStack.getName().getString() + " for " + sellItemStack.getCount() + " " + sellItemStack.getName().getString()));
            else
                tooltip.add(Text.of(firstBuyItemStack.getCount() + " " + firstBuyItemStack.getName().getString() + " and " + secondBuyItemStack.getCount() + " " + secondBuyItemStack.getName().getString() + " for " + sellItemStack.getCount() + " " + sellItemStack.getName().getString()));
        }

        super.appendTooltip(stack, world, tooltip, context);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if(user.isSneaking())
            return super.use(world, user, hand);

        ItemStack stack = user.getStackInHand(hand);
        NbtCompound nbt = stack.getNbt();
        if(nbt == null)
            return super.use(world, user, hand);

        VillagerEntity villagerEntity = (VillagerEntity) EntityType.loadEntityWithPassengers(nbt.getCompound("Entity"), world, (entityx) -> {
            entityx.refreshPositionAndAngles(user.getX(), user.getY(), user.getZ(), entityx.getYaw(), entityx.getPitch());
            return entityx;
        });

        if(villagerEntity == null)
            return super.use(world, user, hand);


        if(villagerEntity.getVillagerData().getProfession() == VillagerProfession.NONE)
        {
            world.playSound(null, user.getBlockPos(), net.minecraft.sound.SoundEvents.ENTITY_VILLAGER_NO, net.minecraft.sound.SoundCategory.NEUTRAL, 1f, 1f);
            return super.use(world, user, hand);
        }

        if(villagerEntity.shouldRestock())
            villagerEntity.restock();

        if(((VillagerEntityInvoker) villagerEntity).getCanLevelUp())
            ((VillagerEntityInvoker) villagerEntity).invokeLevelUp();

        world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_VILLAGER_TRADE, SoundCategory.AMBIENT, 1f, 1.5f);
        user.interact(villagerEntity, hand);

        return super.use(world, user, hand);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return stack.hasNbt();
    }

    @Override
    public ItemStack getRecipeRemainder(ItemStack stack) {
        return stack.copy();
    }

    public void createAndAddVillager(ItemStack stack, ServerWorld serverWorld){
        VillagerType villagerType = VillagerType.PLAINS;
        VillagerEntity villagerEntity = new VillagerEntity(EntityType.VILLAGER, serverWorld, villagerType);
        villagerEntity.initialize(serverWorld, serverWorld.getLocalDifficulty(villagerEntity.getBlockPos()), SpawnReason.SPAWN_EGG, null, null);
        villagerEntity.getVillagerData().withProfession(VillagerProfession.NONE);
        addVillagerToBook(stack, villagerEntity);
    }

    public void addVillagerToBook(ItemStack stack, VillagerEntity villagerEntity){
        NbtCompound stackNbt = stack.getNbt();
        if(stackNbt == null)
            stackNbt = stack.getOrCreateNbt();

        NbtCompound entityNbt = new NbtCompound();
        villagerEntity.saveSelfNbt(entityNbt);
        stackNbt.remove("Entity");
        stackNbt.put("Entity", entityNbt);

        if(villagerEntity.getVillagerData().getProfession() == VillagerProfession.NONE){
            stack.setCustomName(Text.of("Book Of Villager"));
        }
        else{
            var professionName = villagerEntity.getVillagerData().getProfession().toString().substring(0,1).toUpperCase() + villagerEntity.getVillagerData().getProfession().toString().substring(1).toLowerCase();
            stack.setCustomName(Text.of("Book Of " + professionName));
        }

    }
}
