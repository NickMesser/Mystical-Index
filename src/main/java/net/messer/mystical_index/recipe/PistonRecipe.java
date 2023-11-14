package net.messer.mystical_index.recipe;

import net.minecraft.item.Item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PistonRecipe {
    private Map<Item, Integer> inputs;
    private Map<Item, Integer> outputs;

    public PistonRecipe() {
        this.inputs = new HashMap<>();
        this.outputs = new HashMap<>();
    }

    public void addInput(Item item, int count) {
        this.inputs.put(item, count);
    }

    public void addOutput(Item item, int count) {
        outputs.put(item, count);
    }

    public Map<Item, Integer> getInputs() {
        return inputs;
    }

    public Map<Item, Integer> getOutputs() {
        return outputs;
    }
}

