package net.messer.config;


import eu.midnightdust.lib.config.MidnightConfig;

import java.util.List;

public class ModConfig extends MidnightConfig {
    @Comment public static Comment BookStorageTitle;
    @Entry public static int StorageBookMaxStacks = 25;
    @Entry public static List<String> StorageBookBlockBlacklist = List.of("minecraft:shulker_box");

    @Comment public static Comment BoogMangetismTitle;
    @Entry public static int MagnetismRange = 5;

    @Comment public static Comment BookOfSaturationTitle;
    @Entry public static int SaturationBookMaxStacks = 1;
    @Entry public static int SaturationBookTimeBetweenFeedings = 10;

    @Comment public static Comment BookOfFluidTitle;
    @Entry public static int FluidBookMaxBuckets = 10;

}
