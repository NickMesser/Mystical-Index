package net.messer.mixin;

import net.minecraft.entity.passive.PassiveEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PassiveEntity.class)
public interface PassiveEntityAccessor {
    @Accessor("breedingAge")
    public int getBreedingAgeNumber();
}
