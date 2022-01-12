package net.messer.mixin;

import net.messer.mystical_index.events.MixinHooks;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerInventory.class)
public class ItemStackPickupMixin {

    @Inject(method = "insertStack(Lnet/minecraft/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void onItemPickup(ItemStack stack, CallbackInfoReturnable<Boolean> cir){
        if(MixinHooks.interceptPickup((PlayerInventory)(Object) this, stack)) cir.setReturnValue(true);
    }

//    private void onItemAdd

//    @Inject(method = { "onItemPickup" }, at = @At(value = "HEAD"))
//    private void onItemPickup(ItemEntity itemEntity, CallbackInfo info)
//    {
//        if (this instanceof Player)
//        {
//            Player player = (Player)(Object)this;
//            ItemStackPickupCallBack.EVENT.invoker().onItemPickup(player.getUUID(), itemEntity.getItem());
//        }
//    }
}
