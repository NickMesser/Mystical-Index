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

        var slotCandidates = gridCandidates(display);

        // Same answer on both passes, so a red button does nothing when clicked.
        if (!canSatisfy(lectern, slotCandidates))
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

    // Counts what the player carries plus what the lectern last told us the network holds, then
    // assigns one item per filled slot against that pool. Assigning greedily and decrementing as
    // we go makes eight gold slots need eight gold, not one.
    private static boolean canSatisfy(MysticalLecternScreenHandler lectern, List<List<ItemVariant>> slotCandidates) {
        Map<ItemVariant, Long> supply = new HashMap<>();

        var player = MinecraftClient.getInstance().player;
        if (player != null) {
            for (var stack : player.getInventory().main) {
                if (!stack.isEmpty())
                    supply.merge(ItemVariant.of(stack), (long) stack.getCount(), Long::sum);
            }
        }

        // Empty for a tick after the screen opens; the next probe repaint picks up the sync.
        for (var entry : lectern.getClientEntries())
            supply.merge(entry.variant(), entry.count(), Long::sum);

        for (var candidates : slotCandidates) {
            if (candidates.isEmpty())
                continue;

            boolean assigned = false;
            for (var variant : candidates) {
                long available = supply.getOrDefault(variant, 0L);
                if (available <= 0)
                    continue;

                supply.put(variant, available - 1);
                assigned = true;
                break;
            }

            if (!assigned)
                return false;
        }

        return true;
    }

    private static List<List<ItemVariant>> gridCandidates(Display display) {
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
            int slot = GRID_WIDTH * (i / inputWidth) + (i % inputWidth);
            if (slot >= LecternNetworking.RECIPE_GRID_SIZE)
                continue;

            var candidates = new ArrayList<ItemVariant>();
            for (var entry : inputs.get(i)) {
                if (candidates.size() >= LecternNetworking.MAX_RECIPE_CANDIDATES)
                    break;
                if (entry.getType() != VanillaEntryTypes.ITEM)
                    continue;

                ItemStack stack = entry.castValue();
                if (stack == null || stack.isEmpty())
                    continue;

                candidates.add(ItemVariant.of(stack));
            }

            slots.set(slot, candidates);
        }

        return slots;
    }
}
