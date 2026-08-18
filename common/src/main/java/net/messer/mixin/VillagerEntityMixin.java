package net.messer.mixin;

import net.messer.mystical_index.events.MixinHooks;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public abstract class VillagerEntityMixin {
    @Shadow
    private @Nullable Player lastTradedPlayer;

    // Mojang name: Villager.rewardTradeXp(MerchantOffer); the shadowed field is lastTradedPlayer.
    @Inject(method = "rewardTradeXp", at = @At("TAIL"))
    private void rewardTradeXp(MerchantOffer offer, CallbackInfo ci) {
        MixinHooks.afterUsing(offer, ci, this.lastTradedPlayer, (Villager) (Object) this);
    }
}
