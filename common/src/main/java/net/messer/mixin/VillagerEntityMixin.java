package net.messer.mixin;

import net.messer.mystical_index.events.MixinHooks;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.village.TradeOffer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VillagerEntity.class)
public abstract class VillagerEntityMixin {
    @Shadow
    private @Nullable PlayerEntity lastCustomer;

    @Inject(method = "afterUsing", at = @At("TAIL"))
    private void afterUsing(TradeOffer offer, CallbackInfo ci) {
        MixinHooks.afterUsing(offer, ci, this.lastCustomer, (VillagerEntity) (Object) this);
    }
}
