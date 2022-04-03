package net.messer.mixin;

import net.messer.mystical_index.block.entity.IndexLecternBlockEntity;
import net.messer.mystical_index.item.ModItems;
import net.messer.mystical_index.item.custom.book.CustomIndexBook;
import net.messer.mystical_index.util.LecternTracker;
import net.messer.mystical_index.util.WorldEffects;
import net.messer.mystical_index.util.request.ExtractionRequest;
import net.messer.mystical_index.util.request.LibraryIndex;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
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
                    LibraryIndex index = LibraryIndex.fromRange(player.getWorld(), player.getBlockPos(), LibraryIndex.ITEM_SEARCH_RANGE);
                    ExtractionRequest request = ExtractionRequest.get(message);
                    request.setSourcePosition(player.getPos().add(0, 1, 0));
                    request.setBlockAffectedCallback(WorldEffects::extractionParticles);

                    List<ItemStack> extracted = index.extractItems(request);

                    for (ItemStack stack : extracted)
                        player.getInventory().offerOrDrop(stack);

                    player.sendMessage(request.getMessage(), false);
                });
                info.cancel();
            } else { // TODO clean this up
                IndexLecternBlockEntity lectern = LecternTracker.findNearestLectern(player, CustomIndexBook.LECTERN_PICKUP_RADIUS);
                if (lectern != null) {
                    server.execute(() -> {
                        ServerWorld world = player.getWorld();
                        BlockPos blockPos = lectern.getPos();

                        LibraryIndex index = lectern.getLinkedLibraries();
                        ExtractionRequest request = ExtractionRequest.get(message);
                        request.setSourcePosition(Vec3d.ofCenter(blockPos, 0.5));
                        request.setBlockAffectedCallback(WorldEffects::extractionParticles);

                        List<ItemStack> extracted = index.extractItems(request);

                        Vec3d itemPos = Vec3d.ofCenter(blockPos, 1);
                        for (ItemStack stack : extracted) {
                            ItemEntity itemEntity = new ItemEntity(world, itemPos.getX(), itemPos.getY(), itemPos.getZ(), stack);
                            itemEntity.setToDefaultPickupDelay();
                            itemEntity.setVelocity(Vec3d.ZERO);
                            itemEntity.setThrower(CustomIndexBook.EXTRACTED_DROP_UUID);
                            world.spawnEntity(itemEntity);
                        }

                        if (request.hasAffected()) {
                            world.playSound(null, itemPos.getX(), itemPos.getY(), itemPos.getZ(),
                                    SoundEvents.BLOCK_AMETHYST_BLOCK_STEP, SoundCategory.BLOCKS,
                                    0.5f, 1f + world.getRandom().nextFloat() * 0.4f);
                            world.spawnParticles(
                                    ParticleTypes.SOUL_FIRE_FLAME, itemPos.getX(), itemPos.getY(), itemPos.getZ(),
                                    5, 0, 0, 0, 0.1);
                        }

                        player.sendMessage(request.getMessage(), false);
                    });
                    info.cancel();
                }
            }
        }
    }
}
