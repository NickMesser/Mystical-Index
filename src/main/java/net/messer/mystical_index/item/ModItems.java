package net.messer.mystical_index.item;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.block.ModBlocks;
import net.messer.mystical_index.item.custom.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

public class ModItems {

    public static final Item FLUID_BOOK = registerItem("fluid_book",
            new FluidBook(new Item.Settings().maxCount(1)));

    public static final Item FARMING_BOOK = registerItem("farming_book",
            new FarmingBook(new Item.Settings().maxCount(1)));

    public static final Item STORAGE_BOOK = registerItem("storage_book",
            new StorageBook(new Item.Settings().maxCount(1)));

    public static final Item HOLDING_BOOK_TIER1 = registerItem("holding_book_1",
            new TieredStorageBook(new Item.Settings().maxCount(1), 1));

    public static final Item HOLDING_BOOK_TIER2 = registerItem("holding_book_2",
            new TieredStorageBook(new Item.Settings().maxCount(1), 2));

    public static final Item HOLDING_BOOK_TIER3 = registerItem("holding_book_3",
            new TieredStorageBook(new Item.Settings().maxCount(1), 3));

    public static final Item HOLDING_BOOK_TIER4 = registerItem("holding_book_4",
            new TieredStorageBook(new Item.Settings().maxCount(1), 4));

    public static final Item HUSBANDRY_BOOK = registerItem("husbandry_book",
            new HusbandryBook(new Item.Settings().maxCount(1)));

    public static final Item HOSTILE_BOOK = registerItem("hostile_book",
            new HostileBook(new Item.Settings().maxCount(1)));

    public static final Item SATURATION_BOOK = registerItem("saturation_book",
            new SaturationBook(new Item.Settings().maxCount(1)));

    public static final Item MAGNETISM_BOOK = registerItem("magnetism_book",
            new MagnetismBook(new Item.Settings().maxCount(1)));

    public static final Item VILLAGER_BOOK = registerItem("villager_book",
            new VillagerBook(new Item.Settings().maxCount(1)));

    public static final Item BABY_VILLAGER_BOOK = registerItem("baby_villager_book",
            new BabyVillagerBook(new Item.Settings().maxCount(1)));

    public static final Item EMPTY_VILLAGER_BOOK = registerItem("empty_villager_book",
            new EmptyVillagerBook(new Item.Settings().maxCount(1)));

    public static final Item ENTITY_PAPER = registerItem("entity_paper",
            new EntityPaper(new Item.Settings().maxCount(64)));

    private static Item registerItem(String name, Item item){
        return Registry.register(Registries.ITEM, Identifier.of(MysticalIndex.MOD_ID, name), item);
    }

    public static void registerModItems(){
        MysticalIndex.LOGGER.info("Registering items for " + MysticalIndex.MOD_ID);
        addItemsToItemGroup();
    }

    public static void addItemsToItemGroup(){
        addToItemGroup(ItemGroups.TOOLS, FLUID_BOOK);
        addToItemGroup(ItemGroups.TOOLS, STORAGE_BOOK);
        addToItemGroup(ItemGroups.TOOLS, HOLDING_BOOK_TIER1);
        addToItemGroup(ItemGroups.TOOLS, HOLDING_BOOK_TIER2);
        addToItemGroup(ItemGroups.TOOLS, HOLDING_BOOK_TIER3);
        addToItemGroup(ItemGroups.TOOLS, HOLDING_BOOK_TIER4);
        addToItemGroup(ItemGroups.TOOLS, HUSBANDRY_BOOK);
        addToItemGroup(ItemGroups.TOOLS, HOSTILE_BOOK);
        addToItemGroup(ItemGroups.TOOLS, SATURATION_BOOK);
        addToItemGroup(ItemGroups.TOOLS, MAGNETISM_BOOK);
        addToItemGroup(ItemGroups.FUNCTIONAL, Item.fromBlock(ModBlocks.LIBRARY));
        addToItemGroup(ItemGroups.FUNCTIONAL, Item.fromBlock(ModBlocks.MYSTICAL_LECTERN));
        addToItemGroup(ItemGroups.TOOLS, EMPTY_VILLAGER_BOOK);
        addToItemGroup(ItemGroups.TOOLS, BABY_VILLAGER_BOOK);
        addToItemGroup(ItemGroups.TOOLS, FARMING_BOOK);
        addToItemGroup(ItemGroups.TOOLS, ENTITY_PAPER);
    }

    private static void addToItemGroup(RegistryKey<ItemGroup> group, Item item) {
        ItemGroupEvents.modifyEntriesEvent(group).register(entries -> entries.add(item));
    }
}
