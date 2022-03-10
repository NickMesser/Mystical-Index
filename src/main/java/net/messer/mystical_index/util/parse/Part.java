package net.messer.mystical_index.util.parse;

import net.minecraft.nbt.NbtCompound;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Part {
    private final Pattern pattern;
    private final ArrayList<String[]> groups = new ArrayList<>();
    private boolean required = false;

    public Part(String pattern) {
        this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
    }

    public Part required() {
        required = true;
        return this;
    }

    public Part map(String groupName, String tag) {
        groups.add(new String[]{groupName, tag});
        return this;
    }

    public void match(NbtCompound compound, String string) throws IllegalArgumentException {
        Matcher matcher = pattern.matcher(string);

        if (!matcher.matches() && isRequired())
            throw new IllegalArgumentException("Part is required but string does not contain match");

        find(compound, matcher);
    }

    protected void find(NbtCompound compound, Matcher matcher) {
        while (matcher.find()) {
            for (String[] group : groups) {
                add(compound, group[1], matcher.group(group[0]));
            }
        }
    }

    private boolean isRequired() {
        return required;
    }

    protected abstract void add(NbtCompound compound, String key, String value);
}
