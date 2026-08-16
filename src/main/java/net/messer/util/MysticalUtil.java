package net.messer.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import java.util.List;

public class MysticalUtil {
    public static List<ItemStack> generateEntityLoot(PlayerEntity player, Entity entity, Identifier storedEntityLootTable){
        World world = player.getEntityWorld();
        var fakeSword = new ItemStack(Items.DIAMOND_SWORD);
        player.setStackInHand(Hand.MAIN_HAND, fakeSword);
        var source = player.getDamageSources().playerAttack(player);

        LootContextParameterSet context = new LootContextParameterSet.Builder((ServerWorld) world)
                .add(LootContextParameters.THIS_ENTITY, entity)
                .add(LootContextParameters.ORIGIN, player.getPos())
                .add(LootContextParameters.DAMAGE_SOURCE, source)
                .add(LootContextParameters.KILLER_ENTITY, player)
                .add(LootContextParameters.DIRECT_KILLER_ENTITY, player)
                .add(LootContextParameters.LAST_DAMAGE_PLAYER, player)
                .build(LootContextTypes.ENTITY);

        fakeSword.decrement(1);
        LootTable lootTable = world.getServer().getLootManager().getLootTable(storedEntityLootTable);
        return lootTable.generateLoot(context);
    }
}
