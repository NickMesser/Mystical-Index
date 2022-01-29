package net.messer.config;


import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

import java.util.ArrayList;
import java.util.List;

@Config(name = "mystical_index")
public class MysticalConfig implements ConfigData {

    public MysticalConfig(){
        BookOfStorage.BlockBlacklist.add("minecraft:shulker_box");
    }

    @ConfigEntry.Gui.CollapsibleObject
    public BookOfStorageConfig BookOfStorage = new BookOfStorageConfig();

    @ConfigEntry.Gui.CollapsibleObject
    public BookOfFluidConfig BookOfFluid = new BookOfFluidConfig();

    public static class BookOfStorageConfig {
        @Comment("How many stacks a Book Of Storage can hold. Default: 25")
        public int MaxStacks = 25;

        @Comment("What blocks do you NOT want the player to be able to store?")
        public List<String> BlockBlacklist = new ArrayList<>();
    }

    public static class BookOfFluidConfig{
        @Comment("How many buckets of liquid a Book Of Fluids can hold. Default: 10")
        public int MaxBuckets = 10;
    }
}
