package net.messer.mixin;

import net.messer.mystical_index.item.custom.Index;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.LecternScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LecternScreenHandler.class)
public class LecternScreenHandlerMixin {
    @ModifyVariable(
            method = "onButtonClick",
            at = @At(value = "STORE")
    )
    public ItemStack modifyTakenItem(ItemStack i) {
        return Index.getFromLecternBook(i);
    }
}
