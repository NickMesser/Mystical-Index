package net.messer.mystical_index.events;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.messer.config.ModConfig;
import net.messer.mystical_index.item.custom.HostileBook;
import net.messer.mystical_index.item.custom.HusbandryBook;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

public class PlayerKillEvent {
    public static void init() {
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, player, entity) -> {
            if (entity.isDead() && entity != player) {
                var entityName = Registries.ENTITY_TYPE.getId(entity.getType()).toString();
                if(ModConfig.HusbandryBookBlackList.contains(entityName) || ModConfig.HostileBookBlackList.contains(entityName))
                {
                    player.sendMessage(Text.literal("Mob is blacklisted from book."));
                    return;
                }

                if(player instanceof PlayerEntity) {
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
}
