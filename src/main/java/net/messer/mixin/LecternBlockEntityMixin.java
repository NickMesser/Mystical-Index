package net.messer.mixin;

import net.messer.mystical_index.item.custom.book.Index;
import net.messer.mystical_index.util.request.LibraryIndex;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.WrittenBookItem;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LecternBlockEntity.class)
public abstract class LecternBlockEntityMixin {
    @Shadow ItemStack book;

    @Shadow private int pageCount;

    @Inject(method = "createMenu", at = @At("HEAD"))
    public void createMenu(int i, PlayerInventory playerInventory, PlayerEntity playerEntity, CallbackInfoReturnable<ScreenHandler> cir) {
        if (book.getOrCreateNbt().contains(Index.LECTERN_TAG_NAME)) {
            BlockPos pos = ((BlockEntity) (Object) this).getPos();
            World world = ((BlockEntity) (Object) this).getWorld();
            ItemStack stack = Index.getMenuItem((ServerWorld) world, pos, LibraryIndex.LECTERN_SEARCH_RANGE).asStack();
            stack.getOrCreateNbt().put(Index.LECTERN_TAG_NAME, book.getOrCreateNbt().get(Index.LECTERN_TAG_NAME));
            book = stack;
            pageCount = WrittenBookItem.getPageCount(book);
            ((LecternBlockEntity) (Object) this).markDirty();
        }
    }
}
