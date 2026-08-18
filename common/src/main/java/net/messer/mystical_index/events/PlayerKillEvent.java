package net.messer.mystical_index.events;

import net.minecraft.core.registries.Registries;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import net.messer.config.ModConfig;
import net.messer.mystical_index.item.ModItems;
import net.messer.mystical_index.item.custom.HostileBook;
import net.messer.mystical_index.item.custom.HusbandryBook;
import net.messer.util.FakePlayerAccess;
import net.messer.util.MysticalUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class PlayerKillEvent {
    public static void init() {
        // Fabric's ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY handed over the killer; the
        // cross-loader event reports the victim instead, so the killer is taken back off the damage
        // source - which is the very same Entity vanilla passes to Entity#onKilledOther, the call
        // the Fabric event used to wrap. The other conditions that call site implied are restored
        // explicitly: it only ran on a server world, and only when there was an attacker at all.
        EntityEvent.LIVING_DEATH.register((entity, source) -> {
            if (!(entity.level() instanceof ServerLevel world))
                return EventResult.pass();

            Entity player = source.getEntity();
            if (player == null)
                return EventResult.pass();

            if (entity.isDeadOrDying() && entity != player) {
                var entityName = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();

                if(player instanceof Player) {
                    // Entity-paper drop has its own EntityPaperBlackList gate inside dropEntityPaper,
                    // so it must run before the husbandry/hostile blacklist return below.
                    dropEntityPaper(world, (Player) player, entity);

                    if(ModConfig.HusbandryBookBlackList.contains(entityName) || ModConfig.HostileBookBlackList.contains(entityName))
                        return EventResult.pass();

                    // forEachBookIn, deliberately NOT forEachEffectiveBook: the rule has always
                    // been that only the OFFHAND book counts kills, and walking the whole inventory
                    // would quietly turn every carried book into a counter. This sees through a
                    // sling held in the offhand and changes nothing else about the rule.
                    var offHandStack = ((Player) player).getItemBySlot(EquipmentSlot.OFFHAND);
                    MysticalUtil.forEachBookIn(offHandStack, book -> {
                        if(book.getItem() instanceof HusbandryBook husbandryBook)
                            husbandryBook.onKill(book, entity);

                        if(book.getItem() instanceof HostileBook hostileBook)
                            hostileBook.onKill(book, entity);
                    });
                }
            }

            // Never cancel the death: this only observes it.
            return EventResult.pass();
        });
    }

    public static void dropEntityPaper(Level world, Player player, Entity entityId){

        //Check if entity is blacklisted
        if(ModConfig.EntityPaperBlackList.contains(BuiltInRegistries.ENTITY_TYPE.getKey(entityId.getType()).toString()))
            return;

        // Check if entity has spawn egg. byId returns an Optional in this version, and an Optional
        // is never null - so the old "== null" test was always false and this guard had quietly
        // stopped running, dropping entity paper for mobs with no egg at all.
        if(SpawnEggItem.byId(entityId.getType()).isEmpty())
            return;

        // Randomize drop chance that also scales with luck.
        var dropChance = 0.05f + (player.getLuck() * 0.1f);
        if(world.getRandom().nextFloat() > dropChance)
            return;

        // Create entity paper and drop it
        var entityPaper = new ItemStack(ModItems.ENTITY_PAPER.get());
        MysticalUtil.editCustomData(entityPaper,
                nbt -> nbt.putString("entity", BuiltInRegistries.ENTITY_TYPE.getKey(entityId.getType()).toString()));
        entityPaper.onCraftedBy(FakePlayerAccess.get((ServerLevel) world), 1);
        entityId.spawnAtLocation((ServerLevel) world, entityPaper);
    }
}
