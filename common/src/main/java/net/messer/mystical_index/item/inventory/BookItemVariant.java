package net.messer.mystical_index.item.inventory;

import net.minecraft.component.ComponentChanges;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.Objects;

/**
 * An immutable, count-less item stack: an item plus its component changes.
 *
 * <p>This is the loader-neutral stand-in for Fabric's {@code ItemVariant}, which is the key the
 * whole library network is built on (aggregate totals, extract/insert lookups, the lectern
 * payloads and the REI transfer handler). Fabric's version lives in fabric-api, so the shared code
 * cannot reach it; the semantics here are a deliberate one-for-one copy of it so nothing about
 * book or lectern behaviour shifts:
 *
 * <ul>
 *   <li>equality is item identity plus {@link ComponentChanges#equals}, and the hash is cached
 *       because these are used as {@code HashMap} keys on every network aggregate;</li>
 *   <li>the packet codec writes the same two fields in the same order as Fabric's
 *       {@code ItemVariant.PACKET_CODEC} (networked item entry, then component changes);</li>
 *   <li>a blank variant is air, and {@link #toStack(int)} on one yields {@link ItemStack#EMPTY}
 *       rather than a stack of air.</li>
 * </ul>
 */
public final class BookItemVariant {

    public static final PacketCodec<RegistryByteBuf, BookItemVariant> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.registryEntry(RegistryKeys.ITEM), BookItemVariant::getRegistryEntry,
            ComponentChanges.PACKET_CODEC, BookItemVariant::getComponents,
            BookItemVariant::of);

    private static final BookItemVariant BLANK = new BookItemVariant(Items.AIR, ComponentChanges.EMPTY);

    private final Item item;
    private final ComponentChanges components;
    // Component maps can be deep, and these are hashed on every aggregate/lookup, so the hash is
    // computed once at construction exactly as Fabric's variant does.
    private final int hashCode;

    private BookItemVariant(Item item, ComponentChanges components) {
        this.item = item;
        this.components = components;
        this.hashCode = Objects.hash(item, components);
    }

    public static BookItemVariant blank() {
        return BLANK;
    }

    public static BookItemVariant of(Item item, ComponentChanges components) {
        Objects.requireNonNull(item, "Item may not be null.");
        Objects.requireNonNull(components, "Components may not be null.");

        if (item == Items.AIR)
            return BLANK;

        return new BookItemVariant(item, components);
    }

    public static BookItemVariant of(RegistryEntry<Item> item, ComponentChanges components) {
        return of(item.value(), components);
    }

    public static BookItemVariant of(ItemStack stack) {
        return of(stack.getItem(), stack.getComponentChanges());
    }

    public Item getItem() {
        return item;
    }

    public RegistryEntry<Item> getRegistryEntry() {
        return item.getRegistryEntry();
    }

    public ComponentChanges getComponents() {
        return components;
    }

    public boolean isBlank() {
        return item == Items.AIR;
    }

    public boolean isOf(Item item) {
        return this.item == item;
    }

    /**
     * True when the item and component changes of this variant match those of the passed stack,
     * ignoring its count.
     */
    public boolean matches(ItemStack stack) {
        return item == stack.getItem() && Objects.equals(stack.getComponentChanges(), components);
    }

    public ItemStack toStack() {
        return toStack(1);
    }

    /**
     * Builds a stack of the requested count. The count is not clamped to the item's maximum, which
     * matches the callers that deliberately hand out oversized stacks.
     */
    public ItemStack toStack(int count) {
        if (isBlank())
            return ItemStack.EMPTY;

        return new ItemStack(getRegistryEntry(), count, components);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof BookItemVariant other))
            return false;

        return hashCode == other.hashCode && item == other.item && components.equals(other.components);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return "BookItemVariant{item=" + item + ", components=" + components + '}';
    }
}
