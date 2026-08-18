package net.messer.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Villager.class)
public interface VillagerEntityInvoker {
    @Invoker("shouldIncreaseLevel")
    public boolean getCanLevelUp();

    // The level-up now needs the server level handed to it, so the call site passes one.
    @Invoker("increaseMerchantCareer")
    public void invokeLevelUp(ServerLevel level);
}
