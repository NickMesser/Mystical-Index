package net.messer.mystical_index.item.custom;

import net.messer.config.ModConfig;
import net.messer.mystical_index.item.inventory.SingleItemStackingInventory;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextType;
import net.minecraft.nbt.NbtCompound;
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
import java.util.Stack;

public class HusbandryBook extends Item {

    private static final String STORED_ENTITY_NAME_KEY = "storedEntityName";
    private static final String STORED_ENTITY_LOOT_TABLE_KEY = "storedEntityLootTable";
    private static final String NUMBER_OF_KILLS_KEY = "numberOfKills";

    private int maxCooldown = 2400;


    public HusbandryBook(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if(user.world.isClient)
            return super.useOnEntity(stack, user, entity, hand);

        if(!(entity instanceof PassiveEntity))
            return super.useOnEntity(stack, user, entity, hand);

        var compound = stack.getOrCreateNbt();

        var numberOfKills = compound.getInt(NUMBER_OF_KILLS_KEY);
        if(numberOfKills > 0){
            user.sendMessage(Text.literal("Mob already stored in this book."), false);
            return super.useOnEntity(stack, user, entity, hand);
        }

        var lootTableId = entity.getType().getLootTableId();

        compound.putString(STORED_ENTITY_NAME_KEY, entity.getName().getString());
        compound.putString(STORED_ENTITY_LOOT_TABLE_KEY, lootTableId.toString());
        compound.putInt(NUMBER_OF_KILLS_KEY, 0);

        stack.setCustomName(Text.literal("Book of " + entity.getName().getString()));
        updateUseTime(stack, maxCooldown);

        return super.useOnEntity(stack, user, entity, hand);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        var compound = stack.getOrCreateNbt();
        var numberOfKills = compound.getInt(NUMBER_OF_KILLS_KEY);

        return (numberOfKills > 0);
    }

    public void onKill(ItemStack stack, LivingEntity entity){
        if(!stack.hasNbt())
            return;

        var compound = stack.getOrCreateNbt();
        var storedEntityName = compound.getString(STORED_ENTITY_NAME_KEY);
        if(!storedEntityName.equals(entity.getName().getString()))
            return;

        var numberOfKills = compound.getInt(NUMBER_OF_KILLS_KEY);

        if(numberOfKills >= ModConfig.HusbandryBookMaxKills)
            return;

        compound.putInt(NUMBER_OF_KILLS_KEY, numberOfKills + 1);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isClient)
            return;

        PlayerEntity player = (PlayerEntity) entity;

        if(player.isCreative())
            return;

        NbtCompound compound = stack.getOrCreateNbt();
        var storedEntityName = compound.getString(STORED_ENTITY_NAME_KEY);
        var storedEntityLootTable = new Identifier(compound.getString(STORED_ENTITY_LOOT_TABLE_KEY));
        var numberOfKills = compound.getInt(NUMBER_OF_KILLS_KEY);

        if(numberOfKills <= 0)
            return;

        var currentTime = world.getTime() % 24000;
        var lastUsedTime = compound.getLong("lastUsedTime");
        var difference = currentTime - lastUsedTime;

        if(difference > (maxCooldown - (numberOfKills * 20L))){
            updateUseTime(stack, currentTime);
            var inventory = new SingleItemStackingInventory(stack, 2);
            LootContextType lootContextType = new LootContextType.Builder().build();
            LootTable lootTable = world.getServer().getLootManager().getTable(storedEntityLootTable);
            LootContext.Builder builder = new LootContext.Builder((ServerWorld) world);
            List<ItemStack> loot = lootTable.generateLoot(builder.build(lootContextType));
            for(ItemStack itemStack : loot){
                player.giveItemStack(itemStack);
//                if(!inventory.tryAddStack(itemStack, true))
//                    itemStack.setCount(0);
            }
        }

    }

    public void updateUseTime(ItemStack stack, long time){
        NbtCompound compound = stack.getOrCreateNbt();
        compound.putLong("lastUsedTime", time);
    }
    public void write_nbt(ItemStack stack, String entityName, Identifier lootTableId){
        NbtCompound compound = stack.getOrCreateNbt();
        compound.putString("storedEntityName", entityName);
        compound.putString("storedEntityLootTable", lootTableId.toString());
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        SingleItemStackingInventory inventory = new SingleItemStackingInventory(stack, ModConfig.StorageBookMaxStacks);
        List<String> itemNames = new ArrayList<>();
        for (ItemStack inventoryStack : inventory.storedItems) {
            var itemName = inventoryStack.getItem().getName().getString();
            if(itemNames.contains(itemName) || Objects.equals(itemName, Items.AIR.getName().getString()))
                continue;
            else{
                itemNames.add(itemName);
            }
        }

        var compound = stack.getOrCreateNbt();
        var storedEntityName = compound.getString(STORED_ENTITY_NAME_KEY);
        var numberOfKills = compound.getInt(NUMBER_OF_KILLS_KEY);
        tooltip.add(Text.literal("§a"+numberOfKills + "x " + "§f" + storedEntityName));

        for(String itemName : itemNames){
            var currentAmount = 0;
            tooltip.add(Text.literal("§a"+currentAmount + "x " + "§f" + itemName));
            for(ItemStack inventoryStack : inventory.storedItems) {
                if(inventoryStack.getItem().getName().getString().equals(itemName))
                    currentAmount += inventoryStack.getCount();
            }
        }

        super.appendTooltip(stack, world, tooltip, context);
    }
}
