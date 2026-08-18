package net.messer.mystical_index.item.custom;

import net.messer.mystical_index.item.inventory.BookContentsTooltipData;
import net.messer.mystical_index.item.inventory.BookSlingInventory;
import net.messer.util.GlintingBook;
import net.messer.mystical_index.screen.BookSlingScreenHandler;
import net.messer.util.MysticalUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.messer.util.SelfUpdatingBook;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Carries up to five effect books and keeps them working while the player has it.
 *
 * <p>It is itself a {@link SelfUpdatingBook}: the books inside rewrite their own data as they tick,
 * which changes the sling's stack, and without the marker the player's hand would re-raise every
 * time any contained book did anything.
 *
 * <p>That marker is also what a sling accepts, so the acceptance rule has to exclude slings
 * explicitly - see {@link BookSlingInventory#accepts}. A sling can never contain a sling, which
 * keeps the effect delegation exactly one level deep by construction rather than by a depth counter.
 */
public class BookSling extends Item implements GlintingBook, SelfUpdatingBook {

    public BookSling(Item.Properties settings) {
        super(settings);
    }

    public BookSlingInventory getInventory(ItemStack stack) {
        return new BookSlingInventory(stack);
    }

    /**
     * Runs the contained books own tick logic.
     *
     * <p>Vanilla ticks every stack in a player inventory, so a LOOSE book already ticks once by
     * itself and this must never touch loose books - it iterates only this sling's contents. That
     * is the whole double-tick guarantee: vanilla covers loose, the sling covers slung, and neither
     * traversal can see the other's population.
     *
     * <p>World, entity and slot pass straight through, so a contained book sees exactly the player
     * vanilla would have handed it - which is what lets a slung saturation book read the right
     * hunger and a slung magnetism book pull toward the right position.
     */
    @Override
    public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, EquipmentSlot slot) {
        MysticalUtil.forEachBookInSling(stack,
                contained -> contained.getItem().inventoryTick(contained, world, entity, slot));

        super.inventoryTick(stack, world, entity, slot);
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        if (world.isClientSide())
            return InteractionResult.SUCCESS;

        if (user instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (syncId, inventory, viewer) -> new BookSlingScreenHandler(syncId, inventory),
                    Component.translatable("container.mystical_index.book_sling")));
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public boolean shouldGlint(ItemStack stack) {
        return !new BookSlingInventory(stack).isEmpty();
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        var inventory = new BookSlingInventory(stack);
        if (inventory.isEmpty())
            return Optional.empty();

        return Optional.of(BookContentsTooltipData.fromInventory(inventory));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag type) {
        if (Minecraft.getInstance().hasShiftDown())
            tooltip.accept(Component.translatable("tooltip.mystical_index.book_sling_shift0"));
        else
            tooltip.accept(Component.translatable("tooltip.mystical_index.book_sling"));

        super.appendHoverText(stack, context, display, tooltip, type);
    }
}
