package net.messer.mystical_index.item.custom;

import net.messer.config.ModConfig;
import net.messer.mystical_index.block.entity.LibraryBlockEntity;
import net.messer.mystical_index.item.inventory.LibraryNetwork;
import net.messer.util.GlintingBook;
import net.messer.util.MysticalUtil;
import net.messer.util.SelfUpdatingBook;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Binds a Scriptorium to a Library and pumps produce from one to the other.
 *
 * <p>A {@link SelfUpdatingBook}, so the shared predicate admits it into Scriptorium slots and Book
 * Slings alike. Two distinct jobs: from a Scriptorium slot it pumps that block's contents into the
 * bound Library on a timer, and in the hand it deposits matching items on a sneak-use. Both go
 * through {@link #resolveLibrary}, so neither can reach a Library the other could not.
 */
public class TransportBook extends Item implements GlintingBook, SelfUpdatingBook {

    private static final String DIMENSION_KEY = "TransportDim";
    private static final String X_KEY = "TransportX";
    private static final String Y_KEY = "TransportY";
    private static final String Z_KEY = "TransportZ";

    public TransportBook(Item.Properties settings) {
        super(settings);
    }

    // ---- binding ------------------------------------------------------------------------------

    @Nullable
    public static BlockPos boundPos(ItemStack stack) {
        CompoundTag compound = MysticalUtil.getCustomData(stack);
        if (compound == null || !compound.contains(X_KEY))
            return null;

        return new BlockPos(compound.getIntOr(X_KEY, 0), compound.getIntOr(Y_KEY, 0), compound.getIntOr(Z_KEY, 0));
    }

    public static String boundDimension(ItemStack stack) {
        CompoundTag compound = MysticalUtil.getCustomData(stack);
        return compound == null ? "" : compound.getStringOr(DIMENSION_KEY, "");
    }

    public static boolean isBound(ItemStack stack) {
        return boundPos(stack) != null;
    }

    private static void bind(ItemStack stack, Level world, BlockPos pos) {
        var dimension = world.dimension().identifier().toString();
        MysticalUtil.editCustomData(stack, compound -> {
            compound.putString(DIMENSION_KEY, dimension);
            compound.putInt(X_KEY, pos.getX());
            compound.putInt(Y_KEY, pos.getY());
            compound.putInt(Z_KEY, pos.getZ());
        });
    }

    /**
     * Sneak-click a Library to bind to it. Clicking anything else leaves the binding alone - the
     * gesture is deliberately narrow so it cannot be lost by a stray click on a wall.
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        var world = context.getLevel();
        var player = context.getPlayer();
        if (world.isClientSide() || player == null || !player.isShiftKeyDown())
            return super.useOn(context);

        var pos = context.getClickedPos();
        if (!(world.getBlockEntity(pos) instanceof LibraryBlockEntity))
            return super.useOn(context);

        var stack = context.getItemInHand();
        bind(stack, world, pos);
        player.sendOverlayMessage(Component.translatable("message.mystical_index.transport_bound",
                pos.getX(), pos.getY(), pos.getZ()));

        return InteractionResult.SUCCESS;
    }

    // ---- held ability -------------------------------------------------------------------------

    /** Lockout on the held deposit, so a held right-click cannot sweep every tick. */
    private static final int DEPOSIT_COOLDOWN = 20;

    /**
     * Sneak-use with a bound book sweeps the player's inventory into the bound Library.
     *
     * <p>Binding is the sneak gesture ON a Library, and that path consumes the click before this
     * one is ever reached - so the two never collide and the deposit is what sneak-use does
     * everywhere else.
     */
    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        var stack = user.getItemInHand(hand);
        // Unbound or not sneaking: behave like an ordinary book rather than swallowing the click.
        if (!user.isShiftKeyDown() || !isBound(stack))
            return super.use(world, user, hand);
        if (world.isClientSide() || user.getCooldowns().isOnCooldown(stack))
            return InteractionResult.SUCCESS;

        user.getCooldowns().addCooldown(stack, DEPOSIT_COOLDOWN);

        // Resolved from the PLAYER, which is also what the tooltip measures from - so the range
        // reading a player sees while holding the book is the one this gesture actually applies.
        var library = resolveLibrary(world, stack, user.blockPosition());
        if (library == null) {
            user.sendOverlayMessage(Component.translatable("message.mystical_index.transport_unreachable"));
            return InteractionResult.SUCCESS;
        }

        int deposited = deposit(user, stack, library);
        if (deposited > 0)
            world.playSound(null, user.blockPosition(), SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 1.0f, 1.0f);

        user.sendOverlayMessage(deposited > 0
                ? Component.translatable("message.mystical_index.transport_deposited", deposited)
                : Component.translatable("message.mystical_index.transport_nothing"));

        return InteractionResult.SUCCESS;
    }

    /**
     * Sweeps hotbar and main inventory into the types the Library already holds.
     *
     * <p>Same shape as {@code TieredStorageBook.deposit} on purpose, down to the held-stack guard
     * and the emptied-slot write-back: that gesture is playtested, and a second sweep with subtly
     * different rules is exactly the drift worth avoiding. Existing types only is what makes it
     * safe to fire without looking - a deposit can never claim a new type slot, so it can never
     * eat something the Library was not already stocking.
     */
    private static int deposit(Player user, ItemStack held, LibraryBlockEntity library) {
        var items = user.getInventory().getNonEquipmentItems();
        int deposited = 0;

        for (int slot = 0; slot < items.size(); slot++) {
            var candidate = items.get(slot);
            if (candidate.isEmpty() || candidate == held)
                continue;

            deposited += (int) LibraryNetwork.insertExistingIntoLibrary(library, candidate);
            if (candidate.isEmpty())
                items.set(slot, ItemStack.EMPTY);
        }

        return deposited;
    }

    // ---- link resolution ----------------------------------------------------------------------

    /**
     * The bound Library, or null if the link cannot be used from {@code from} right now.
     *
     * <p>The single gate for every constraint - dimension, range, chunk residency and the block
     * entity still being there. Both the block pump and the held ability go through it, so neither
     * can drift into a laxer rule than the other.
     *
     * <p>{@code create = false} is load bearing: {@link Level#getBlockEntity} force-loads the chunk
     * it is asked about, which would let a bound book hold a chunk open from anywhere in range. An
     * unloaded Library is simply unreachable until something else loads it, exactly as the lectern
     * network already treats them.
     */
    @Nullable
    public static LibraryBlockEntity resolveLibrary(Level world, ItemStack stack, BlockPos from) {
        var pos = boundPos(stack);
        if (pos == null || !world.dimension().identifier().toString().equals(boundDimension(stack)))
            return null;

        int range = ModConfig.TransportRange;
        if (pos.distSqr(from) > (double) range * range)
            return null;

        var chunk = world.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.FULL, false);
        if (!(chunk instanceof LevelChunk loaded))
            return null;

        return loaded.getBlockEntity(pos) instanceof LibraryBlockEntity library && !library.isRemoved()
                ? library
                : null;
    }

    // ---- display ------------------------------------------------------------------------------

    @Override
    public boolean shouldGlint(ItemStack stack) {
        return isBound(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag type) {
        var pos = boundPos(stack);
        if (pos == null) {
            tooltip.accept(Component.translatable("tooltip.mystical_index.transport_book.unbound"));
        } else {
            tooltip.accept(Component.translatable("tooltip.mystical_index.transport_book.bound",
                    pos.getX(), pos.getY(), pos.getZ()));
            tooltip.accept(status(stack, pos));
        }

        if (Minecraft.getInstance().hasShiftDown()) {
            tooltip.accept(Component.translatable("tooltip.mystical_index.transport_book_shift0"));
            tooltip.accept(Component.translatable("tooltip.mystical_index.transport_book_shift1"));
            tooltip.accept(Component.translatable("tooltip.mystical_index.transport_book_shift2"));
        } else {
            tooltip.accept(Component.translatable("tooltip.mystical_index.transport_book"));
        }

        super.appendHoverText(stack, context, display, tooltip, type);
    }

    /**
     * Live validity, from position maths only.
     *
     * <p>Deliberately no chunk or block-entity query: this runs every frame the tooltip is up, and
     * the client has no business forcing loads to answer it. Dimension and distance are enough to
     * tell the player why a link is not pumping.
     */
    private static Component status(ItemStack stack, BlockPos pos) {
        var world = MysticalUtil.tooltipWorld();
        var player = Minecraft.getInstance().player;
        if (world == null || player == null)
            return Component.translatable("tooltip.mystical_index.transport_book.unknown");

        if (!world.dimension().identifier().toString().equals(boundDimension(stack)))
            return Component.translatable("tooltip.mystical_index.transport_book.wrong_dimension");

        double distance = Math.sqrt(pos.distToCenterSqr(player.position()));
        return distance <= ModConfig.TransportRange
                ? Component.translatable("tooltip.mystical_index.transport_book.linked")
                : Component.translatable("tooltip.mystical_index.transport_book.out_of_range",
                        (int) Math.round(distance), ModConfig.TransportRange);
    }
}
