package net.messer.mystical_index.compat;

import me.shedaniel.rei.api.client.registry.transfer.TransferHandler;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.display.SimpleGridMenuDisplay;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import me.shedaniel.rei.plugin.common.BuiltinPlugin;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.messer.mystical_index.network.LecternNetworking;
import net.messer.mystical_index.screen.MysticalLecternScreenHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LecternTransferHandler implements TransferHandler {

    private static final int GRID_WIDTH = 3;

    @Override
    public Result handle(Context context) {
        if (!(context.getMenu() instanceof MysticalLecternScreenHandler lectern))
            return Result.createNotApplicable();

        // Keyed on the category rather than on a display class, so hand-built crafting displays
        // are picked up alongside REI's own.
        var display = context.getDisplay();
        if (!BuiltinPlugin.CRAFTING.getIdentifier().equals(display.getCategoryIdentifier().getIdentifier()))
            return Result.createNotApplicable();

        // The lectern only has a 3x3 grid. A wider/taller recipe, or one with more than nine inputs,
        // can't be laid out without silently reshaping it or dropping the surplus - which would leave
        // canSatisfy green while the craft filled the wrong slots and produced nothing. Defer instead
        // so REI can offer the recipe elsewhere rather than mis-map it here.
        if (display instanceof SimpleGridMenuDisplay grid
                && (grid.getInputWidth(GRID_WIDTH, GRID_WIDTH) > GRID_WIDTH
                || grid.getInputHeight(GRID_WIDTH, GRID_WIDTH) > GRID_WIDTH))
            return Result.createNotApplicable();
        if (display.getInputEntries().size() > LecternNetworking.RECIPE_GRID_SIZE)
            return Result.createNotApplicable();

        // Built once and shared: gridCandidates reads it to order variants by what is on hand, and
        // canSatisfy copies it for the greedy assignment.
        Map<ItemVariant, Long> supply = buildSupply(lectern);

        var slotCandidates = gridCandidates(display, supply);
        // A required ingredient that yields no item candidate (a fluid input, an all-empty
        // ingredient) can't be sourced from the lectern. gridCandidates flags that by returning null;
        // defer rather than lighting the button green on a slot that will never fill.
        if (slotCandidates == null)
            return Result.createNotApplicable();

        // Same answer on both passes, so a red button does nothing when clicked.
        if (!canSatisfy(supply, slotCandidates))
            return Result.createFailed(Text.literal("Missing items"));

        if (!context.isActuallyCrafting())
            return Result.createSuccessful();

        // REI's own DefaultCategoryHandler switches back to the container screen itself before
        // sending its packet. createSuccessful() leaves isReturningToScreen() false, so without
        // this the grid fills behind the recipe screen and the click looks like it did nothing.
        var containerScreen = context.getContainerScreen();
        if (containerScreen != null)
            context.getMinecraft().setScreen(containerScreen);

        var buf = PacketByteBufs.create();
        buf.writeVarInt(lectern.syncId);
        for (var candidates : slotCandidates) {
            buf.writeVarInt(candidates.size());
            for (var variant : candidates)
                variant.toPacket(buf);
        }

        ClientPlayNetworking.send(LecternNetworking.FILL_RECIPE, buf);
        return Result.createSuccessful();
    }

    // The pool the client believes is reachable for a craft: what the player carries, what is already
    // staged in the 3x3 grid, and what the lectern last told us the network holds. The grid counts
    // because handleFillRecipe hands the current grid back to the player before refilling, so a recipe
    // whose ingredients are already sitting in the grid is craftable and must not read as missing.
    private static Map<ItemVariant, Long> buildSupply(MysticalLecternScreenHandler lectern) {
        Map<ItemVariant, Long> supply = new HashMap<>();

        var player = MinecraftClient.getInstance().player;
        if (player != null) {
            for (var stack : player.getInventory().main) {
                if (!stack.isEmpty())
                    supply.merge(ItemVariant.of(stack), (long) stack.getCount(), Long::sum);
            }
        }

        // Slot 0 is the result; slots 1..9 are the 3x3 crafting input.
        for (int slot = 1; slot <= LecternNetworking.RECIPE_GRID_SIZE; slot++) {
            var stack = lectern.getSlot(slot).getStack();
            if (!stack.isEmpty())
                supply.merge(ItemVariant.of(stack), (long) stack.getCount(), Long::sum);
        }

        // Empty for a tick after the screen opens; the next probe repaint picks up the sync.
        for (var entry : lectern.getClientEntries())
            supply.merge(entry.variant(), entry.count(), Long::sum);

        return supply;
    }

    // Assigns one item per filled slot against a copy of the supply pool. Assigning greedily and
    // decrementing as we go makes eight gold slots need eight gold, not one. Works on a copy so the
    // caller's shared supply map is left intact.
    private static boolean canSatisfy(Map<ItemVariant, Long> supply, List<List<ItemVariant>> slotCandidates) {
        Map<ItemVariant, Long> remaining = new HashMap<>(supply);

        for (var candidates : slotCandidates) {
            if (candidates.isEmpty())
                continue;

            boolean assigned = false;
            for (var variant : candidates) {
                long available = remaining.getOrDefault(variant, 0L);
                if (available <= 0)
                    continue;

                remaining.put(variant, available - 1);
                assigned = true;
                break;
            }

            if (!assigned)
                return false;
        }

        return true;
    }

    // Returns one candidate list per grid slot, or null if a required ingredient can't be satisfied
    // with items (see handle()). Candidates within a slot are ordered by how many the client can see
    // on hand so the affordable variant survives the MAX_RECIPE_CANDIDATES truncation.
    private static List<List<ItemVariant>> gridCandidates(Display display, Map<ItemVariant, Long> supply) {
        var slots = new ArrayList<List<ItemVariant>>(LecternNetworking.RECIPE_GRID_SIZE);
        for (int i = 0; i < LecternNetworking.RECIPE_GRID_SIZE; i++)
            slots.add(List.of());

        // A recipe narrower than the grid has to keep its shape, so input i lands on row
        // i / inputWidth of the 3 wide grid instead of straight in slot i.
        int inputWidth = GRID_WIDTH;
        if (display instanceof SimpleGridMenuDisplay grid)
            inputWidth = Math.max(1, Math.min(GRID_WIDTH, grid.getInputWidth(GRID_WIDTH, GRID_WIDTH)));

        var inputs = display.getInputEntries();
        for (int i = 0; i < inputs.size(); i++) {
            var ingredient = inputs.get(i);

            // Collect the item variants this ingredient accepts, and note whether the recipe actually
            // requires this slot (a non-empty ingredient) even when none of its entries are items.
            boolean required = false;
            var candidates = new ArrayList<ItemVariant>();
            for (var entry : ingredient) {
                if (entry.isEmpty())
                    continue;
                required = true;

                if (entry.getType() != VanillaEntryTypes.ITEM)
                    continue;

                ItemStack stack = entry.castValue();
                if (stack == null || stack.isEmpty())
                    continue;

                var variant = ItemVariant.of(stack);
                if (!candidates.contains(variant))
                    candidates.add(variant);
            }

            // A blank recipe slot constrains nothing; leave it as the empty list seeded above.
            if (!required)
                continue;

            int slot = GRID_WIDTH * (i / inputWidth) + (i % inputWidth);
            // The size guards in handle() keep required ingredients inside the grid; if one still maps
            // out of bounds the recipe doesn't fit, so bail rather than silently drop it.
            if (slot >= LecternNetworking.RECIPE_GRID_SIZE)
                return null;

            // Required but no item can supply it (fluid ingredient, all-empty). Not craftable here.
            if (candidates.isEmpty())
                return null;

            // The wire format and the server both take at most MAX_RECIPE_CANDIDATES variants per
            // slot, but a tag like #planks (11) or #logs has more, and they arrive in tag order rather
            // than owned order. Sort by client-visible availability first so an affordable variant is
            // never truncated away in favour of one the player doesn't have.
            if (candidates.size() > 1)
                candidates.sort(Comparator.comparingLong(
                        (ItemVariant variant) -> supply.getOrDefault(variant, 0L)).reversed());
            if (candidates.size() > LecternNetworking.MAX_RECIPE_CANDIDATES)
                candidates.subList(LecternNetworking.MAX_RECIPE_CANDIDATES, candidates.size()).clear();

            slots.set(slot, candidates);
        }

        return slots;
    }
}
