package net.messer.mystical_index.item.custom;

import net.messer.mystical_index.item.inventory.ExperienceBookData;
import net.messer.mystical_index.screen.ExperienceScreenHandler;
import net.messer.util.GlintingBook;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.messer.util.SelfUpdatingBook;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * Stores experience as raw points and hands it back a level at a time.
 *
 * <p>A {@link SelfUpdatingBook} because auto-collect rewrites its own data while carried - the
 * marker keeps the hand from re-raising on every orb, and it is also what lets the book work from
 * inside a Book Sling.
 */
public class ExperienceBook extends Item implements GlintingBook, SelfUpdatingBook {

    public ExperienceBook(Item.Properties settings) {
        super(settings);
    }

    public ExperienceBookData getData(ItemStack stack) {
        return new ExperienceBookData(stack);
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        if (world.isClientSide())
            return InteractionResult.SUCCESS;

        if (user instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (syncId, inventory, viewer) -> new ExperienceScreenHandler(syncId, inventory),
                    Component.translatable("container.mystical_index.experience_book")));
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public boolean shouldGlint(ItemStack stack) {
        return !new ExperienceBookData(stack).isEmpty();
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag type) {
        var data = new ExperienceBookData(stack);

        tooltip.accept(Component.translatable("tooltip.mystical_index.experience_book.stored",
                data.storedLevels()));
        tooltip.accept(Component.translatable("tooltip.mystical_index.experience_book.points",
                data.points()));
        tooltip.accept(Component.translatable("tooltip.mystical_index.experience_book.auto",
                Component.translatable(data.autoCollect()
                        ? "tooltip.mystical_index.experience_book.auto_on"
                        : "tooltip.mystical_index.experience_book.auto_off")));

        if (Minecraft.getInstance().hasShiftDown())
            tooltip.accept(Component.translatable("tooltip.mystical_index.experience_book_shift0"));
        else
            tooltip.accept(Component.translatable("tooltip.mystical_index.experience_book"));

        super.appendHoverText(stack, context, display, tooltip, type);
    }
}
