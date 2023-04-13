package net.messer.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.messer.mystical_index.block.custom.LibraryInventoryBlock;
import net.messer.mystical_index.block.entity.LibraryBlockEntity;
import net.messer.mystical_index.events.MixinHooks;
import net.messer.mystical_index.item.custom.BaseStorageBook;
import net.minecraft.block.entity.Hopper;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(HopperBlockEntity.class)
public class HopperBlockEntityMixin {
    @ModifyExpressionValue(method = "extract(Lnet/minecraft/world/World;Lnet/minecraft/block/entity/Hopper;)Z",
    at = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/HopperBlockEntity;getInputInventory(Lnet/minecraft/world/World;Lnet/minecraft/block/entity/Hopper;)Lnet/minecraft/inventory/Inventory;"))
    private static Inventory getInputInventory(World world, Hopper hopper, Inventory inventory) {
        return MixinHooks.getInputInventory(world, hopper, inventory);
    }
}
