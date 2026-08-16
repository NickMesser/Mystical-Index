package net.messer.mystical_index.item.custom;

import net.messer.config.ModConfig;
import net.messer.mystical_index.item.custom.base_books.BaseStorageBook;
import net.messer.mystical_index.item.inventory.MultiTypeBookInventory;
import net.messer.mystical_index.item.inventory.BookContentsTooltipData;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.item.TooltipData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class TieredStorageBook extends BaseStorageBook {

    private static final String[] TIER_NUMERALS = {"I", "II", "III", "IV"};

    private final int tier;

    public TieredStorageBook(Settings settings, int tier) {
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
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if(world.isClient)
            return super.use(world, user, hand);

        ItemStack stack = user.getStackInHand(hand);
        var inventory = getInventory(stack);

        if(user.isSneaking())
            return deposit(world, user, hand, stack, inventory);

        // Withdraw: one stack of whatever sits in the first occupied slot.
        for (int slot = 0; slot < inventory.size(); slot++) {
            var stored = inventory.getStack(slot);
            if(stored.isEmpty())
                continue;

            var removed = inventory.removeStack(slot, Math.min(stored.getCount(), stored.getMaxCount()));
            if(removed.isEmpty())
                break;

            user.getInventory().offerOrDrop(removed);
            user.getItemCooldownManager().set(this, 10);
            return TypedActionResult.success(stack);
        }

        return super.use(world, user, hand);
    }

    // Sweeps the hotbar and main inventory into the types this book already holds. New types are
    // never claimed here, so a deposit can never surprise the player by eating something.
    private TypedActionResult<ItemStack> deposit(World world, PlayerEntity user, Hand hand, ItemStack stack, MultiTypeBookInventory inventory) {
        var playerInventory = user.getInventory();
        boolean moved = false;

        for (int slot = 0; slot < playerInventory.main.size(); slot++) {
            var candidate = playerInventory.main.get(slot);
            if(candidate.isEmpty() || candidate == stack)
                continue;

            int before = candidate.getCount();
            inventory.tryAddStack(candidate, false);
            if(candidate.getCount() == before)
                continue;

            moved = true;
            if(candidate.isEmpty())
                playerInventory.main.set(slot, ItemStack.EMPTY);
        }

        if(!moved)
            return super.use(world, user, hand);

        world.playSound(null, user.getBlockPos(), SoundEvents.ITEM_BUNDLE_INSERT, SoundCategory.PLAYERS, 1.0f, 1.0f);
        user.getItemCooldownManager().set(this, 10);
        return TypedActionResult.success(stack);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return !getInventory(stack).isEmpty();
    }

    @Override
    public Optional<TooltipData> getTooltipData(ItemStack stack) {
        var inventory = getInventory(stack);
        if(inventory.isEmpty())
            return Optional.empty();

        var summaries = inventory.getTypeSummaries();
        return Optional.of(new BookContentsTooltipData(summaries, summaries.size(), inventory.getTypeCapacity()));
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.literal("§7Tier " + getTierNumeral()));

        var inventory = getInventory(stack);
        var summaries = inventory.getTypeSummaries();
        var stacksPerType = inventory.stacksPerType;

        tooltip.add(Text.literal("§7Types: " + summaries.size() + "/" + inventory.getTypeCapacity()
                + " - " + stacksPerType + (stacksPerType == 1 ? " stack" : " stacks") + " each"));

        // The per-type list lives in BookContentsTooltipComponent's grid now.
        tooltip.add(Text.literal(""));

        if(Screen.hasShiftDown()){
            tooltip.add(Text.translatable("tooltip.mystical_index.holding_book_shift0"));
            tooltip.add(Text.translatable("tooltip.mystical_index.holding_book_shift1"));
            tooltip.add(Text.translatable("tooltip.mystical_index.holding_book_shift2"));
            tooltip.add(Text.translatable("tooltip.mystical_index.holding_book_shift3"));
        } else {
            tooltip.add(Text.translatable("tooltip.mystical_index.holding_book"));
        }

        super.appendTooltip(stack, world, tooltip, context);
    }
}
