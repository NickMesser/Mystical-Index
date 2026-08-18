package net.messer.mystical_index.item.custom;

import net.messer.util.SelfUpdatingBook;
import net.minecraft.core.registries.Registries;
import net.messer.util.FakePlayerAccess;
import net.messer.config.ModConfig;
import net.messer.util.MysticalUtil;
import net.minecraft.core.component.DataComponents;
import net.messer.mystical_index.block.entity.ScriptoriumBlockEntity;
import net.messer.mystical_index.item.custom.base_books.BaseGeneratingBook;
import net.messer.mystical_index.item.inventory.SingleItemStackingInventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.client.gui.screens.Screen;
import net.messer.mystical_index.item.inventory.BookContentsTooltipData;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import net.minecraft.client.Minecraft;

import net.minecraft.world.entity.EquipmentSlot;

import net.minecraft.world.entity.EntitySpawnReason;

import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

import net.messer.util.GlintingBook;

public class HusbandryBook extends BaseGeneratingBook implements GlintingBook, SelfUpdatingBook {

    private static final String STORED_ENTITY_NAME_KEY = "storedEntityName";
    private static final String STORED_ENTITY_LOOT_TABLE_KEY = "storedEntityLootTable";
    private static final String STORED_ENTITY_ID_KEY = "storedEntityId";
    private static final String NUMBER_OF_KILLS_KEY = "numberOfKills";

    private static final int INVENTORY_SIZE = 6;


    public HusbandryBook(Item.Properties settings) {
        super(settings);
    }

    @Override
    public SingleItemStackingInventory getInventory(ItemStack stack) {
        return new SingleItemStackingInventory(stack, INVENTORY_SIZE);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity entity, InteractionHand hand) {
        if(user.level().isClientSide())
            return super.interactLivingEntity(stack, user, entity, hand);

        if(!(entity instanceof AgeableMob))
            return super.interactLivingEntity(stack, user, entity, hand);

        var entityName = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        if(ModConfig.HusbandryBookBlackList.contains(entityName) || ModConfig.HostileBookBlackList.contains(entityName))
        {
            user.sendSystemMessage(Component.translatable("message.mystical_index.mob_blacklisted"));
            return super.interactLivingEntity(stack, user, entity, hand);
        }

        var compound = MysticalUtil.getOrCreateCustomData(stack);

        var numberOfKills = compound.getIntOr(NUMBER_OF_KILLS_KEY, 0);
        if(numberOfKills > 0){
            user.sendSystemMessage(Component.translatable("message.mystical_index.mob_already_stored"));
            return super.interactLivingEntity(stack, user, entity, hand);
        }

        var lootTableId = entity.getType().getDefaultLootTable();

        stack.set(DataComponents.CUSTOM_NAME, Component.translatable("item.mystical_index.husbandry_book.named", entity.getName()));
        compound.putString(STORED_ENTITY_NAME_KEY, entity.getName().getString());
        // RegistryKey.toString() is a debug string; only the value is the id the book stores.
        compound.putString(STORED_ENTITY_LOOT_TABLE_KEY, MysticalUtil.lootTableIdString(lootTableId));
        compound.putInt(NUMBER_OF_KILLS_KEY, 0);
        compound.putString(STORED_ENTITY_ID_KEY, entityName);
        MysticalUtil.setCustomData(stack, compound);

        // lastUsedTime is the raw (monotonic) world age; cooldown is now - lastUsedTime.
        updateUseTime(stack, user.level().getGameTime());
        return super.interactLivingEntity(stack, user, entity, hand);
    }
    @Override
    public boolean shouldGlint(ItemStack stack) {
        if(!MysticalUtil.hasCustomData(stack))
            return false;

        var compound = MysticalUtil.getCustomData(stack);

        assert compound != null;
        var numberOfKills = compound.getIntOr(NUMBER_OF_KILLS_KEY, 0);

        return (numberOfKills > 0);
    }

    public void onKill(ItemStack stack, LivingEntity entity){
        if(!MysticalUtil.hasCustomData(stack))
            return;

        var compound = MysticalUtil.getCustomData(stack);
        assert compound != null;
        var storedEntityId = compound.getStringOr(STORED_ENTITY_ID_KEY, "");
        var killedEntityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();

        // Name tags rename the mob, so the type id is the reliable match. Books bound before the
        // id was stored still fall back to the display name.
        if(storedEntityId.isEmpty()){
            var storedEntityName = compound.getStringOr(STORED_ENTITY_NAME_KEY, "");
            if(!storedEntityName.equals(entity.getName().getString()))
                return;
        }
        else if(!storedEntityId.equals(killedEntityId)){
            return;
        }

        var numberOfKills = compound.getIntOr(NUMBER_OF_KILLS_KEY, 0);

        if(numberOfKills >= ModConfig.HusbandryBookMaxKills)
            return;

        var updated = numberOfKills + 1;
        MysticalUtil.editCustomData(stack, nbt -> nbt.putInt(NUMBER_OF_KILLS_KEY, updated));
    }

    @Override
    public void customBookTick(ItemStack stack, Level world, BlockEntity be) {
        if (world.isClientSide())
            return;

        if(!MysticalUtil.hasCustomData(stack))
            return;

        // The Scriptorium is where books WORK; the Library is storage only. This gate named the
        // Library back when Libraries ticked their contents - once that job moved, it matched
        // nothing a book is ever ticked from, so generating books grew nowhere in the world. Keep
        // it explicit rather than "any block entity": letting the Library tick again is exactly
        // what the storage-only rule forbids.
        if(!(be instanceof ScriptoriumBlockEntity))
            return;

        tryGenerateResources(stack, world);
    }

