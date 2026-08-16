package net.messer.mystical_index.item.custom;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.messer.config.ModConfig;
import net.messer.util.MysticalUtil;
import net.minecraft.component.DataComponentTypes;
import net.messer.mystical_index.block.custom.LibraryInventoryBlock;
import net.messer.mystical_index.block.entity.LibraryBlockEntity;
import net.messer.mystical_index.item.custom.base_books.BaseGeneratingBook;
import net.messer.mystical_index.item.custom.base_books.BaseStorageBook;
import net.messer.mystical_index.item.inventory.SingleItemStackingInventory;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.gui.screen.Screen;
import net.messer.mystical_index.item.inventory.BookContentsTooltipData;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class HostileBook extends BaseGeneratingBook {

    private static final String STORED_ENTITY_NAME_KEY = "storedEntityName";
    private static final String STORED_ENTITY_LOOT_TABLE_KEY = "storedEntityLootTable";
    private static final String NUMBER_OF_KILLS_KEY = "numberOfKills";

    private static final String STORED_ENTITY_ID_KEY = "storedEntityId";

    private static final int INVENTORY_SIZE = 6;

    private int maxCooldown = ModConfig.HostileBookCooldown * 20;


    public HostileBook(Settings settings) {
        super(settings);
    }

    @Override
    public SingleItemStackingInventory getInventory(ItemStack stack) {
        return new SingleItemStackingInventory(stack, INVENTORY_SIZE);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if(user.getWorld().isClient)
            return super.useOnEntity(stack, user, entity, hand);

        if((entity instanceof PassiveEntity))
            return super.useOnEntity(stack, user, entity, hand);

        var entityName = Registries.ENTITY_TYPE.getId(entity.getType()).toString();
        if(ModConfig.HusbandryBookBlackList.contains(entityName) || ModConfig.HostileBookBlackList.contains(entityName))
        {
            user.sendMessage(Text.literal("Mob is blacklisted from book."));
            return super.useOnEntity(stack, user, entity, hand);
        }

        var compound = MysticalUtil.getOrCreateCustomData(stack);

        var numberOfKills = compound.getInt(NUMBER_OF_KILLS_KEY);
        if(numberOfKills > 0){
            user.sendMessage(Text.literal("Mob already stored in this book."), false);
            return super.useOnEntity(stack, user, entity, hand);
        }

        var lootTableId = entity.getType().getLootTableId();

        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Book of " + entity.getName().getString()));
        compound.putString(STORED_ENTITY_NAME_KEY, entity.getName().getString());
        // RegistryKey.toString() is a debug string; only the value is the id the book stores.
        compound.putString(STORED_ENTITY_LOOT_TABLE_KEY, lootTableId.getValue().toString());
        compound.putInt(NUMBER_OF_KILLS_KEY, 0);
        compound.putString(STORED_ENTITY_ID_KEY, entityName);
        MysticalUtil.setCustomData(stack, compound);

        // lastUsedTime is compared against world.getTime() % 24000, so it must be a timestamp.
        updateUseTime(stack, user.getWorld().getTime() % 24000);
        return super.useOnEntity(stack, user, entity, hand);
    }

    public void addEntityToBook(ItemStack stack, LivingEntity entity){
        var world = entity.getWorld();
        var entityName = Registries.ENTITY_TYPE.getId(entity.getType()).toString();
        if(ModConfig.HusbandryBookBlackList.contains(entityName) || ModConfig.HostileBookBlackList.contains(entityName))
        {
            return;
        }

        var compound = MysticalUtil.getOrCreateCustomData(stack);

        var numberOfKills = compound.getInt(NUMBER_OF_KILLS_KEY);
        if(numberOfKills > 0){
            return;
        }

        var lootTableId = entity.getType().getLootTableId();

        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Book of " + entity.getName().getString()));
        compound.putString(STORED_ENTITY_NAME_KEY, entity.getName().getString());
        // RegistryKey.toString() is a debug string; only the value is the id the book stores.
        compound.putString(STORED_ENTITY_LOOT_TABLE_KEY, lootTableId.getValue().toString());
        compound.putInt(NUMBER_OF_KILLS_KEY, 0);
        compound.putString(STORED_ENTITY_ID_KEY, entityName);
        MysticalUtil.setCustomData(stack, compound);

        updateUseTime(stack, world.getTime() % 24000);
    }

    public void increaseKills(ItemStack stack, int amount){
        var compound = MysticalUtil.getCustomData(stack);
        if(compound == null)
            return;

        var numberOfKills = compound.getInt(NUMBER_OF_KILLS_KEY);
        if(numberOfKills >= ModConfig.HostileBookMaxKills)
            return;

        MysticalUtil.editCustomData(stack, nbt -> nbt.putInt(NUMBER_OF_KILLS_KEY, numberOfKills + amount));
    }

    public void addEntityToBook(ItemStack stack, String entityId, World world){
        if(ModConfig.HusbandryBookBlackList.contains(entityId) || ModConfig.HostileBookBlackList.contains(entityId))
        {
            return;
        }

        var compound = MysticalUtil.getOrCreateCustomData(stack);

        var numberOfKills = compound.getInt(NUMBER_OF_KILLS_KEY);
        if(numberOfKills > 0){
            return;
        }

        var entity = EntityType.get(entityId).orElse(null);
        if(entity == null)
            return;

        var lootTableId = entity.getLootTableId();

        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Book of " + entity.getName().getString()));
        compound.putString(STORED_ENTITY_NAME_KEY, entity.getName().getString());
        compound.putString(STORED_ENTITY_LOOT_TABLE_KEY, lootTableId.getValue().toString());
        compound.putInt(NUMBER_OF_KILLS_KEY, 0);
        compound.putString(STORED_ENTITY_ID_KEY, entityId);
        MysticalUtil.setCustomData(stack, compound);

        updateUseTime(stack, world.getTime() % 24000);

    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        if(!MysticalUtil.hasCustomData(stack))
            return false;

        var compound = MysticalUtil.getCustomData(stack);

        var numberOfKills = compound.getInt(NUMBER_OF_KILLS_KEY);

        return (numberOfKills > 0);
    }

    public void onKill(ItemStack stack, LivingEntity entity){
        if(!MysticalUtil.hasCustomData(stack))
            return;

        var compound = MysticalUtil.getCustomData(stack);
        var storedEntityId = compound.getString(STORED_ENTITY_ID_KEY);
        var killedEntityId = Registries.ENTITY_TYPE.getId(entity.getType()).toString();

        // Name tags rename the mob, so the type id is the reliable match. Books bound before the
        // id was stored still fall back to the display name.
        if(storedEntityId.isEmpty()){
            var storedEntityName = compound.getString(STORED_ENTITY_NAME_KEY);
            if(!storedEntityName.equals(entity.getName().getString()))
                return;
        }
        else if(!storedEntityId.equals(killedEntityId)){
            return;
        }

        var numberOfKills = compound.getInt(NUMBER_OF_KILLS_KEY);

        if(numberOfKills >= ModConfig.HostileBookMaxKills)
            return;

        var updated = numberOfKills + 1;
        MysticalUtil.editCustomData(stack, nbt -> nbt.putInt(NUMBER_OF_KILLS_KEY, updated));
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        customBookTick(stack, world, entity);
    }

    public void tryGenerateResources(ItemStack stack, World world){
        NbtCompound compound = MysticalUtil.getCustomData(stack);

        var storedEntityLootTable = Identifier.of(compound.getString(STORED_ENTITY_LOOT_TABLE_KEY));
        var numberOfKills = compound.getInt(NUMBER_OF_KILLS_KEY);
        var storedEntityId = compound.getString(STORED_ENTITY_ID_KEY);

        if(numberOfKills <= 0)
            return;

        var currentTime = world.getTime() % 24000;
        var lastUsedTime = compound.getLong("lastUsedTime");
        var difference = currentTime - lastUsedTime;
        if(difference < 0)
            updateUseTime(stack, currentTime);

        if(difference > (maxCooldown - (numberOfKills * 20L))){
            updateUseTime(stack, currentTime);

            var inventory = new SingleItemStackingInventory(stack, INVENTORY_SIZE);
            var storedEntityType = EntityType.get(storedEntityId).orElse(null);
            if(storedEntityType == null)
                return;

            Entity storedEntity = storedEntityType.create(world);
            FakePlayer player = FakePlayer.get((ServerWorld) world);

            List<ItemStack> loot = MysticalUtil.generateEntityLoot(player, storedEntity, storedEntityLootTable);
            for(ItemStack itemStack : loot) {
                if (!inventory.tryAddStack(itemStack, Boolean.TRUE))
                    itemStack.setCount(0);
            }
        }
    }

    @Override
    public void customBookTick(ItemStack stack, World world, BlockEntity be) {
        if (world.isClient)
            return;

        if(!MysticalUtil.hasCustomData(stack))
            return;

        if(!(be instanceof LibraryBlockEntity))
            return;

        tryGenerateResources(stack, world);
    }

    @Override
    public void customBookTick(ItemStack stack, World world, Entity entity) {
        if (world.isClient)
            return;

        if(!(entity instanceof PlayerEntity player))
            return;

        if(player.isCreative())
            return;

        if(!MysticalUtil.hasCustomData(stack))
            return;

        tryGenerateResources(stack, world);
    }


    @Override
    public Optional<TooltipData> getTooltipData(ItemStack stack) {
        var storageInventory = new SingleItemStackingInventory(stack, INVENTORY_SIZE);
        if(storageInventory.isEmpty())
            return Optional.empty();


        return Optional.of(BookContentsTooltipData.fromInventory(storageInventory));
    }
    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        var world = MysticalUtil.tooltipWorld();
        var compound = MysticalUtil.getCustomData(stack);
        if(compound != null && world != null){

            var storedEntityName = compound.getString(STORED_ENTITY_NAME_KEY);
            var numberOfKills = compound.getInt(NUMBER_OF_KILLS_KEY);

            if(storedEntityName.equals(""))
                return;

            if(numberOfKills >= ModConfig.HostileBookMaxKills)
                tooltip.add(Text.literal("§cMax kills reached"));
            else
                tooltip.add(Text.literal("§aKills: " + numberOfKills));

            var timeLastUsed = compound.getLong("lastUsedTime");
            var difference = world.getTime() % 24000 - timeLastUsed;
            var timeLeft = (difference - (maxCooldown - (numberOfKills * 20L)));

            if((timeLeft/20) * -1 < 0)
                timeLeft = 0;

            tooltip.add(Text.literal("Cooldown: " + ((maxCooldown - (20 * numberOfKills))/20) + " seconds"));
            tooltip.add(Text.literal("Time left: " + ((timeLeft/20) * -1) + " seconds"));
        }

        if(Screen.hasShiftDown()){
            tooltip.add(Text.translatable("tooltip.mystical_index.hostile_book_shift0"));
            tooltip.add(Text.translatable("tooltip.mystical_index.hostile_book_shift1"));
        } else {
            tooltip.add(Text.translatable("tooltip.mystical_index.hostile_book"));
        }

        super.appendTooltip(stack, context, tooltip, type);
    }
}
