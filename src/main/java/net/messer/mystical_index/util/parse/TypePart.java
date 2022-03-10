package net.messer.mystical_index.util.parse;

import net.minecraft.nbt.NbtCompound;

import java.util.regex.Matcher;

public class TypePart extends Part {
    private final String key;
    private final String value;

    public TypePart(String pattern, String key, String value) {
        super(pattern);
        this.key = key;
        this.value = value;
    }

    @Override
    protected void find(NbtCompound compound, Matcher matcher) {
        if (matcher.matches())
            add(compound, key, value);
    }

    @Override
    protected void add(NbtCompound compound, String key, String value) {
        compound.putString(key, value);
    }
}
