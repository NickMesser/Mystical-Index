package net.messer.mystical_index.item;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.block.ModBlocks;
import net.messer.mystical_index.item.custom.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

import java.util.function.Supplier;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(MysticalIndex.MOD_ID, RegistryKeys.ITEM);

    // Every item is constructed inside its supplier rather than in the field initialiser. An Item's
    // constructor creates its intrusive registry holder, and NeoForge only allows that while the
    // item registry is unfrozen, so class loading this must not build anything on its own.
    public static final RegistrySupplier<Item> FLUID_BOOK = registerItem("fluid_book",
            () -> new FluidBook(new Item.Settings().maxCount(1)));

    public static final RegistrySupplier<Item> FARMING_BOOK = registerItem("farming_book",
            () -> new FarmingBook(new Item.Settings().maxCount(1)));

    public static final RegistrySupplier<Item> STORAGE_BOOK = registerItem("storage_book",
            () -> new StorageBook(new Item.Settings().maxCount(1)));

    public static final RegistrySupplier<Item> HOLDING_BOOK_TIER1 = registerItem("holding_book_1",
            () -> new TieredStorageBook(new Item.Settings().maxCount(1), 1));

    public static final RegistrySupplier<Item> HOLDING_BOOK_TIER2 = registerItem("holding_book_2",
            () -> new TieredStorageBook(new Item.Settings().maxCount(1), 2));

    public static final RegistrySupplier<Item> HOLDING_BOOK_TIER3 = registerItem("holding_book_3",
            () -> new TieredStorageBook(new Item.Settings().maxCount(1), 3));

    public static final RegistrySupplier<Item> HOLDING_BOOK_TIER4 = registerItem("holding_book_4",
            () -> new TieredStorageBook(new Item.Settings().maxCount(1), 4));

    public static final RegistrySupplier<Item> HUSBANDRY_BOOK = registerItem("husbandry_book",
            () -> new HusbandryBook(new Item.Settings().maxCount(1)));

    public static final RegistrySupplier<Item> HOSTILE_BOOK = registerItem("hostile_book",
            () -> new HostileBook(new Item.Settings().maxCount(1)));

    public static final RegistrySupplier<Item> SATURATION_BOOK = registerItem("saturation_book",
            () -> new SaturationBook(new Item.Settings().maxCount(1)));

    public static final RegistrySupplier<Item> MAGNETISM_BOOK = registerItem("magnetism_book",
            () -> new MagnetismBook(new Item.Settings().maxCount(1)));

    public static final RegistrySupplier<Item> VILLAGER_BOOK = registerItem("villager_book",
            () -> new VillagerBook(new Item.Settings().maxCount(1)));

    public static final RegistrySupplier<Item> BABY_VILLAGER_BOOK = registerItem("baby_villager_book",
            () -> new BabyVillagerBook(new Item.Settings().maxCount(1)));

    public static final RegistrySupplier<Item> EMPTY_VILLAGER_BOOK = registerItem("empty_villager_book",
            () -> new EmptyVillagerBook(new Item.Settings().maxCount(1)));

    public static final RegistrySupplier<Item> ENTITY_PAPER = registerItem("entity_paper",
            () -> new EntityPaper(new Item.Settings().maxCount(64)));

    private static RegistrySupplier<Item> registerItem(String name, Supplier<Item> item){
        return ITEMS.register(name, item);
    }

    public static void registerModItems(){
        MysticalIndex.LOGGER.info("Registering items for " + MysticalIndex.MOD_ID);
        ITEMS.register();
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
        addToItemGroup(ItemGroups.FUNCTIONAL, ModBlocks.LIBRARY_ITEM);
        addToItemGroup(ItemGroups.FUNCTIONAL, ModBlocks.MYSTICAL_LECTERN_ITEM);
        addToItemGroup(ItemGroups.TOOLS, EMPTY_VILLAGER_BOOK);
        addToItemGroup(ItemGroups.TOOLS, BABY_VILLAGER_BOOK);
        addToItemGroup(ItemGroups.TOOLS, FARMING_BOOK);
        addToItemGroup(ItemGroups.TOOLS, ENTITY_PAPER);
    }

    // Takes the supplier, not the item: this runs during mod construction, before the item registry
    // has been filled on NeoForge, so the tab has to be told what to look up later rather than
    // being handed an instance that does not exist yet.
    private static void addToItemGroup(RegistryKey<ItemGroup> group, RegistrySupplier<Item> item) {
        CreativeTabRegistry.append(group, item);
    }
}
