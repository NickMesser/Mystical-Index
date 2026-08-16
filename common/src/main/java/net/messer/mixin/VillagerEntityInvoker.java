package net.messer.mixin;

import net.minecraft.entity.passive.VillagerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(VillagerEntity.class)
public interface VillagerEntityInvoker {
    @Invoker("canLevelUp")
    public boolean getCanLevelUp();

    @Invoker("levelUp")
    public void invokeLevelUp();
}
