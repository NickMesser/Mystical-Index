package net.messer.mixin;

import net.messer.mystical_index.item.ModItems;
import net.messer.mystical_index.item.custom.book.CustomIndexBook;
import net.messer.mystical_index.util.LecternTracker;
import net.messer.mystical_index.util.ParticleSystem;
import net.messer.mystical_index.util.request.ExtractionRequest;
import net.messer.mystical_index.util.request.LibraryIndex;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {
    @Shadow
    public ServerPlayerEntity player;
    @Final
    @Shadow
    private MinecraftServer server;

    @Inject(method = "onChatMessage", at = @At(value = "INVOKE", target = "Ljava/lang/String;startsWith(Ljava/lang/String;)Z"), cancellable = true)
    public void onMessage(ChatMessageC2SPacket packet, CallbackInfo info) {
        String message = packet.getChatMessage();

        if (!(message.startsWith("/") || player.isSpectator())) {
            if (player.getStackInHand(Hand.MAIN_HAND).getItem() == ModItems.CUSTOM_INDEX ||
                    player.getStackInHand(Hand.OFF_HAND).getItem() == ModItems.CUSTOM_INDEX) {
                server.execute(() -> {
                    LibraryIndex index = LibraryIndex.get(player.getWorld(), player.getBlockPos(), LibraryIndex.ITEM_SEARCH_RANGE);
                    ExtractionRequest request = ExtractionRequest.get(message);
                    request.setSourcePosition(player.getPos().add(0, 1, 0));
                    request.setBlockAffectedCallback(ParticleSystem::extractionParticles);

                    List<ItemStack> extracted = index.extractItems(request);

                    for (ItemStack stack : extracted)
                        player.getInventory().offerOrDrop(stack);

                    player.sendMessage(request.getMessage(), false);
                });
                info.cancel();
            } else { // TODO clean this up
                LecternBlockEntity lectern = LecternTracker.findNearestLectern(player, CustomIndexBook.LECTERN_PICKUP_RADIUS);
                if (lectern != null) {
                    server.execute(() -> {
                        World world = player.getWorld();
                        BlockPos blockPos = lectern.getPos();

                        LibraryIndex index = LibraryIndex.get(world, blockPos, LibraryIndex.LECTERN_SEARCH_RANGE);
                        ExtractionRequest request = ExtractionRequest.get(message);
                        request.setSourcePosition(Vec3d.ofCenter(blockPos, 0.5));
                        request.setBlockAffectedCallback(ParticleSystem::extractionParticles);

                        List<ItemStack> extracted = index.extractItems(request);

                        Vec3d itemPos = Vec3d.ofCenter(blockPos, 1);
                        for (ItemStack stack : extracted) {
                            ItemEntity itemEntity = new ItemEntity(world, itemPos.getX(), itemPos.getY(), itemPos.getZ(), stack);
                            itemEntity.setToDefaultPickupDelay();
                            itemEntity.setVelocity(Vec3d.ZERO);
                            itemEntity.setThrower(CustomIndexBook.EXTRACTED_DROP_UUID);
                            world.spawnEntity(itemEntity);
                        }

                        player.sendMessage(request.getMessage(), false);
                    });
                    info.cancel();
                }
            }
        }
    }

//    @ModifyArg(
//            method = "addBook",
//            at = @At(
//                    value = "INVOKE",
//                    target = "Lnet/minecraft/entity/player/PlayerInventory;setStack(ILnet/minecraft/item/ItemStack;)V"
//            )
//    )
//    public ItemStack modifyBook(ItemStack itemStack) {
////        var nbt = CustomInventoryBook.PARSER.parse(itemStack);
////        if (nbt != null) itemStack.getOrCreateNbt().put(MysticalIndex.MOD_ID, nbt);
//
//        return itemStack;
//    }
}