    @Override
    public void customBookTick(ItemStack stack, Level world, Entity entity) {
        if (world.isClientSide())
            return;

        if(!(entity instanceof Player player))
            return;

        if(player.isCreative())
            return;

        if(!MysticalUtil.hasCustomData(stack))
            return;

        tryGenerateResources(stack, world);
    }

    public void tryGenerateResources(ItemStack stack, Level world){
        CompoundTag compound = MysticalUtil.getCustomData(stack);

        assert compound != null;
        // Never Identifier.parse here. This runs from inventoryTick, so a stored string it
        // dislikes throws twenty times a second for as long as the book is held - which is exactly
        // how a bad bind turned into a world that crashes on rejoin. parseStoredId also repairs the
        // debug-rendering form an earlier build wrote, so an affected book silently heals.
        var storedLootTableRaw = compound.getStringOr(STORED_ENTITY_LOOT_TABLE_KEY, "");
        var storedEntityLootTable = MysticalUtil.parseStoredId(storedLootTableRaw);
        if (storedEntityLootTable == null) {
            // Nothing recoverable: drop the binding rather than retry a parse that cannot succeed.
            if (!storedLootTableRaw.isEmpty())
                MysticalUtil.editCustomData(stack, nbt -> nbt.remove(STORED_ENTITY_LOOT_TABLE_KEY));
            return;
        }

        // Recovered from the mangled form - write the clean id back so the repair happens once.
        if (!storedEntityLootTable.toString().equals(storedLootTableRaw)) {
            var repaired = storedEntityLootTable.toString();
            MysticalUtil.editCustomData(stack, nbt -> nbt.putString(STORED_ENTITY_LOOT_TABLE_KEY, repaired));
        }
        var numberOfKills = compound.getIntOr(NUMBER_OF_KILLS_KEY, 0);
        var storedEntityId = compound.getStringOr(STORED_ENTITY_ID_KEY, "");

        if(numberOfKills <= 0)
            return;

        var maxCooldown = ModConfig.HusbandryBookCooldown * 20;
        var currentTime = world.getGameTime();
        var lastUsedTime = compound.getLongOr("lastUsedTime", 0L);
        var difference = currentTime - lastUsedTime;
        if(difference < 0){
            updateUseTime(stack, currentTime);
            return;
        }

        if(difference > (maxCooldown - (numberOfKills * 20L))){
            updateUseTime(stack, currentTime);
            var inventory = new SingleItemStackingInventory(stack, INVENTORY_SIZE);
            var storedEntityType = EntityType.byString(storedEntityId).orElse(null);
            if(storedEntityType == null)
                return;

            Entity storedEntity = storedEntityType.create(world, EntitySpawnReason.COMMAND);
            if(storedEntity == null)
                return;

            var player = FakePlayerAccess.get((ServerLevel) world);
            var loot = MysticalUtil.generateEntityLoot(player, storedEntity, storedEntityLootTable);

            if (storedEntity instanceof Sheep) // Dumb hack because sheep dont have wool in a drop table. TODO: Fix this
                loot.add(new ItemStack(Items.WHITE_WOOL, 1 + world.getRandom().nextInt(2)));

            for(ItemStack itemStack : loot) {
                if (!inventory.tryAddStack(itemStack, Boolean.TRUE))
                    itemStack.setCount(0);
            }
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, EquipmentSlot slot) {
        customBookTick(stack, world, entity);
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        var storageInventory = new SingleItemStackingInventory(stack, INVENTORY_SIZE);
        if(storageInventory.isEmpty())
            return Optional.empty();


        return Optional.of(BookContentsTooltipData.fromInventory(storageInventory));
    }
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag type) {
        var world = MysticalUtil.tooltipWorld();
        var compound = MysticalUtil.getCustomData(stack);
        if(compound != null && world != null){

            var storedEntityName = compound.getStringOr(STORED_ENTITY_NAME_KEY, "");
            var numberOfKills = compound.getIntOr(NUMBER_OF_KILLS_KEY, 0);

            // Only this stats block is entity-specific; the shift/help lines below must always show.
            if(!storedEntityName.equals("")){
                var maxCooldown = ModConfig.HusbandryBookCooldown * 20;

                if(numberOfKills >= ModConfig.HusbandryBookMaxKills)
                    tooltip.accept(Component.translatable("tooltip.mystical_index.husbandry_book.max_kills"));
                else
                    tooltip.accept(Component.translatable("tooltip.mystical_index.husbandry_book.kills", numberOfKills));

                var timeLastUsed = compound.getLongOr("lastUsedTime", 0L);
                var difference = world.getGameTime() - timeLastUsed;
                var timeLeft = (difference - (maxCooldown - (numberOfKills * 20L)));

                if((timeLeft/20) * -1 < 0)
                    timeLeft = 0;

                tooltip.accept(Component.translatable("tooltip.mystical_index.husbandry_book.cooldown", (maxCooldown - (20 * numberOfKills))/20));
                tooltip.accept(Component.translatable("tooltip.mystical_index.husbandry_book.time_left", (timeLeft/20) * -1));
            }
        }

        if(Minecraft.getInstance().hasShiftDown()){
            tooltip.accept(Component.translatable("tooltip.mystical_index.husbandry_book_shift0"));
            tooltip.accept(Component.translatable("tooltip.mystical_index.husbandry_book_shift1"));
            tooltip.accept(Component.translatable("tooltip.mystical_index.husbandry_book_shift2"));
        } else {
            tooltip.accept(Component.translatable("tooltip.mystical_index.husbandry_book"));
        }

        super.appendHoverText(stack, context, display, tooltip, type);
    }
}
