package net.messer.config;

import com.mojang.datafixers.util.Pair;
import net.messer.mystical_index.MysticalIndex;

public class ModConfigs {
    public static SimpleConfig CONFIG;
    private static ModConfigProvider configs;

    public static int BOOK_OF_STORAGE_MAX_STACKS;

    public static void registerConfigs() {
        configs = new ModConfigProvider();
        createConfigs();

        CONFIG = SimpleConfig.of(MysticalIndex.MOD_ID + "_config").provider(configs).request();

        assignConfigs();
    }

    private static void createConfigs() {
        configs.addKeyValuePair(new Pair<>("book_of_storage_max_stacks", 25), "int");
    }

    private static void assignConfigs() {
        BOOK_OF_STORAGE_MAX_STACKS = CONFIG.getOrDefault("book_of_storage_max_stacks", 25);
        System.out.println("All " + configs.getConfigsList().size() + " have been set properly");
    }
}
