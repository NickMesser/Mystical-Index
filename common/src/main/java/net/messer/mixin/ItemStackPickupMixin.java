package net.messer.mixin;

import net.messer.mystical_index.events.MixinHooks;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Inventory.class)
public class ItemStackPickupMixin {

    // Mojang name, unobfuscated: Inventory.add(ItemStack). Descriptors in mixin targets are matched
    // against the runtime names verbatim - there is no refmap on 26.x to fix a stale string up.
    @Inject(method = "add(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void onItemPickup(ItemStack stack, CallbackInfoReturnable<Boolean> cir){
        if(MixinHooks.interceptPickup((Inventory)(Object) this, stack)) cir.setReturnValue(true);
    }
}
