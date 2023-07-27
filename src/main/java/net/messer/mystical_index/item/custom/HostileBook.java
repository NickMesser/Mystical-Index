package net.messer.mystical_index.item.custom;

import net.messer.config.ModConfig;
import net.messer.mystical_index.item.custom.base_books.BaseGeneratingBook;
import net.messer.mystical_index.item.custom.base_books.BaseStorageBook;
import net.messer.mystical_index.item.inventory.SingleItemStackingInventory;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
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

        var compound = stack.getNbt();

        if(compound == null)
            compound = stack.getOrCreateNbt();

        var numberOfKills = compound.getInt(NUMBER_OF_KILLS_KEY);
        if(numberOfKills > 0){
            user.sendMessage(Text.literal("Mob already stored in this book."), false);
            return super.useOnEntity(stack, user, entity, hand);
        }

        var lootTableId = entity.getType().getLootTableId();

        stack.setCustomName(Text.literal("Book of " + entity.getName().getString()));
        compound.putString(STORED_ENTITY_NAME_KEY, entity.getName().getString());
        compound.putString(STORED_ENTITY_LOOT_TABLE_KEY, lootTableId.toString());
        compound.putInt(NUMBER_OF_KILLS_KEY, 0);
        compound.putString(STORED_ENTITY_ID_KEY, entityName);

        updateUseTime(stack, maxCooldown);
        return super.useOnEntity(stack, user, entity, hand);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        if(!stack.hasNbt())
            return false;

        var compound = stack.getNbt();

        var numberOfKills = compound.getInt(NUMBER_OF_KILLS_KEY);

        return (numberOfKills > 0);
    }

    public void onKill(ItemStack stack, LivingEntity entity){
        if(!stack.hasNbt())
            return;

        var compound = stack.getNbt();
        var storedEntityName = compound.getString(STORED_ENTITY_NAME_KEY);
        if(!storedEntityName.equals(entity.getName().getString()))
            return;

        var numberOfKills = compound.getInt(NUMBER_OF_KILLS_KEY);

        if(numberOfKills >= ModConfig.HostileBookMaxKills)
            return;

        compound.putInt(NUMBER_OF_KILLS_KEY, numberOfKills + 1);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isClient)
            return;

        PlayerEntity player = (PlayerEntity) entity;

        if(!(entity instanceof PlayerEntity))
            return;

        if(player.isCreative())
            return;

        if(!stack.hasNbt())
            return;

        NbtCompound compound = stack.getNbt();

        var storedEntityLootTable = new Identifier(compound.getString(STORED_ENTITY_LOOT_TABLE_KEY));
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
            Entity storedEntity = EntityType.get(storedEntityId).get().create(world);

            var source = player.getDamageSources().playerAttack(player);

            LootContextParameterSet context = new LootContextParameterSet.Builder((ServerWorld) world)
                    .add(LootContextParameters.THIS_ENTITY, storedEntity)
                    .add(LootContextParameters.ORIGIN, player.getPos())
                    .add(LootContextParameters.DAMAGE_SOURCE, source)
                    .add(LootContextParameters.KILLER_ENTITY, player)
                    .add(LootContextParameters.DIRECT_KILLER_ENTITY, player)
                    .add(LootContextParameters.LAST_DAMAGE_PLAYER, player)
                    .build(LootContextTypes.ENTITY);


            LootTable lootTable = world.getServer().getLootManager().getLootTable(storedEntityLootTable);
            var loot = lootTable.generateLoot(context);
            for(ItemStack itemStack : loot) {
                if (!inventory.tryAddStack(itemStack, true))
                    itemStack.setCount(0);
            }
        }

    }

    public void updateUseTime(ItemStack stack, long time){
        NbtCompound compound = stack.getNbt();

        if(compound == null)
            compound = new NbtCompound();

        compound.putLong("lastUsedTime", time);
    }
    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if(!stack.hasNbt())
            return;

        var compound = stack.getNbt();
        if (compound == null)
            return;

        if(compound.contains("endless") && !compound.contains(STORED_ENTITY_NAME_KEY)){
            tooltip.add(Text.literal("§a+Endless"));
            return;
        }


        SingleItemStackingInventory inventory = new SingleItemStackingInventory(stack, INVENTORY_SIZE);
        List<String> itemNames = new ArrayList<>();
        for (ItemStack inventoryStack : inventory.storedItems) {
            var itemName = inventoryStack.getItem().getName().getString();
            if(itemNames.contains(itemName) || Objects.equals(itemName, Items.AIR.getName().getString()))
                continue;
            else{
                itemNames.add(itemName);
            }
        }

        var storedEntityName = compound.getString(STORED_ENTITY_NAME_KEY);
        var numberOfKills = compound.getInt(NUMBER_OF_KILLS_KEY);

        tooltip.add(Text.literal("Cooldown: " + ((maxCooldown - (20 * numberOfKills))/20) + " seconds"));

        tooltip.add(Text.literal("§a"+numberOfKills + "x " + "§f" + storedEntityName));

        for(String itemName : itemNames){
            var currentAmount = 0;
            for(ItemStack inventoryStack : inventory.storedItems) {
                if(inventoryStack.getItem().getName().getString().equals(itemName))
                    currentAmount += inventoryStack.getCount();
            }

            tooltip.add(Text.literal("§a"+currentAmount + "x " + "§f" + itemName));
        }

        if(compound.contains("endless") && compound.contains(STORED_ENTITY_NAME_KEY))
            tooltip.add(Text.literal("§aEndless"));

        if(Screen.hasShiftDown()){
            tooltip.add(Text.translatable("tooltip.mystical_index.hostile_book_shift0"));
            tooltip.add(Text.translatable("tooltip.mystical_index.hostile_book_shift1"));
        } else {
            tooltip.add(Text.translatable("tooltip.mystical_index.hostile_book"));
        }



        super.appendTooltip(stack, world, tooltip, context);
    }
}
