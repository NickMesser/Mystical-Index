package net.messer.mixin;

import net.messer.mystical_index.item.custom.Index;
import net.minecraft.block.LecternBlock;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LecternBlock.class)
public class LecternBlockMixin {
    @ModifyVariable(
            method = "dropBook",
            at = @At(value = "STORE")
    )
    public ItemStack modifyDroppedItem(ItemStack i) {
        return Index.getFromLecternBook(i);
    }
}
