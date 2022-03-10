package net.messer.mystical_index.util.parse;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtString;

public class StringListPart extends Part {
    public StringListPart(String pattern) {
        super(pattern);
    }

    @Override
    protected void add(NbtCompound compound, String key, String value) {
        compound.getList(key, NbtElement.STRING_TYPE).add(NbtString.of(value));
    }
}
