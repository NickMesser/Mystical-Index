package net.messer.mystical_index.block;

import net.messer.mystical_index.MysticalIndex;
import net.minecraft.block.Block;
import net.minecraft.tag.TagKey;
import net.minecraft.util.registry.Registry;

public class ModTags {
    //public static final Tag<Block> INDEX_INTRACTABLE = TagFactory.BLOCK.create(new Identifier(MysticalIndex.MOD_ID, "index_intractable"));
    public static final TagKey<Block> INDEX_INTRACTABLE = TagKey.of(Registry.BLOCK_KEY, MysticalIndex.id("index_intractable"));
}
