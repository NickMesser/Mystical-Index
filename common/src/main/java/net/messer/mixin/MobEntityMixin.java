package net.messer.mixin;

import net.messer.mystical_index.events.MixinHooks;
import net.messer.mystical_index.item.ModItems;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(Mob.class)
public abstract class MobEntityMixin extends LivingEntity {
    protected MobEntityMixin(EntityType<? extends LivingEntity> entityType, Level world) {
        super(entityType, world);
    }
    /**
     * Mojang name: {@code checkAndHandleImportantInteractions(Player, InteractionHand)} - the
     * private helper that handles name tags and spawn eggs, not the public {@code mobInteract}.
     *
     * <p>The name-tag check compiles to {@code ItemStack.is} with an ERASED {@code (Object)Z}
     * descriptor, so that is what the target has to say; the pre-26 {@code isOf(Item)} form no
     * longer exists. Only the item stack local is live at that call.
     */
    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Ljava/lang/Object;)Z", ordinal = 0), method = "checkAndHandleImportantInteractions", locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
    public void interactWithItem(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir, ItemStack itemStack) {
        LivingEntity entity = this;
        MixinHooks.interactWithItem(player, hand, cir, itemStack, entity);
    }
}
