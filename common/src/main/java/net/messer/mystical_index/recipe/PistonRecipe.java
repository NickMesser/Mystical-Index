package net.messer.mystical_index.recipe;

import net.minecraft.world.item.Item;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;

import java.util.*;

public class PistonRecipe {
    private Map<Item, ItemEntry> inputs;
    private Map<Item, ItemEntry> outputs;

    public PistonRecipe() {
        this.inputs = new LinkedHashMap<>();
        this.outputs = new LinkedHashMap<>();
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

    public static class ItemEntry {
        public Item item;
        public int count;
        public Optional<CompoundTag> nbt;

        public ItemEntry(Item item, int count, String nbtData) {
            this.item = item;
            this.count = count;
            if (nbtData != null && !nbtData.isEmpty()) {
                try {
                    this.nbt = Optional.of(TagParser.parseCompoundFully(nbtData));
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

