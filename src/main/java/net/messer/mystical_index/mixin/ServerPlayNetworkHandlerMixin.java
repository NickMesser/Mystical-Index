package net.messer.mystical_index.mixin;

import net.messer.mystical_index.block.entity.MysticalLecternBlockEntity;
import net.messer.mystical_index.item.ModItems;
import net.messer.mystical_index.item.custom.book.MysticalBookItem;
import net.messer.mystical_index.util.LecternTracker;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.messer.mystical_index.block.entity.MysticalLecternBlockEntity.LECTERN_DETECTION_RADIUS;

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
            ItemStack book = null;
            for (Hand hand : Hand.values()) {
                book = player.getStackInHand(hand);
                if (book.isOf(ModItems.MYSTICAL_BOOK)) {
                    break;
                }
            }

            if (book != null && book.isOf(ModItems.MYSTICAL_BOOK) &&
                    ((MysticalBookItem) book.getItem()).interceptsChatMessage(book, player, message)) {
                ItemStack finalBook = book;
                server.execute(() -> {
                    ((MysticalBookItem) finalBook.getItem()).onInterceptedChatMessage(finalBook, player, message);
                });
                info.cancel();
            } else {
                MysticalLecternBlockEntity lectern = LecternTracker.findNearestLectern(player, LECTERN_DETECTION_RADIUS);
                if (lectern != null &&
                        ((MysticalBookItem) lectern.getBook().getItem()).lectern$interceptsChatMessage(lectern, player, message)) {
                    server.execute(() -> {
                        ((MysticalBookItem) lectern.getBook().getItem()).lectern$onInterceptedChatMessage(lectern, player, message);
                    });
                    info.cancel();
                }
            }
        }
    }
}
