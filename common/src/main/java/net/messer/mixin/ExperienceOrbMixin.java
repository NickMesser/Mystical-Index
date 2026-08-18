package net.messer.mixin;

import net.messer.mystical_index.item.custom.ExperienceBook;
import net.messer.mystical_index.item.inventory.ExperienceBookData;
import net.messer.util.MysticalUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Routes experience orbs into a carried Book of Experience when auto-collect is on.
 *
 * <p><b>Why HEAD.</b> The two loaders do not agree on this method's body: NeoForge inserts a
 * {@code PlayerXpEvent.PickupXp} post and an {@code isCanceled()} check between the takeXpDelay read
 * and the rest, which Fabric has no equivalent of. Injecting anywhere inside that region would need
 * a different target per loader. HEAD sits before the divergence entirely, so one common mixin
 * serves both - and it is the only point that is provably identical on the two jars.
 *
 * <p>The cost of HEAD is that vanilla's own guards have not run yet, so this replicates them
 * exactly, in vanilla's order: server side only, the takeXpDelay gate, the two-tick delay, the
 * pickup animation, and the orb's count/discard bookkeeping. Those were read straight off the
 * bytecode rather than remembered.
 *
 * <p><b>Mending.</b> Vanilla routes points through {@code repairPlayerItems} before the player ever
 * sees them. With auto-collect ON the book takes the orb whole and mending gets nothing - the book
 * intercepts first, deliberately. It is the simpler rule to hold in your head, and the player has a
 * switch: turn auto-collect off and mending works exactly as it always did.
 */
@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbMixin {

    @Shadow
    private int count;

    @Shadow
    public abstract int getValue();

    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void mysticalIndex$collectIntoBook(Player player, CallbackInfo ci) {
        if (!(player instanceof ServerPlayer))
            return;

        // Vanilla's own throttle, checked before ours so a book collects at exactly the rate a
        // player would have picked orbs up by hand.
        if (player.takeXpDelay != 0)
            return;

        int value = getValue();
        boolean[] collected = {false};

        // The deposit happens INSIDE the visit on purpose: forEachEffectiveBook only writes a slung
        // book back if its component changed while the visitor held it. Adding the points after the
        // walk returned would update a loose book and silently lose the change on a slung one.
        MysticalUtil.forEachEffectiveBook(player, book -> {
            if (collected[0] || !(book.getItem() instanceof ExperienceBook))
                return;

            var data = new ExperienceBookData(book);
            if (!data.autoCollect())
                return;

            data.add(value);
            collected[0] = true;
        });

        if (!collected[0])
            return;

        // Everything below mirrors vanilla playerTouch, minus repairPlayerItems and the points
        // going to the player.
        var orb = (ExperienceOrb) (Object) this;
        player.takeXpDelay = 2;
        player.take(orb, 1);

        if (--this.count == 0)
            orb.discard();

        ci.cancel();
    }
}
