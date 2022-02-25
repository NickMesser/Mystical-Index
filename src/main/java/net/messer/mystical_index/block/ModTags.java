package net.messer.mystical_index.block;

import net.fabricmc.fabric.api.tag.TagFactory;
import net.messer.mystical_index.MysticalIndex;
import net.minecraft.block.Block;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;

public class ModTags {
    public static final Tag<Block> INDEX_INTRACTABLE = TagFactory.BLOCK.create(new Identifier(MysticalIndex.MOD_ID, "index_intractable"));
}
