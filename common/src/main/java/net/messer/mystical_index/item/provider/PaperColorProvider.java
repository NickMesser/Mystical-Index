package net.messer.mystical_index.item.provider;

import net.messer.mystical_index.item.ModItems;
import net.messer.util.MysticalUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.ItemStack;

/**
 * Colours an entity paper after the mob it holds.
 *
 * <p>This used to be a client colour provider, but the whole {@code ItemColors} system is gone: item
 * tints are declared in the client item definition and computed by an {@code ItemTintSource}.
 * Registering a custom source is not open to mods either - {@code ItemTintSources.ID_MAPPER} is
 * private, and reaching it would need an access widener on Fabric plus an access transformer on
 * NeoForge. So the colour is written onto the stack instead, and {@code assets/mystical_index/items/
 * entity_paper.json} tints with vanilla's {@code minecraft:dye} source, which simply reads it back.
 *
 * <p>Same choke-point rule as the book glint: the write hangs off the single custom-data path every
 * mutation already goes through, so a paper can never be left showing the previous mob's colour.
 *
 * <p>The colour itself can no longer come from the spawn egg. Egg tints were removed from code in
 * 1.21.4 and from data soon after - vanilla ships a separate texture per egg now, so there is no
 * value left to read. It is derived from the entity id instead, which keeps what the feature was
 * actually for (every mob's paper is a distinct, stable colour) and extends it to modded mobs that
 * never had an egg colour to borrow. Swapping in another rule means changing only {@link #colourOf}.
 */
public class PaperColorProvider {

    private static final int DEFAULT_COLOUR = 0xFFFFFF;

    public static void applyTint(ItemStack stack) {
        if (!stack.is(ModItems.ENTITY_PAPER.get()))
            return;

        var compound = MysticalUtil.getCustomData(stack);
        var entityId = compound == null ? "" : compound.getStringOr("entity", "");

        if (entityId.isBlank())
            stack.remove(DataComponents.DYED_COLOR);
        else
            stack.set(DataComponents.DYED_COLOR, new DyedItemColor(colourOf(entityId)));
    }

    /**
     * Stable, well-spread colour for an entity id. Hashing the id keeps the same mob the same colour
     * across worlds and sessions; the fixed saturation and value keep every result legible against
     * the paper texture rather than letting the hash pick something near-black or washed out.
     */
    private static int colourOf(String entityId) {
        int hash = entityId.hashCode();
        float hue = Math.floorMod(hash, 360) / 360f;
        return java.awt.Color.HSBtoRGB(hue, 0.65f, 0.95f) & 0xFFFFFF;
    }

    public static int defaultColour() {
        return DEFAULT_COLOUR;
    }
}
