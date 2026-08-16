package net.messer.mystical_index.events;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import net.messer.config.ModConfig;
import net.messer.mystical_index.item.ModItems;
import net.messer.mystical_index.item.custom.HostileBook;
import net.messer.mystical_index.item.custom.HusbandryBook;
import net.messer.util.FakePlayerAccess;
import net.messer.util.MysticalUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

public class PlayerKillEvent {
    public static void init() {
        // Fabric's ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY handed over the killer; the
        // cross-loader event reports the victim instead, so the killer is taken back off the damage
        // source - which is the very same Entity vanilla passes to Entity#onKilledOther, the call
        // the Fabric event used to wrap. The other conditions that call site implied are restored
        // explicitly: it only ran on a server world, and only when there was an attacker at all.
        EntityEvent.LIVING_DEATH.register((entity, source) -> {
            if (!(entity.getWorld() instanceof ServerWorld world))
                return EventResult.pass();

            Entity player = source.getAttacker();
            if (player == null)
                return EventResult.pass();

            if (entity.isDead() && entity != player) {
                var entityName = Registries.ENTITY_TYPE.getId(entity.getType()).toString();

                if(player instanceof PlayerEntity) {
                    // Entity-paper drop has its own EntityPaperBlackList gate inside dropEntityPaper,
                    // so it must run before the husbandry/hostile blacklist return below.
                    dropEntityPaper(world, (PlayerEntity) player, entity);

                    if(ModConfig.HusbandryBookBlackList.contains(entityName) || ModConfig.HostileBookBlackList.contains(entityName))
                        return EventResult.pass();

                    var offHandStack = ((PlayerEntity) player).getEquippedStack(EquipmentSlot.OFFHAND);
                    if(offHandStack.getItem() instanceof HusbandryBook husbandryBook) {
                        husbandryBook.onKill(offHandStack, entity);
                    }
                    if(offHandStack.getItem() instanceof HostileBook hostileBook) {
                        hostileBook.onKill(offHandStack, entity);
                    }
                }
            }

            // Never cancel the death: this only observes it.
            return EventResult.pass();
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
        var entityPaper = new ItemStack(ModItems.ENTITY_PAPER.get());
        MysticalUtil.editCustomData(entityPaper,
                nbt -> nbt.putString("entity", Registries.ENTITY_TYPE.getId(entityId.getType()).toString()));
        entityPaper.onCraftByPlayer(world, FakePlayerAccess.get((ServerWorld) world), 1);
        entityId.dropStack(entityPaper);
    }
}
