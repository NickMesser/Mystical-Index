package net.messer.mystical_index.item.custom;

import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.nbt.NbtOps;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.messer.mixin.VillagerEntityInvoker;
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
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import net.minecraft.world.entity.EntitySpawnReason;

import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;



import net.messer.util.GlintingBook;

public class VillagerBook extends Item implements GlintingBook {
    public VillagerBook(Item.Properties settings) {
        super(settings);
    }
    @Override
    public InteractionResult useOn(UseOnContext context) {
        if(context.getLevel().isClientSide())
            return super.useOn(context);

        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        CompoundTag nbt = MysticalUtil.getCustomData(stack);
        Level world = context.getLevel();

        if(player != null && player.isShiftKeyDown() && nbt != null){
            var server = context.getLevel().getServer();
            var jobSiteWorld = server.getLevel(context.getLevel().dimension());
            var poiType = jobSiteWorld.getPoiManager().getType(context.getClickedPos()).orElse(null);
            if(poiType == null){
                var position = context.getClickedPos();
                var targetPos = switch (context.getClickedFace()) {
                    case UP -> position.above();
                    case DOWN -> position.below(2);
                    case NORTH -> position.north();
                    case SOUTH -> position.south();
                    case EAST -> position.east();
                    case WEST -> position.west();
                    default -> position;
                };

                ServerLevel serverWorld = world.getServer().getLevel(world.dimension());

                Entity entity = EntityType.loadEntityRecursive(nbt.getCompoundOrEmpty("Entity"), world, EntitySpawnReason.LOAD, (entityx) -> {
                    entityx.absSnapTo(targetPos.getX()+.5, targetPos.getY(), targetPos.getZ()+.5, entityx.getYRot(), entityx.getXRot());
                    if (!(serverWorld.addFreshEntity(entityx)))
                        return null;

                    return entityx;
                });

                // If the villager could not be placed, keep the book intact rather than trading it
                // for an empty one and losing the stored villager.
                if(entity == null) {
                    // Two different failures used to look identical here - a placement that was
                    // refused (blocked space), and stored data that cannot be read back at all.
                    // The second is worth saying out loud: books written by a build that saved the
                    // entity without its type id are unrecoverable, because nothing records what
                    // kind of entity it was, and silence made them indistinguishable from a
                    // no-op click.
                    if(!nbt.getCompoundOrEmpty("Entity").isEmpty())
                        player.sendOverlayMessage(
                                Component.translatable("message.mystical_index.villager_data_invalid"));

                    return super.useOn(context);
                }

                nbt.remove("Entity");
                MysticalUtil.setCustomData(stack, nbt);

                ItemStack emptyVillagerStack = new ItemStack(ModItems.EMPTY_VILLAGER_BOOK.get());
                player.setItemInHand(context.getHand(), ItemUtils.createFilledResult(stack, player, emptyVillagerStack));
                return super.useOn(context);
            }

            var profession = BuiltInRegistries.VILLAGER_PROFESSION.listElements().filter(profession1 -> profession1.value().heldJobSite().test((Holder<PoiType>)poiType)).findFirst().orElse(null);

            if(profession == null)
                return super.useOn(context);

            // Set villager profession if profession is found at block.

            Villager villagerEntity = (Villager) EntityType.loadEntityRecursive(nbt.getCompoundOrEmpty("Entity"), world, EntitySpawnReason.LOAD, (entityx) -> {
                entityx.absSnapTo(context.getClickedPos().getX(), context.getClickedPos().getY(), context.getClickedPos().getZ(), entityx.getYRot(), entityx.getXRot());
                return entityx;
            });

            if(villagerEntity == null)
                return super.useOn(context);

            if(villagerEntity.getVillagerData().profession().is(VillagerProfession.NONE) || villagerEntity.getVillagerXp() == 0)
            {
                player.sendOverlayMessage(Component.translatable("message.mystical_index.set_villager_profession", profession.value().name()));
                villagerEntity.setVillagerData(villagerEntity.getVillagerData().withProfession(world.registryAccess(), VillagerProfession.NONE));
                villagerEntity.refreshBrain((ServerLevel) world);
                villagerEntity.setVillagerData(villagerEntity.getVillagerData().withProfession(profession));
                villagerEntity.refreshBrain((ServerLevel) world);
                addVillagerToBook(stack, villagerEntity);
                stack.set(DataComponents.CUSTOM_NAME,
                        Component.translatable("item.mystical_index.villager_book.named", profession.value().name()));
            }
        }


        return super.useOn(context);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag type) {
        if(!MysticalUtil.hasCustomData(stack))
        {
            super.appendHoverText(stack, context, display, tooltip, type);
            return;
        }

        CompoundTag nbt = MysticalUtil.getCustomData(stack);
        if(nbt == null)
            return;

        var entityNbt = nbt.getCompoundOrEmpty("Entity");
        if(entityNbt.isEmpty())
            return;

        // Everything this tooltip shows is read straight out of the stored compound instead of
        // rebuilding the villager. That is not just an optimisation: getOffers() is server-only
        // now and throws IllegalStateException outright when the entity's level is not a
        // ServerLevel, which is always true here, so interrogating a tooltip-side villager
        // crashed the moment the book had a profession. Dropping the load also retires a full
        // entity deserialisation that was running on every frame the book was hovered.
        var ops = MysticalUtil.registryLookup().createSerializationContext(NbtOps.INSTANCE);

        var villagerData = entityNbt.read("VillagerData", VillagerData.CODEC, ops).orElse(null);
        if(villagerData == null)
            return;

        if(villagerData.profession().is(VillagerProfession.NONE)){
            tooltip.accept(Component.translatable("tooltip.mystical_index.villager_book.set_profession0"));
            tooltip.accept(Component.translatable("tooltip.mystical_index.villager_book.set_profession1"));
            return;
        }

        tooltip.accept(Component.translatable("tooltip.mystical_index.villager_book.trade"));
        tooltip.accept(Component.literal(""));

        // Vanilla only writes Offers once the villager actually has trades, so an absent key is
        // the ordinary state of one that has never been opened for trading - worth saying so
        // rather than ending the tooltip on a blank line.
        var trades = entityNbt.read("Offers", MerchantOffers.CODEC, ops).orElse(null);
        if(trades == null || trades.isEmpty()) {
            tooltip.accept(Component.translatable("tooltip.mystical_index.villager_book.no_trades"));
            super.appendHoverText(stack, context, display, tooltip, type);
            return;
        }

        tooltip.accept(Component.translatable("tooltip.mystical_index.villager_book.trades_header"));

        for (MerchantOffer trade : trades) {
            ItemStack firstBuyItemStack = trade.getBaseCostA();
            ItemStack secondBuyItemStack = trade.getCostB();
            ItemStack sellItemStack = trade.getResult();

            if(secondBuyItemStack.isEmpty())
                tooltip.accept(Component.translatable("tooltip.mystical_index.villager_book.trade_single",
                        firstBuyItemStack.getCount(), firstBuyItemStack.getHoverName(),
                        sellItemStack.getCount(), sellItemStack.getHoverName()));
            else
                tooltip.accept(Component.translatable("tooltip.mystical_index.villager_book.trade_double",
                        firstBuyItemStack.getCount(), firstBuyItemStack.getHoverName(),
                        secondBuyItemStack.getCount(), secondBuyItemStack.getHoverName(),
                        sellItemStack.getCount(), sellItemStack.getHoverName()));
        }

        super.appendHoverText(stack, context, display, tooltip, type);
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        // Server-side only: the client would deserialize a throwaway villager, run restock/levelUp
        // on it and play a second copy of the trade sound the server already broadcasts.
        if(world.isClientSide())
            return super.use(world, user, hand);

        if(user.isShiftKeyDown())
            return super.use(world, user, hand);

        ItemStack stack = user.getItemInHand(hand);
        CompoundTag nbt = MysticalUtil.getCustomData(stack);
        if(nbt == null)
            return super.use(world, user, hand);

        Villager villagerEntity = (Villager) EntityType.loadEntityRecursive(nbt.getCompoundOrEmpty("Entity"), world, EntitySpawnReason.LOAD, (entityx) -> {
            entityx.absSnapTo(user.getX(), user.getY(), user.getZ(), entityx.getYRot(), entityx.getXRot());
            return entityx;
        });

        if(villagerEntity == null)
            return super.use(world, user, hand);


        if(villagerEntity.getVillagerData().profession().is(VillagerProfession.NONE))
        {
            world.playSound(null, user.blockPosition(), net.minecraft.sounds.SoundEvents.VILLAGER_NO, net.minecraft.sounds.SoundSource.NEUTRAL, 1f, 1f);
            user.getCooldowns().addCooldown(user.getItemInHand(hand), 20);
            return super.use(world, user, hand);
        }

        if(villagerEntity.shouldRestock((ServerLevel) world))
            villagerEntity.restock();

        if(((VillagerEntityInvoker) villagerEntity).getCanLevelUp())
            ((VillagerEntityInvoker) villagerEntity).invokeLevelUp((ServerLevel) world);

        world.playSound(null, user.blockPosition(), SoundEvents.VILLAGER_TRADE, SoundSource.AMBIENT, 1f, 1.5f);
        user.interactOn(villagerEntity, hand, villagerEntity.position());
        user.getCooldowns().addCooldown(user.getItemInHand(hand), 20);
        // Return success so the hand swings; the afterUsing hook persists the throwaway villager
        // back into this book once a trade actually completes.
        return InteractionResult.SUCCESS;
    }
    @Override
    public boolean shouldGlint(ItemStack stack) {
        return MysticalUtil.hasCustomData(stack);
    }

