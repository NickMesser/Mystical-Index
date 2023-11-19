package net.messer.mystical_index.recipe;

import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import org.spongepowered.asm.mixin.injection.selectors.ElementNode;

import java.util.*;

public class PistonRecipe {
    private Map<Item, ItemEntry> inputs;
    private Map<Item, ItemEntry> outputs;

    public PistonRecipe() {
        this.inputs = new HashMap<>();
        this.outputs = new HashMap<>();
    }

    public void addInput(Item item, int count, String nbt) {
        ItemEntry entry = new ItemEntry(item, count, nbt);
        this.inputs.put(item, entry);
    }

    public void addOutput(Item item, int count, String nbt) {
        ItemEntry entry = new ItemEntry(item, count, nbt);
        outputs.put(item, entry);
    }

    public Map<Item, ItemEntry> getInputs() {
        return inputs;
    }

    public Map<Item, ItemEntry> getOutputs() {
        return outputs;
    }

    public Optional<NbtCompound> getInputNbt(Item item) {
        return inputs.containsKey(item) ? inputs.get(item).nbt : Optional.empty();
    }

    public Optional<NbtCompound> getOutputNbt(Item item) {
        return outputs.containsKey(item) ? outputs.get(item).nbt : Optional.empty();
    }

    public static class ItemEntry {
        public Item item;
        public int count;
        public Optional<NbtCompound> nbt;

        public ItemEntry(Item item, int count, String nbtData) {
            this.item = item;
            this.count = count;
            if (nbtData != null && !nbtData.isEmpty()) {
                try {
                    this.nbt = Optional.of(StringNbtReader.parse(nbtData));
                } catch (Exception e) {
                    // Handle NBT parsing exception
                    this.nbt = Optional.empty();
                }
            } else {
                this.nbt = Optional.empty();
            }
        }
    }
}

