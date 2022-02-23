package net.messer.mixin;

import net.messer.mystical_index.item.ModItems;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.LecternScreenHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LecternScreenHandler.class)
public class LecternScreenHandlerMixin {
    @Shadow @Final private Inventory inventory;

    @Inject(method = "getBookItem", at = @At("HEAD"), cancellable = true)
    public void getBookItem(CallbackInfoReturnable<ItemStack> cir) {
        if (inventory.getStack(0).getItem() == ModItems.INDEX) {
            cir.setReturnValue(inventory.getStack(1));
        }
    }
}
