package net.messer.mystical_index.util.parse;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BookParser {
    private final ArrayList<Part> parts = new ArrayList<>();

    public BookParser(Part... parts) {
        this.parts.addAll(List.of(parts));
    }

    public BookParser add(Part part) {
        parts.add(part);
        return this;
    }

    public NbtCompound parse(ItemStack book) {
        return parse(
                book.getOrCreateNbt().getList("pages", NbtElement.STRING_TYPE)
                    .stream()
                    .map((NbtElement::asString))
                    .collect(Collectors.joining("\\n"))
                    .replace("\\n", " ")
        );
    }

    public NbtCompound parse(String string) {
        NbtCompound nbt = new NbtCompound();

        try {
            for (Part part : parts) {
                part.match(nbt, string);
            }
        } catch (IllegalArgumentException e) {
            return null;
        }

        return nbt;
    }
}
