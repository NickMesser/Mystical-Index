package net.messer.mystical_index.item.custom;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.messer.config.ModConfig;
import net.messer.mystical_index.block.entity.LibraryBlockEntity;
import net.messer.mystical_index.item.custom.base_books.BaseGeneratingBook;
import net.messer.mystical_index.item.inventory.SingleItemStackingInventory;
import net.messer.util.MysticalUtil;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.BundleTooltipData;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.item.TooltipData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
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

public class HusbandryBook extends BaseGeneratingBook {

    private static final String STORED_ENTITY_NAME_KEY = "storedEntityName";
    private static final String STORED_ENTITY_LOOT_TABLE_KEY = "storedEntityLootTable";
    private static final String STORED_ENTITY_ID_KEY = "storedEntityId";
    private static final String NUMBER_OF_KILLS_KEY = "numberOfKills";

    private static final int INVENTORY_SIZE = 6;

    private final int maxCooldown = ModConfig.HusbandryBookCooldown * 20;


    public HusbandryBook(Settings settings) {
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

        if(!(entity instanceof PassiveEntity))
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

        assert compound != null;
        var numberOfKills = compound.getInt(NUMBER_OF_KILLS_KEY);

        return (numberOfKills > 0);
    }

    public void onKill(ItemStack stack, LivingEntity entity){
        if(!stack.hasNbt())
            return;

        var compound = stack.getNbt();
        assert compound != null;
        var storedEntityName = compound.getString(STORED_ENTITY_NAME_KEY);
        if(!storedEntityName.equals(entity.getName().getString()))
            return;

        var numberOfKills = compound.getInt(NUMBER_OF_KILLS_KEY);

        if(numberOfKills >= ModConfig.HusbandryBookMaxKills)
            return;

        compound.putInt(NUMBER_OF_KILLS_KEY, numberOfKills + 1);
    }

    @Override
    public void customBookTick(ItemStack stack, World world, BlockEntity be) {
        if (world.isClient)
            return;

        if(!stack.hasNbt())
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

        if(!stack.hasNbt())
            return;

        tryGenerateResources(stack, world);
    }

    public void tryGenerateResources(ItemStack stack, World world){
        NbtCompound compound = stack.getNbt();

        assert compound != null;
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

            var player = FakePlayer.get((ServerWorld) world);
            var loot = MysticalUtil.generateEntityLoot(player, storedEntity, storedEntityLootTable);

            if (storedEntity instanceof SheepEntity) // Dumb hack because sheep dont have wool in a drop table. TODO: Fix this
                loot.add(new ItemStack(Items.WHITE_WOOL, 1 + world.random.nextInt(2)));

            for(ItemStack itemStack : loot) {
                if (!inventory.tryAddStack(itemStack, true))
                    itemStack.setCount(0);
            }
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        customBookTick(stack, world, entity);
    }

    @Override
    public Optional<TooltipData> getTooltipData(ItemStack stack) {
        var storageInventory = new SingleItemStackingInventory(stack, INVENTORY_SIZE);
        if(storageInventory.isEmpty())
            return Optional.empty();


        return Optional.of(new BundleTooltipData(storageInventory.storedItems, INVENTORY_SIZE * 64));
    }
    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if(stack.getNbt() != null){
            var compound = stack.getNbt();

            var storedEntityName = compound.getString(STORED_ENTITY_NAME_KEY);
            var numberOfKills = compound.getInt(NUMBER_OF_KILLS_KEY);

            if(storedEntityName.equals(""))
                return;

            if(numberOfKills >= ModConfig.HusbandryBookMaxKills)
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
            tooltip.add(Text.translatable("tooltip.mystical_index.husbandry_book_shift0"));
            tooltip.add(Text.translatable("tooltip.mystical_index.husbandry_book_shift1"));
        } else {
            tooltip.add(Text.translatable("tooltip.mystical_index.husbandry_book"));
        }

        super.appendTooltip(stack, world, tooltip, context);
    }
}
