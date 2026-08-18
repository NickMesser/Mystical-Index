package net.messer.util;

import net.messer.mystical_index.item.custom.BookSling;
import net.messer.mystical_index.item.custom.base_books.BaseStorageBook;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.messer.mystical_index.item.inventory.BookSlingInventory;
import net.messer.mystical_index.item.provider.FarmingGrowthModel;
import net.messer.mystical_index.item.provider.PaperColorProvider;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MysticalUtil {

    // Item stacks nested inside a book now serialize through a registry lookup, and the inventory
    // classes are built from places that have no world in reach (glint checks, tooltip data). The
    // side that owns one installs it here; the static registries stand in until then so a read
    // before any world exists degrades instead of throwing.
    private static final HolderLookup.Provider FALLBACK_LOOKUP = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    private static HolderLookup.Provider registryLookup;

    public static void setRegistryLookup(HolderLookup.Provider lookup) {
        if (lookup != null)
            registryLookup = lookup;
    }

    public static HolderLookup.Provider registryLookup() {
        return registryLookup != null ? registryLookup : FALLBACK_LOOKUP;
    }

    // appendTooltip lost its World parameter in 1.21. The client entrypoint installs this so the
    // books that show a countdown can still read the world time, without dragging a client-only
    // class into anything a dedicated server loads. Null outside a world, exactly like the old
    // nullable parameter.
    private static Supplier<Level> tooltipWorld = () -> null;

    public static void setTooltipWorldSupplier(Supplier<Level> supplier) {
        tooltipWorld = supplier;
    }

    public static Level tooltipWorld() {
        return tooltipWorld.get();
    }

    // A stack's loose NBT lives in the custom_data component now. Components are immutable, so
    // every read hands back a copy and a mutation only lands once it is written back.
    public static CompoundTag getCustomData(ItemStack stack) {
        var component = stack.get(DataComponents.CUSTOM_DATA);
        return component == null ? null : component.copyTag();
    }

    // Mirrors the old getOrCreateNbt(): the compound is attached to the stack even when nothing is
    // written into it, which is what several books read back as "this book has been used".
    public static CompoundTag getOrCreateCustomData(ItemStack stack) {
        var compound = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        setCustomData(stack, compound);
        return compound;
    }

    public static CompoundTag copyCustomData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    // Every book's data write funnels through here, so this is where the glint component is
    // refreshed. Item.hasGlint is gone in 26.x - the glint is data now, computed on change
    // instead of on render - and driving it from the single write path is what guarantees it
    // can never be left stale by a mutation path nobody remembered to update.
    private static void applyGlint(ItemStack stack) {
        if (!(stack.getItem() instanceof GlintingBook book))
            return;

        if (book.shouldGlint(stack))
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        else
            stack.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
    }

    public static void setCustomData(ItemStack stack, CompoundTag compound) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(compound));
        applyGlint(stack);
        PaperColorProvider.applyTint(stack);
        FarmingGrowthModel.applyStage(stack);
    }

    /**
     * Serializes an entity the way the books store it: WITH its type id.
     *
     * <p>The distinction matters and is easy to get wrong. {@code saveWithoutId} writes only the
     * state - position, motion, and the entity's own fields - and deliberately omits the {@code id}
     * that says WHAT was saved. Everything that reads these compounds back goes through
     * {@code EntityType.loadEntityRecursive}, which needs that id to know what to build: without it
     * the load simply returns null, the game logs "Skipping Entity with id [invalid]", and every
     * caller silently takes its "nothing stored" branch even though the data is right there.
     *
     * <p>Returns null when the entity refuses to be saved at all (no encode id, or already removed
     * for a reason that does not persist), so callers can tell that apart from a successful save.
     */
    @Nullable
    public static CompoundTag saveEntityWithId(Entity entity) {
        var output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        // saveAsPassenger rather than save: both write the id, but save() refuses outright for an
        // entity that is currently riding something, which would quietly drop a captured villager
        // that happened to be in a boat or minecart.
        if (!entity.saveAsPassenger(output))
            return null;

        return output.buildResult();
    }


    /**
     * The id string to store for an entity's loot table.
     *
     * <p>{@code getDefaultLootTable()} and {@code Entity#getLootTable()} both return
     * {@code Optional<ResourceKey<LootTable>>} in this version. Calling toString on that Optional
     * yields {@code Optional[ResourceKey[minecraft:loot_table / minecraft:entities/sheep]]}, which
     * is a debug rendering, not an id - and storing it poisons the book, because the read side
     * parses it every tick and Identifier.parse throws on the brackets and spaces. Returns "" when
     * the entity has no loot table at all, which callers treat as "do not bind".
     */
    public static String lootTableIdString(java.util.Optional<ResourceKey<LootTable>> key) {
        return key.map(k -> k.identifier().toString()).orElse("");
    }

    /**
     * Parses a stored id, repairing the mangled form written before that bug was found.
     *
     * <p>An affected book stores {@code Optional[ResourceKey[minecraft:loot_table /
     * minecraft:entities/sheep]]} - a debug rendering rather than an id. The real id is still in
     * there, after the " / " and before the closing brackets, so it is recovered rather than thrown
     * away and the player never has to know. Done with plain string surgery, not a regex: the shape
     * is fixed and a literal search is easier to read than the escaping would be.
     *
     * <p>Returns null only when nothing usable can be recovered; callers clear the binding then,
     * rather than retrying a parse that will never succeed.
     */
    public static Identifier parseStoredId(String stored) {
        if (stored == null || stored.isEmpty())
            return null;

        var direct = Identifier.tryParse(stored);
        if (direct != null)
            return direct;

        int separator = stored.indexOf(" / ");
        if (separator < 0)
            return null;

        var tail = stored.substring(separator + 3);
        int end = tail.indexOf(']');
        if (end >= 0)
            tail = tail.substring(0, end);

        return Identifier.tryParse(tail.trim());
    }

    /** Receives one effect-bearing book stack. See {@link #forEachEffectiveBook}. */
    public interface BookVisitor {
        void accept(ItemStack bookStack);
    }

    /**
     * The effect books inside one Book Sling, with write-back.
     *
     * <p><b>Flush semantics.</b> A contained book that mutates itself replaces its own
     * CUSTOM_DATA component. Each slot component is recorded before the visit and compared after,
     * writing back only slots whose identity actually changed - at most one write per changed slot
     * per call, through BookSlingInventory#setSlotAndFlush, itself a per-key edit through the
     * shared path. A pass where nothing inside changed costs no writes at all.
     */
    public static void forEachBookInSling(ItemStack slingStack, BookVisitor visitor) {
        if (!(slingStack.getItem() instanceof BookSling sling))
            return;

        var slots = sling.getInventory(slingStack);
        for (int slot = 0; slot < BookSlingInventory.SIZE; slot++) {
            var contained = slots.getItem(slot);
            if (contained.isEmpty())
                continue;

            var before = contained.get(DataComponents.CUSTOM_DATA);
            visitor.accept(contained);

            if (contained.get(DataComponents.CUSTOM_DATA) != before)
                slots.setSlotAndFlush(slot, contained);
        }
    }

    /**
     * The effect books reachable through ONE stack: the stack itself if it is a book, or the
     * contents if it is a sling. Never both - a sling has no effect of its own.
     *
     * <p>For call sites that already decided WHICH stack matters (the kill counter looks only at
     * the offhand) and just need to see through a sling without widening that rule.
     */
    public static void forEachBookIn(ItemStack stack, BookVisitor visitor) {
        if (stack.isEmpty())
            return;

        if (stack.getItem() instanceof BookSling) {
            forEachBookInSling(stack, visitor);
            return;
        }

        visitor.accept(stack);
    }

    /**
     * Every effect book in the player inventory, loose or slung, each exactly once.
     *
     * <p>For call sites that already scan the whole inventory themselves - it replaces that scan
     * rather than adding to it, which is what keeps the count at one per book.
     *
     * <p>NOT for the tick path: vanilla already ticks every loose stack, so walking the inventory
     * again there would tick loose books twice. The sling drives its own contents instead.
     */
    public static void forEachEffectiveBook(Player player, BookVisitor visitor) {
        var inventory = player.getInventory().getNonEquipmentItems();

        for (int i = 0; i < inventory.size(); i++) {
            var stack = inventory.get(i);
            if (stack.isEmpty())
                continue;

            if (stack.getItem() instanceof BookSling) {
                forEachBookInSling(stack, visitor);
                continue;
            }

            visitor.accept(stack);
        }
    }

    /**
     * Whether a stack is an effect book something else may carry.
     *
     * <p>{@link SelfUpdatingBook} is exactly the set of books whose effect runs passively while
     * carried. The second clause is what makes nesting structurally impossible: a Book Sling is
     * itself a self-updating book, so without it a sling could hold a sling - or a Scriptorium
     * could hold a sling that holds books whose effects would then run at a block that never
     * intended to drive them.
     *
     * <p>One expression, used by every container that holds books, so the two enforcement sites
     * cannot drift apart.
     */
    /**
     * Runs the block-side tick for every book held in a container.
     *
     * <p>Originally the Library's tick, now the Scriptorium's only - a Library is storage and does
     * not run books at all. The {@code BaseStorageBook} gate is
     * load-bearing: {@code customBookTick(ItemStack, Level, BlockEntity)} is declared there, so the
     * books outside that hierarchy - magnetism, saturation, fluid, experience - have no block-side
     * entry point at all and are inert here by construction rather than by an exclusion list.
     *
     * <p>setChanged fires unconditionally after the loop, exactly as the Library always did:
     * contained books rewrite their own stacks in place, so the container cannot tell whether
     * anything changed and marking dirty every tick is what guarantees the writes reach disk.
     */
    public static void tickContainedBooks(SimpleContainer container, Level world, BlockEntity blockEntity) {
        for (var book : container.getItems()) {
            if (book.getItem() instanceof BaseStorageBook storageBook) {
                storageBook.customBookTick(book, world, blockEntity);
            }
            // Books outside BaseStorageBook (magnetism, saturation, fluid, experience) have no
            // block-side entry point here and never will: this helper is shared with the Library,
            // whose behaviour is frozen. The Scriptorium's world behaviours are a SECOND pass in
            // ScriptoriumBlockEntity.tick, so a magnet parked in a Library stays inert.
        }
        container.setChanged();
    }

    public static boolean isEffectBook(ItemStack stack) {
        return stack.getItem() instanceof SelfUpdatingBook
                && !(stack.getItem() instanceof BookSling);
    }

    public static boolean hasCustomData(ItemStack stack) {
        return stack.has(DataComponents.CUSTOM_DATA);
    }

    // Read, mutate, store: the only safe shape for editing a component in place.
    public static void editCustomData(ItemStack stack, Consumer<CompoundTag> edit) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, edit);
        applyGlint(stack);
        PaperColorProvider.applyTint(stack);
        FarmingGrowthModel.applyStage(stack);
    }

    public static List<ItemStack> generateEntityLoot(Player player, Entity entity, Identifier storedEntityLootTable){
        Level world = player.level();
        var fakeSword = new ItemStack(Items.DIAMOND_SWORD);
        player.setItemInHand(InteractionHand.MAIN_HAND, fakeSword);
        var source = player.damageSources().playerAttack(player);

        LootParams context = new LootParams.Builder((ServerLevel) world)
                .withParameter(LootContextParams.THIS_ENTITY, entity)
                .withParameter(LootContextParams.ORIGIN, player.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, source)
                .withParameter(LootContextParams.ATTACKING_ENTITY, player)
                .withParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, player)
                .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player)
                .create(LootContextParamSets.ENTITY);

        fakeSword.shrink(1);
        // Books persist the loot table as a plain id string, so the key is rebuilt from it here
        // rather than changing what is written to disk.
        ResourceKey<LootTable> lootTableKey = ResourceKey.create(Registries.LOOT_TABLE, storedEntityLootTable);
        LootTable lootTable = world.getServer().reloadableRegistries().getLootTable(lootTableKey);
        return lootTable.getRandomItems(context);
    }
}
