package net.messer.mystical_index.util.parse;

import net.minecraft.nbt.NbtCompound;

public class IntPart extends Part {
    public IntPart(String pattern) {
        super(pattern);
    }

    @Override
    protected void add(NbtCompound compound, String key, String value) {
        compound.putInt(key, Integer.parseInt(value));
    }
}
