package net.messer.mystical_index.compat;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.messer.mystical_index.item.ModItems;
import net.messer.mystical_index.recipe.PistonRecipe;
import net.messer.mystical_index.recipe.PistonRecipeInitializer;
import net.messer.util.MysticalUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PistonCraftingDisplay extends BasicDisplay {

    // Kept short enough to fit inside the 150 wide recipe panel; the category wraps anything that
    // still does not fit rather than letting it clip.
    private static final Component DROP_NOTE = Component.literal("Drop onto an Iron Block");
    private static final Component CHARGE_NOTE = Component.literal("Paper must match the mob");

    @Nullable
    private final Component note;

    public PistonCraftingDisplay(List<EntryIngredient> inputs, List<EntryIngredient> outputs, @Nullable Component note) {
        super(inputs, outputs, Optional.empty());
        this.note = note;
    }

    // Codec form of the same three fields, used by the serializer below.
    private PistonCraftingDisplay(List<EntryIngredient> inputs, List<EntryIngredient> outputs, Optional<Component> note) {
        this(inputs, outputs, note.orElse(null));
    }

    private Optional<Component> noteOptional() {
        return Optional.ofNullable(note);
    }

    /**
     * Display#getSerializer is abstract in this version - every display has to declare how it is
     * written, whether or not it ever travels. These displays are built client-side from the piston
     * recipe list, so nothing depends on the round trip, but the codecs are real rather than a stub
     * so REI's own persistence and syncing behave normally if it decides to use them.
     */
    public static final DisplaySerializer<PistonCraftingDisplay> SERIALIZER = DisplaySerializer.of(
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    EntryIngredient.codec().listOf().fieldOf("inputs")
                            .forGetter(PistonCraftingDisplay::getInputEntries),
                    EntryIngredient.codec().listOf().fieldOf("outputs")
                            .forGetter(PistonCraftingDisplay::getOutputEntries),
                    ComponentSerialization.CODEC.optionalFieldOf("note")
                            .forGetter(PistonCraftingDisplay::noteOptional)
            ).apply(instance, PistonCraftingDisplay::new)),
            StreamCodec.composite(
                    EntryIngredient.streamCodec().apply(ByteBufCodecs.list()),
                    PistonCraftingDisplay::getInputEntries,
                    EntryIngredient.streamCodec().apply(ByteBufCodecs.list()),
                    PistonCraftingDisplay::getOutputEntries,
                    ByteBufCodecs.optional(ComponentSerialization.STREAM_CODEC),
                    PistonCraftingDisplay::noteOptional,
                    PistonCraftingDisplay::new));

    public static final Identifier SERIALIZER_ID =
            Identifier.fromNamespaceAndPath("mystical_index", "piston_crafting");

    @Override
    public DisplaySerializer<? extends me.shedaniel.rei.api.common.display.Display> getSerializer() {
        return SERIALIZER;
    }

    @Nullable
    public Component getNote() {
        return note;
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return PistonCraftingCategory.PISTON_CRAFTING;
    }

    public static List<PistonCraftingDisplay> buildAll() {
        var displays = new ArrayList<PistonCraftingDisplay>();

        // Loaded by a SERVER_DATA reload listener, so this is populated once a world is running.
        for (var recipe : PistonRecipeInitializer.getInstance().getRecipes()) {
            var inputs = new ArrayList<EntryIngredient>();
            for (var entry : recipe.getInputs().values())
                inputs.add(EntryIngredients.of(toStack(entry)));

            var outputs = new ArrayList<EntryIngredient>();
            for (var entry : recipe.getOutputs().values())
                outputs.add(EntryIngredients.of(toStack(entry)));

            if (inputs.isEmpty() || outputs.isEmpty())
                continue;

            displays.add(new PistonCraftingDisplay(inputs, outputs, DROP_NOTE));
        }

        displays.add(bookCharging());
        return displays;
    }

    private static ItemStack toStack(PistonRecipe.ItemEntry entry) {
        var stack = new ItemStack(entry.item, Math.max(1, entry.count));

        var nbt = entry.nbt.orElse(null);
        if (nbt == null)
            return stack;

        MysticalUtil.setCustomData(stack, nbt.copy());
        // Same naming EntityPaper.onCraftByPlayer applies, without needing a World here.
        var entityType = EntityType.byString(nbt.getStringOr("entity", "")).orElse(null);
        if (entityType != null)
            stack.set(DataComponents.CUSTOM_NAME, Component.literal(entityType.getDescription().getString() + " Paper"));

        return stack;
    }

    // The book charging path in PistonEntityHook is not a piston recipe json, so it is documented
    // by hand: paper matching the book's mob is consumed and each one adds a kill.
    private static PistonCraftingDisplay bookCharging() {
        var charged = new ItemStack(ModItems.HOSTILE_BOOK.get());
        charged.set(DataComponents.CUSTOM_NAME, Component.literal("Book of Hostile (+1 kill per Paper)"));

        return new PistonCraftingDisplay(
                List.of(EntryIngredients.of(new ItemStack(ModItems.HOSTILE_BOOK.get())),
                        EntryIngredients.of(new ItemStack(ModItems.ENTITY_PAPER.get()))),
                List.of(EntryIngredients.of(charged)),
                CHARGE_NOTE);
    }
}
