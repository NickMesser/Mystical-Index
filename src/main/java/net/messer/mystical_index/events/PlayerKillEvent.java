package net.messer.mystical_index.events;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.messer.mystical_index.item.ModItems;
import net.messer.mystical_index.item.custom.HusbandryBook;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public class PlayerKillEvent {
    public static void init() {
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, player, entity) -> {
            if (entity.isDead() && entity != player) {
                if(player instanceof PlayerEntity) {
                    var offHandStack = ((PlayerEntity) player).getEquippedStack(EquipmentSlot.OFFHAND);
                    if(offHandStack.getItem() instanceof HusbandryBook husbandryBook) {
                        husbandryBook.onKill(offHandStack, entity);
                    }
                }
            }
        });
    }
}
