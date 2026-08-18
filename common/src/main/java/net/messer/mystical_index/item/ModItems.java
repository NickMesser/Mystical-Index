package net.messer.mystical_index.item;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.block.ModBlocks;
import net.messer.mystical_index.item.custom.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

import java.util.function.Function;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(MysticalIndex.MOD_ID, Registries.ITEM);

    // Every item is constructed inside its supplier rather than in the field initialiser. An Item's
    // constructor creates its intrusive registry holder, and NeoForge only allows that while the
    // item registry is unfrozen, so class loading this must not build anything on its own.
    public static final RegistrySupplier<Item> FLUID_BOOK = registerItem("fluid_book",
            props -> new FluidBook(props.stacksTo(1)));

    public static final RegistrySupplier<Item> FARMING_BOOK = registerItem("farming_book",
            props -> new FarmingBook(props.stacksTo(1)));

    public static final RegistrySupplier<Item> STORAGE_BOOK = registerItem("storage_book",
            props -> new StorageBook(props.stacksTo(1)));

    public static final RegistrySupplier<Item> HOLDING_BOOK_TIER1 = registerItem("holding_book_1",
            props -> new TieredStorageBook(props.stacksTo(1), 1));

    public static final RegistrySupplier<Item> HOLDING_BOOK_TIER2 = registerItem("holding_book_2",
            props -> new TieredStorageBook(props.stacksTo(1), 2));

    public static final RegistrySupplier<Item> HOLDING_BOOK_TIER3 = registerItem("holding_book_3",
            props -> new TieredStorageBook(props.stacksTo(1), 3));

    public static final RegistrySupplier<Item> HOLDING_BOOK_TIER4 = registerItem("holding_book_4",
            props -> new TieredStorageBook(props.stacksTo(1), 4));

    public static final RegistrySupplier<Item> HUSBANDRY_BOOK = registerItem("husbandry_book",
            props -> new HusbandryBook(props.stacksTo(1)));

    public static final RegistrySupplier<Item> HOSTILE_BOOK = registerItem("hostile_book",
            props -> new HostileBook(props.stacksTo(1)));

    public static final RegistrySupplier<Item> SATURATION_BOOK = registerItem("saturation_book",
            props -> new SaturationBook(props.stacksTo(1)));

    public static final RegistrySupplier<Item> TRANSPORT_BOOK = registerItem("transport_book",
            settings -> new net.messer.mystical_index.item.custom.TransportBook(settings.stacksTo(1)));

    public static final RegistrySupplier<Item> EXPERIENCE_BOOK = registerItem("experience_book",
            settings -> new net.messer.mystical_index.item.custom.ExperienceBook(settings.stacksTo(1)));

    public static final RegistrySupplier<Item> BOOK_SLING = registerItem("book_sling",
            settings -> new net.messer.mystical_index.item.custom.BookSling(settings.stacksTo(1)));

    public static final RegistrySupplier<Item> MAGNETISM_BOOK = registerItem("magnetism_book",
            props -> new MagnetismBook(props.stacksTo(1)));

    public static final RegistrySupplier<Item> VILLAGER_BOOK = registerItem("villager_book",
            props -> new VillagerBook(props.stacksTo(1)));

    public static final RegistrySupplier<Item> BABY_VILLAGER_BOOK = registerItem("baby_villager_book",
            props -> new BabyVillagerBook(props.stacksTo(1)));

    public static final RegistrySupplier<Item> EMPTY_VILLAGER_BOOK = registerItem("empty_villager_book",
            props -> new EmptyVillagerBook(props.stacksTo(1)));

    public static final RegistrySupplier<Item> ENTITY_PAPER = registerItem("entity_paper",
            props -> new EntityPaper(props.stacksTo(64)));

    /**
     * Items now have to be told their own registry id at construction, so the factory is handed a
     * {@link Item.Properties} that already carries it. Deriving the key from the same name the
     * entry is registered under is what keeps the two from ever drifting apart.
     */
    private static RegistrySupplier<Item> registerItem(String name, Function<Item.Properties, Item> factory){
        var key = ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(MysticalIndex.MOD_ID, name));
        return ITEMS.register(name, () -> factory.apply(new Item.Properties().setId(key)));
    }

    public static void registerModItems(){
        MysticalIndex.LOGGER.info("Registering items for " + MysticalIndex.MOD_ID);
        ITEMS.register();
        addItemsToItemGroup();
    }

    public static void addItemsToItemGroup(){
        addToItemGroup(CreativeModeTabs.TOOLS_AND_UTILITIES, FLUID_BOOK);
        addToItemGroup(CreativeModeTabs.TOOLS_AND_UTILITIES, STORAGE_BOOK);
        addToItemGroup(CreativeModeTabs.TOOLS_AND_UTILITIES, HOLDING_BOOK_TIER1);
        addToItemGroup(CreativeModeTabs.TOOLS_AND_UTILITIES, HOLDING_BOOK_TIER2);
        addToItemGroup(CreativeModeTabs.TOOLS_AND_UTILITIES, HOLDING_BOOK_TIER3);
        addToItemGroup(CreativeModeTabs.TOOLS_AND_UTILITIES, HOLDING_BOOK_TIER4);
        addToItemGroup(CreativeModeTabs.TOOLS_AND_UTILITIES, HUSBANDRY_BOOK);
        addToItemGroup(CreativeModeTabs.TOOLS_AND_UTILITIES, HOSTILE_BOOK);
        addToItemGroup(CreativeModeTabs.TOOLS_AND_UTILITIES, SATURATION_BOOK);
        addToItemGroup(CreativeModeTabs.TOOLS_AND_UTILITIES, TRANSPORT_BOOK);
        addToItemGroup(CreativeModeTabs.TOOLS_AND_UTILITIES, EXPERIENCE_BOOK);
        addToItemGroup(CreativeModeTabs.TOOLS_AND_UTILITIES, BOOK_SLING);
        addToItemGroup(CreativeModeTabs.TOOLS_AND_UTILITIES, MAGNETISM_BOOK);
        addToItemGroup(CreativeModeTabs.FUNCTIONAL_BLOCKS, ModBlocks.LIBRARY_ITEM);
        addToItemGroup(CreativeModeTabs.FUNCTIONAL_BLOCKS, ModBlocks.MYSTICAL_LECTERN_ITEM);
        addToItemGroup(CreativeModeTabs.TOOLS_AND_UTILITIES, EMPTY_VILLAGER_BOOK);
        addToItemGroup(CreativeModeTabs.TOOLS_AND_UTILITIES, BABY_VILLAGER_BOOK);
        addToItemGroup(CreativeModeTabs.TOOLS_AND_UTILITIES, FARMING_BOOK);
        addToItemGroup(CreativeModeTabs.TOOLS_AND_UTILITIES, ENTITY_PAPER);
    }

    // Takes the supplier, not the item: this runs during mod construction, before the item registry
    // has been filled on NeoForge, so the tab has to be told what to look up later rather than
    // being handed an instance that does not exist yet.
    private static void addToItemGroup(ResourceKey<CreativeModeTab> group, RegistrySupplier<Item> item) {
        CreativeTabRegistry.append(group, item);
    }
}
