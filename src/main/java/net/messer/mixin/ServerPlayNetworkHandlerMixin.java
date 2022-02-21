package net.messer.mixin;

import net.messer.mystical_index.item.ModItems;
import net.messer.mystical_index.util.request.LibraryIndex;
import net.messer.mystical_index.util.ParticleSystem;
import net.messer.mystical_index.util.request.ExtractionRequest;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Hand;
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

    @Inject(method = "onChatMessage", at = @At(value = "INVOKE", target = "Ljava/lang/String;startsWith(Ljava/lang/String;)Z", shift = At.Shift.BEFORE), cancellable = true)
    public void onMessage(ChatMessageC2SPacket packet, CallbackInfo info) {
        String message = packet.getChatMessage();

        if (player.getStackInHand(Hand.MAIN_HAND).getItem() == ModItems.INDEX ||
                player.getStackInHand(Hand.OFF_HAND).getItem() == ModItems.INDEX) { // TODO startswith /
            server.execute(() -> {
                LibraryIndex index = LibraryIndex.get(player.getWorld(), player.getBlockPos());
                ExtractionRequest request = ExtractionRequest.get(message);
                request.setSourcePosition(player.getPos());
                request.setBlockAffectedCallback(ParticleSystem::extractionParticles);

                List<ItemStack> extracted = index.extractItems(request);

                for (ItemStack stack : extracted)
                    player.getInventory().offerOrDrop(stack);

                if (request.hasMatched())
                    player.sendMessage(new LiteralText("extracted " + request.getTotalAmountAffected() + " of " + request.getMatchedItem().getName().getString()), false); // TODO
                else
                    player.sendMessage(new LiteralText("no match"), false);
            });
            info.cancel();
        }
    }
}