    public void createAndAddVillager(ItemStack stack, ServerLevel serverWorld){
        ResourceKey<VillagerType> villagerType = VillagerType.PLAINS;
        Villager villagerEntity = new Villager(EntityType.VILLAGER, serverWorld, villagerType);
        villagerEntity.finalizeSpawn(serverWorld, serverWorld.getCurrentDifficultyAt(villagerEntity.blockPosition()), EntitySpawnReason.SPAWN_ITEM_USE, null);
        villagerEntity.getVillagerData().withProfession(serverWorld.registryAccess(), VillagerProfession.NONE);
        addVillagerToBook(stack, villagerEntity);
    }

    public void addVillagerToBook(ItemStack stack, Villager villagerEntity){
        CompoundTag stackNbt = MysticalUtil.getOrCreateCustomData(stack);

        CompoundTag entityNbt = MysticalUtil.saveEntityWithId(villagerEntity);
        if (entityNbt == null)
            return;

        stackNbt.remove("Entity");
        stackNbt.put("Entity", entityNbt);
        MysticalUtil.setCustomData(stack, stackNbt);

        if(villagerEntity.getVillagerData().profession().is(VillagerProfession.NONE)){
            stack.set(DataComponents.CUSTOM_NAME, Component.translatable("item.mystical_index.villager_book"));
        }
        else{
            // VillagerProfession carries its own display name - the same Component vanilla puts on a
            // professioned villager - so it is used verbatim. Building the text from the profession
            // object instead produced its debug form, which since professions became registry
            // holders reads as "ResourceKey[minecraft:villager_profession / minecraft:cartographer]".
            // It also localizes, which a capitalized id never did.
            stack.set(DataComponents.CUSTOM_NAME, Component.translatable("item.mystical_index.villager_book.named",
                    villagerEntity.getVillagerData().profession().value().name()));
        }

    }
}
