package net.messer.mystical_index.block;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.block.custom.LibraryInventoryBlock;
import net.messer.mystical_index.block.custom.MysticalLecternBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(MysticalIndex.MOD_ID, RegistryKeys.BLOCK);

    // The block items go through their own deferred register rather than ModItems.ITEMS, so the two
    // classes can be initialised in either order without one having to be loaded before the other
    // finishes collecting its entries.
    public static final DeferredRegister<Item> BLOCK_ITEMS =
            DeferredRegister.create(MysticalIndex.MOD_ID, RegistryKeys.ITEM);

    // Everything is built inside its supplier, never in the field initialiser. Constructing a Block
    // or an Item creates its intrusive registry holder, which NeoForge only permits while the
    // matching registry is unfrozen, so the construction itself has to be deferred and not just the
    // registration.
    public static final RegistrySupplier<Block> LIBRARY = BLOCKS.register("library",
            () -> new LibraryInventoryBlock(AbstractBlock.Settings.create().strength(1.5f)));

    public static final RegistrySupplier<Block> MYSTICAL_LECTERN = BLOCKS.register("mystical_lectern",
            () -> new MysticalLecternBlock(AbstractBlock.Settings.create().strength(2.5f).nonOpaque()));

    // Safe to resolve the block inside these suppliers: vanilla registers BLOCK before ITEM, so the
    // block always exists by the time the item registry is being filled on either loader.
    public static final RegistrySupplier<Item> LIBRARY_ITEM = BLOCK_ITEMS.register("library",
            () -> new BlockItem(LIBRARY.get(), new Item.Settings().maxCount(64)));

    public static final RegistrySupplier<Item> MYSTICAL_LECTERN_ITEM = BLOCK_ITEMS.register("mystical_lectern",
            () -> new BlockItem(MYSTICAL_LECTERN.get(), new Item.Settings().maxCount(64)));

    public static void registerModBlocks(){
        MysticalIndex.LOGGER.info("Registering Mod Blocks");
        BLOCKS.register();
        BLOCK_ITEMS.register();
    }

}
