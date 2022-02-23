package net.messer.mixin;

import net.messer.mystical_index.item.ModItems;
import net.messer.mystical_index.item.custom.Index;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.screen.LecternScreenHandler;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LecternBlockEntity.class)
public class LecternBlockEntityMixin {
    @Shadow @Final private Inventory inventory;

    @Inject(method = "createMenu", at = @At("HEAD"))
    public void createMenu(int i, PlayerInventory playerInventory, PlayerEntity playerEntity, CallbackInfoReturnable<ScreenHandler> cir) {
        Item item = inventory.getStack(0).getItem();
        if (playerEntity instanceof ServerPlayerEntity serverPlayer && item == ModItems.INDEX) {
            BlockPos pos = ((BlockEntity) (Object) this).getPos();
            inventory.setStack(1, ((Index) ModItems.INDEX).getMenuItem(serverPlayer, pos).asStack());
//            Inventory bookInventory = new BookInventory(new BookGui(serverPlayer, ));
        }
    }
}
