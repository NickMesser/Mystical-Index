package net.messer.mystical_index.item.custom;

import net.messer.util.SelfUpdatingBook;
import net.messer.config.ModConfig;
import net.messer.mystical_index.item.custom.base_books.BaseStorageBook;
import net.messer.mystical_index.item.inventory.MultiTypeBookInventory;
import net.messer.mystical_index.item.inventory.BookContentsTooltipData;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

import net.minecraft.client.Minecraft;

import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

import net.messer.util.GlintingBook;

public class TieredStorageBook extends BaseStorageBook implements GlintingBook, SelfUpdatingBook {

    private static final String[] TIER_NUMERALS = {"I", "II", "III", "IV"};

    private final int tier;

    public TieredStorageBook(Item.Properties settings, int tier) {
        super(settings);
        this.tier = tier;
    }

    public int getTier() {
        return tier;
    }

    public int getMaxTypes() {
        return switch (tier) {
            case 2 -> ModConfig.HoldingBookTier2Types;
            case 3 -> ModConfig.HoldingBookTier3Types;
            case 4 -> ModConfig.HoldingBookTier4Types;
            default -> ModConfig.HoldingBookTier1Types;
        };
    }

    public int getStacksPerType() {
        return switch (tier) {
            case 2 -> ModConfig.HoldingBookTier2StacksPerType;
            case 3 -> ModConfig.HoldingBookTier3StacksPerType;
            case 4 -> ModConfig.HoldingBookTier4StacksPerType;
            default -> ModConfig.HoldingBookTier1StacksPerType;
        };
    }

    // Every inventory access goes through here; a hardcoded size would silently truncate a book
    // of a different tier.
    @Override
    public MultiTypeBookInventory getInventory(ItemStack stack) {
        return new MultiTypeBookInventory(stack, getMaxTypes(), getStacksPerType());
    }

    private String getTierNumeral() {
        var index = Math.min(Math.max(tier, 1), TIER_NUMERALS.length) - 1;
        return TIER_NUMERALS[index];
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        if(world.isClientSide())
            return super.use(world, user, hand);

        ItemStack stack = user.getItemInHand(hand);
        var inventory = getInventory(stack);

        if(user.isShiftKeyDown())
            return deposit(world, user, hand, stack, inventory);

        // Withdraw: one stack of whatever sits in the first occupied slot.
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            var stored = inventory.getItem(slot);
            if(stored.isEmpty())
                continue;

            var removed = inventory.removeItem(slot, Math.min(stored.getCount(), stored.getMaxStackSize()));
            if(removed.isEmpty())
                break;

            user.getInventory().placeItemBackInInventory(removed);
            user.getCooldowns().addCooldown(user.getItemInHand(hand), 10);
            return InteractionResult.SUCCESS;
        }

        return super.use(world, user, hand);
    }

    // Sweeps the hotbar and main inventory into the types this book already holds. New types are
    // never claimed here, so a deposit can never surprise the player by eating something.
    private InteractionResult deposit(Level world, Player user, InteractionHand hand, ItemStack stack, MultiTypeBookInventory inventory) {
        var playerInventory = user.getInventory();
        boolean moved = false;

        for (int slot = 0; slot < playerInventory.getNonEquipmentItems().size(); slot++) {
            var candidate = playerInventory.getNonEquipmentItems().get(slot);
            if(candidate.isEmpty() || candidate == stack)
                continue;

            int before = candidate.getCount();
            inventory.tryAddStack(candidate, false);
            if(candidate.getCount() == before)
                continue;

            moved = true;
            if(candidate.isEmpty())
                playerInventory.getNonEquipmentItems().set(slot, ItemStack.EMPTY);
        }

        if(!moved)
            return super.use(world, user, hand);

        world.playSound(null, user.blockPosition(), SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 1.0f, 1.0f);
        user.getCooldowns().addCooldown(user.getItemInHand(hand), 10);
        return InteractionResult.SUCCESS;
    }
    @Override
    public boolean shouldGlint(ItemStack stack) {
        return !getInventory(stack).isEmpty();
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        var inventory = getInventory(stack);
        if(inventory.isEmpty())
            return Optional.empty();

        var summaries = inventory.getTypeSummaries();
        return Optional.of(new BookContentsTooltipData(summaries, summaries.size(), inventory.getTypeCapacity()));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag type) {
        tooltip.accept(Component.translatable("tooltip.mystical_index.holding_book.tier", getTierNumeral()));

        var inventory = getInventory(stack);
        var summaries = inventory.getTypeSummaries();
        var stacksPerType = inventory.stacksPerType;

        if(stacksPerType == 1)
            tooltip.accept(Component.translatable("tooltip.mystical_index.holding_book.types_single",
                    summaries.size(), inventory.getTypeCapacity(), stacksPerType));
        else
            tooltip.accept(Component.translatable("tooltip.mystical_index.holding_book.types_multiple",
                    summaries.size(), inventory.getTypeCapacity(), stacksPerType));

        // The per-type list lives in BookContentsTooltipComponent's grid now.
        tooltip.accept(Component.literal(""));

        if(Minecraft.getInstance().hasShiftDown()){
            tooltip.accept(Component.translatable("tooltip.mystical_index.holding_book_shift0"));
            tooltip.accept(Component.translatable("tooltip.mystical_index.holding_book_shift1"));
            tooltip.accept(Component.translatable("tooltip.mystical_index.holding_book_shift2"));
            tooltip.accept(Component.translatable("tooltip.mystical_index.holding_book_shift3"));
        } else {
            tooltip.accept(Component.translatable("tooltip.mystical_index.holding_book"));
        }

        super.appendHoverText(stack, context, display, tooltip, type);
    }
}
