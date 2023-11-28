package net.messer.mystical_index.events;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.messer.config.ModConfig;
import net.messer.mystical_index.item.ModItems;
import net.messer.mystical_index.item.custom.HostileBook;
import net.messer.mystical_index.item.custom.HusbandryBook;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.World;

public class PlayerKillEvent {
    public static void init() {
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, player, entity) -> {
            if (entity.isDead() && entity != player) {
                var entityName = Registries.ENTITY_TYPE.getId(entity.getType()).toString();

                if(ModConfig.HusbandryBookBlackList.contains(entityName) || ModConfig.HostileBookBlackList.contains(entityName))
                    return;

                if(player instanceof PlayerEntity) {
                    dropEntityPaper(world, (PlayerEntity) player, entity);
                    var offHandStack = ((PlayerEntity) player).getEquippedStack(EquipmentSlot.OFFHAND);
                    if(offHandStack.getItem() instanceof HusbandryBook husbandryBook) {
                        husbandryBook.onKill(offHandStack, entity);
                    }
                    if(offHandStack.getItem() instanceof HostileBook hostileBook) {
                        hostileBook.onKill(offHandStack, entity);
                    }
                }
            }
        });
    }

    public static void dropEntityPaper(World world, PlayerEntity player, Entity entityId){

        //Check if entity is blacklisted
        if(ModConfig.EntityPaperBlackList.contains(Registries.ENTITY_TYPE.getId(entityId.getType()).toString()))
            return;

        // Check if entity has spawn egg
        if(SpawnEggItem.forEntity(entityId.getType()) == null)
            return;

        // Randomize drop chance that also scales with luck.
        var dropChance = 0.05f + (player.getLuck() * 0.1f);
        if(world.random.nextFloat() > dropChance)
            return;

        // Create entity paper and drop it
        var entityPaper = new ItemStack(ModItems.ENTITY_PAPER);
        var nbt = entityPaper.getOrCreateNbt();
        nbt.putString("entity", Registries.ENTITY_TYPE.getId(entityId.getType()).toString());
        entityPaper.onCraft(world, FakePlayer.get((ServerWorld) world), 1);
        entityId.dropStack(entityPaper);
    }
}
