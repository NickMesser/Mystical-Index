package net.messer.mystical_index.item.custom;

import net.messer.util.SelfUpdatingBook;
import net.messer.config.ModConfig;
import net.messer.mixin.CropBlockInvoker;
import net.messer.mystical_index.block.entity.ScriptoriumBlockEntity;
import net.messer.mystical_index.item.custom.base_books.BaseGeneratingBook;
import net.messer.mystical_index.item.inventory.BookContentsTooltipData;
import net.messer.mystical_index.item.inventory.FarmingBookInventory;
import net.messer.mystical_index.item.inventory.SingleItemStackingInventory;
import net.messer.mystical_index.item.provider.FarmingGrowthModel;
import net.messer.mystical_index.screen.FarmingBookScreenHandler;
import net.messer.util.GlintingBook;
import net.messer.util.MysticalUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * A book that grows one crop for you, botany-pot style.
 *
 * <p>Put soil in one slot and a seed in the other and the book runs a timed cycle: when it
 * completes it rolls the crop's mature drops into its four output slots and starts again. Neither
 * the soil nor the seed is ever consumed - the book is a pot, not a recipe - so the only thing the
 * player has to do is empty the outputs.
 *
 * <p>Time comes from the world clock rather than a tick counter, so a book advances by however much
 * time passed while it was loaded, and catching up after a reload is automatic. It grows the same
 * way in a player's inventory and inside a Library.
 */
public class FarmingBook extends BaseGeneratingBook implements GlintingBook, SelfUpdatingBook {

    private static final String PLANTED_AT_KEY = "plantedAt";
    private static final String LAST_TICK_KEY = "lastTick";
    private static final String LEGACY_CROP_KEY = "cropBlock";

    // How often the stored clock is refreshed. Every tick would rewrite (and resync) the component
    // 20 times a second for a bar that moves in whole percents; a second's granularity is invisible
    // to the player and costs a twentieth as much.
    private static final long CLOCK_INTERVAL = 20L;

    public FarmingBook(Item.Properties settings) {
        super(settings);
    }

    // ---- opening the book -------------------------------------------------------------------

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        if (world.isClientSide())
            return InteractionResult.SUCCESS;

