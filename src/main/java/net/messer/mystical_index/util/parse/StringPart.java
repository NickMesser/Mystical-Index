package net.messer.mystical_index.util.parse;

import net.minecraft.nbt.NbtCompound;

public class StringPart extends Part {
    public StringPart(String pattern) {
        super(pattern);
    }

    @Override
    protected void add(NbtCompound compound, String key, String value) {
        compound.putString(key, value);
    }
}
