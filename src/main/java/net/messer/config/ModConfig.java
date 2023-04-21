package net.messer.config;


import eu.midnightdust.lib.config.MidnightConfig;

import java.util.ArrayList;
import java.util.List;

public class ModConfig extends MidnightConfig {
    @Comment public static Comment BookStorageTitle;
    @Entry public static int StorageBookMaxStacks = 10;
    @Entry public static List<String> StorageBookBlockBlacklist = List.of("minecraft:shulker_box");

    @Comment public static Comment BoogMangetismTitle;
    @Entry public static int MagnetismRange = 5;

    @Comment public static Comment BookOfSaturationTitle;
    @Entry public static int SaturationBookMaxStacks = 1;
    @Entry public static int SaturationBookTimeBetweenFeedings = 10;

    @Comment public static Comment BookOfFluidTitle;
    @Entry public static int FluidBookMaxBuckets = 10;

    @Comment public static Comment BookOfHusbandryTitle;
    @Entry public static int HusbandryBookCooldown = 120;
    @Entry public static int HusbandryBookMaxKills = 100;
    @Entry public static List<String> HusbandryBookBlackList = new ArrayList<>();
    @Comment public static Comment BookOfHostilityTitle;
    @Entry public static int HostileBookCooldown = 120;
    @Entry public static int HostileBookMaxKills = 100;

    @Entry public static List<String> HostileBookBlackList = List.of("minecraft:ender_dragon", "minecraft:wither");

}