        if (user instanceof ServerPlayer serverPlayer) {
            // Deliberately NOT keyed on the hand this call came from. The menu resolves the book by
            // scanning main hand then offhand, and the client does the identical scan, so a player
            // holding a farming book in each hand always ends up editing the same one on both
            // sides. Opening "the other" book would be the only way for the two to disagree.
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (syncId, inventory, player) -> new FarmingBookScreenHandler(syncId, inventory),
                    Component.translatable("container.mystical_index.farming_book")));
        }

        return InteractionResult.CONSUME;
    }

    // ---- growth ---------------------------------------------------------------------------

    /**
     * The world time this book last observed. Progress is measured against it rather than against
     * the live clock so the client, which never runs this logic, still sees a correct number: the
     * server bakes the answer into the stack and it rides along with the component.
     */
    public static long lastSeenTime(ItemStack stack) {
        var compound = MysticalUtil.getCustomData(stack);
        return compound == null ? 0L : compound.getLongOr(LAST_TICK_KEY, 0L);
    }

    public void tryGrow(ItemStack stack, Level world) {
        if (world.isClientSide())
            return;

        migrateLegacy(stack, world);

        var inventory = new FarmingBookInventory(stack);
        if (!inventory.isPlanted()) {
            // Pulling the seed or the soil abandons the cycle; replanting starts a fresh one rather
            // than resuming where the last crop left off.
            if (inventory.plantedAt() != 0L)
                MysticalUtil.editCustomData(stack, compound -> compound.putLong(PLANTED_AT_KEY, 0L));
            return;
        }

        long now = world.getGameTime();
        long planted = inventory.plantedAt();

        // A book that has just been planted, or one carried into a world whose clock runs behind
        // the one it was planted in, restarts from now instead of reading as infinitely old.
        if (planted <= 0L || planted > now) {
            inventory.setPlantedAt(now);
            touchClock(stack, now, true);
            return;
        }

        long total = Math.max(1L, ModConfig.BookOfFarmingGrowthTicks);
        if (now - planted < total) {
            touchClock(stack, now, false);
            return;
        }

        // Cheap gate before the expensive part. A book whose outputs are completely full is the
        // normal AFK state, and rollHarvest is a full loot-table evaluation - running it twenty
        // times a second per parked book, only to discard the result, is pure waste.
        if (!inventory.hasAnyOutputRoom()) {
            touchClock(stack, now, false);
            return;
        }

        var roll = rollHarvest(stack, inventory, (ServerLevel) world);
        if (roll.isEmpty()) {
            inventory.setPlantedAt(now);
            touchClock(stack, now, true);
            return;
        }

        // Nothing is produced unless all of it fits. The alternative is either destroying the
        // overflow or leaving a crop half harvested, and neither is acceptable - so a full book
        // simply waits, fully grown, until the player makes room.
        if (!inventory.canAcceptAll(roll)) {
            touchClock(stack, now, false);
            return;
        }

        inventory.deposit(roll);
        inventory.setPlantedAt(now);
        touchClock(stack, now, true);
    }

    /** Stores the observed clock, throttled unless the caller needs it exact right now. */
    private void touchClock(ItemStack stack, long now, boolean force) {
        long last = lastSeenTime(stack);
        if (!force && now - last < CLOCK_INTERVAL && now >= last)
            return;

        MysticalUtil.editCustomData(stack, compound -> compound.putLong(LAST_TICK_KEY, now));
    }

    /**
     * One crop's worth of mature drops, minus a single seed.
     *
     * <p>Holding a seed back is the botany-pot convention: the plant reseeds itself, which is why
     * the seed slot is never emptied. Without it every cycle would hand back the seed it grew from
     * and the book would print seeds forever.
     */
    private List<ItemStack> rollHarvest(ItemStack stack, FarmingBookInventory inventory, ServerLevel world) {
        CropBlock crop = inventory.plantedCrop();
        if (crop == null)
            return List.of();

        BlockState mature = crop.defaultBlockState().setValue(CropBlock.AGE, crop.getMaxAge());
        var drops = Block.getDrops(mature, world, BlockPos.ZERO, null);

        var seedItem = inventory.getItem(FarmingBookInventory.SEED_SLOT).getItem();
        var result = new ArrayList<ItemStack>(drops.size());
        boolean seedWithheld = false;

        for (ItemStack drop : drops) {
            if (drop.isEmpty())
                continue;

            if (!seedWithheld && drop.getItem() == seedItem) {
                drop.shrink(1);
                seedWithheld = true;
                if (drop.isEmpty())
                    continue;
            }

            result.add(drop);
        }

        return result;
    }

    // ---- legacy books ------------------------------------------------------------------------

    /**
     * Converts a book from the bind-a-crop design to the planted one.
     *
     * <p>The old book stored the crop BLOCK it was bound to and kept its produce in a single
     * stacking inventory. The mapping is: the crop's own seed item goes into the seed slot (asked
     * of the block through {@link CropBlockInvoker}, because the item is not derivable from the
     * block id), and whatever the old inventory held moves into the outputs as far as it fits.
     *
     * <p>Anything that does not fit is left exactly where it was, under the old {@code Items} key,
     * which the old inventory still reads - so an over-full legacy book keeps draining the old way
     * instead of losing anything. Soil is not migrated because the old design had none; the book
     * arrives planted but dormant until the player supplies some, which is also the clearest signal
     * that the rules changed.
     */
    private void migrateLegacy(ItemStack stack, Level world) {
        var compound = MysticalUtil.getCustomData(stack);
        if (compound == null || !compound.contains(LEGACY_CROP_KEY))
            return;

        // tryParse, not parse: parse THROWS on a malformed or empty id, and this runs from
        // inventoryTick - so one corrupt book would throw twenty times a second for as long as a
        // player held it, which in practice means the world cannot be joined. A null here skips
        // the seed step but still falls through to the key-clear below, so the book leaves the
        // legacy state instead of retrying the same failure forever. (An id that parses but names
        // nothing is already safe: the registry hands back its default and the instanceof fails.)
        var cropId = Identifier.tryParse(compound.getStringOr(LEGACY_CROP_KEY, ""));
        var block = cropId == null ? null : BuiltInRegistries.BLOCK.getValue(cropId);
        var inventory = new FarmingBookInventory(stack);

        if (block instanceof CropBlock crop && inventory.getItem(FarmingBookInventory.SEED_SLOT).isEmpty()) {
            var seed = ((CropBlockInvoker) crop).invokeGetBaseSeedId();
            if (seed != null)
                inventory.setItem(FarmingBookInventory.SEED_SLOT, new ItemStack(seed.asItem()));
        }

        var legacy = new SingleItemStackingInventory(stack, ModConfig.BookOfFarmingMaxStacks);
        boolean movedAny = false;
        for (int slot = 0; slot < legacy.storedItems.size(); slot++) {
            var item = legacy.storedItems.get(slot);
            if (item.isEmpty())
                continue;

            // Slot by slot, and only what fits. Whatever is left stays in the old inventory, which
            // the old read path still drains, so nothing is destroyed by a book that was fuller
            // than four output slots can hold.
            if (!inventory.canAcceptAll(List.of(item)))
                continue;

            inventory.deposit(List.of(item));
            legacy.storedItems.set(slot, ItemStack.EMPTY);
            movedAny = true;
        }

        if (movedAny)
            legacy.writeNbt();

        // The bound-crop key is what marks a book as legacy, so clearing it is what makes the
        // conversion happen exactly once.
        MysticalUtil.editCustomData(stack, nbt -> {
            nbt.remove(LEGACY_CROP_KEY);
            nbt.putLong(PLANTED_AT_KEY, world.getGameTime());
        });
    }

    // ---- ticking ----------------------------------------------------------------------------

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, EquipmentSlot slot) {
        customBookTick(stack, world, entity);
        super.inventoryTick(stack, world, entity, slot);
    }

    @Override
    public void customBookTick(ItemStack stack, Level world, BlockEntity be) {
        // The Scriptorium is where books WORK; the Library is storage only. This gate named the
        // Library back when Libraries ticked their contents - once that job moved, it matched
        // nothing a book is ever ticked from, so generating books grew nowhere in the world. Keep
        // it explicit rather than "any block entity": letting the Library tick again is exactly
        // what the storage-only rule forbids.
        if (!(be instanceof ScriptoriumBlockEntity))
            return;

        tryGrow(stack, world);
    }

    @Override
    public void customBookTick(ItemStack stack, Level world, Entity entity) {
        if (!(entity instanceof Player))
            return;

        tryGrow(stack, world);
    }

    // ---- display -----------------------------------------------------------------------------

    /**
     * The six planting slots. Deliberately NOT the {@code getInventory} override: that one is the
     * storage-book contract and still hands back the old single-stack inventory, which is what the
     * legacy drain path and the tooltip grid read. The two coexist on a migrated book - old produce
     * in the old inventory, new produce in these slots.
     */
    public FarmingBookInventory farmInventory(ItemStack stack) {
        return new FarmingBookInventory(stack);
    }

    @Override
    public SingleItemStackingInventory getInventory(ItemStack stack) {
        return new SingleItemStackingInventory(stack, ModConfig.BookOfFarmingMaxStacks);
    }

    @Override
    public boolean shouldGlint(ItemStack stack) {
        return new FarmingBookInventory(stack).isPlanted();
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        // Outputs first, then anything a migrated book still has in its old inventory. Both are
        // drainable, so both belong in the grid; soil and seed do not, which is why this reads the
        // outputs through a view rather than handing over the whole farming inventory.
        var outputs = new OutputView(farmInventory(stack));
        var legacy = getInventory(stack);
        if (outputs.isEmpty() && legacy.isEmpty())
            return Optional.empty();

        return Optional.of(BookContentsTooltipData.fromInventories(outputs, legacy));
    }

    /** Read-only window onto just the four output slots, for display code. */
    private record OutputView(FarmingBookInventory backing) implements net.minecraft.world.Container {
        @Override public int getContainerSize() { return FarmingBookInventory.OUTPUT_SLOTS; }
        @Override public boolean isEmpty() {
            for (int i = 0; i < FarmingBookInventory.OUTPUT_SLOTS; i++)
                if (!getItem(i).isEmpty()) return false;
            return true;
        }
        @Override public ItemStack getItem(int slot) {
            return backing.getItem(FarmingBookInventory.FIRST_OUTPUT_SLOT + slot);
        }
        @Override public ItemStack removeItem(int slot, int amount) { return ItemStack.EMPTY; }
        @Override public ItemStack removeItemNoUpdate(int slot) { return ItemStack.EMPTY; }
        @Override public void setItem(int slot, ItemStack value) { }
        @Override public void setChanged() { }
        @Override public boolean stillValid(Player player) { return true; }
        @Override public void clearContent() { }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag type) {
        var inventory = new FarmingBookInventory(stack);

        if (!inventory.isPlanted()) {
            tooltip.accept(Component.translatable("tooltip.mystical_index.farming_book.needs_planting"));
        } else {
            var crop = inventory.plantedCrop();
            if (crop != null) {
                var seed = inventory.getItem(FarmingBookInventory.SEED_SLOT);
                tooltip.accept(Component.translatable("tooltip.mystical_index.farming_book.growing_crop",
                        seed.getHoverName()));
            }

            int percent = Math.round(FarmingGrowthModel.progressOf(stack) * 100.0F);
            tooltip.accept(Component.translatable("tooltip.mystical_index.farming_book.growth", percent));

            // Fully grown with nowhere to put the harvest is a designed state, not a stall: the
            // cycle deliberately refuses to roll unless the whole result fits. Without this line a
            // book parked at 100% looks broken.
            if (percent >= 100 && !inventory.hasAnyOutputRoom())
                tooltip.accept(Component.translatable("tooltip.mystical_index.farming_book.output_full"));
        }

        if (Minecraft.getInstance().hasShiftDown()) {
            tooltip.accept(Component.translatable("tooltip.mystical_index.farming_book_shift0"));
            tooltip.accept(Component.translatable("tooltip.mystical_index.farming_book_shift1"));
            tooltip.accept(Component.translatable("tooltip.mystical_index.farming_book_shift2"));
        } else {
            tooltip.accept(Component.translatable("tooltip.mystical_index.farming_book"));
        }

        super.appendHoverText(stack, context, display, tooltip, type);
    }
}
