package net.messer.mystical_index.block;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.block.custom.LibraryInventoryBlock;
import net.messer.mystical_index.block.custom.MysticalLecternBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Function;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(MysticalIndex.MOD_ID, Registries.BLOCK);

    // The block items go through their own deferred register rather than ModItems.ITEMS, so the two
    // classes can be initialised in either order without one having to be loaded before the other
    // finishes collecting its entries.
    public static final DeferredRegister<Item> BLOCK_ITEMS =
            DeferredRegister.create(MysticalIndex.MOD_ID, Registries.ITEM);

    // Everything is built inside its supplier, never in the field initialiser. Constructing a Block
    // or an Item creates its intrusive registry holder, which NeoForge only permits while the
    // matching registry is unfrozen, so the construction itself has to be deferred and not just the
    // registration.
    public static final RegistrySupplier<Block> LIBRARY = registerBlock("library",
            props -> new LibraryInventoryBlock(props.strength(1.5f)));

    public static final RegistrySupplier<Block> MYSTICAL_LECTERN = registerBlock("mystical_lectern",
            props -> new MysticalLecternBlock(props.strength(2.5f).noOcclusion()));

    // Safe to resolve the block inside these suppliers: vanilla registers BLOCK before ITEM, so the
    // block always exists by the time the item registry is being filled on either loader.
    public static final RegistrySupplier<Block> SCRIPTORIUM = registerBlock("scriptorium",
            net.messer.mystical_index.block.custom.ScriptoriumBlock::new);

    public static final RegistrySupplier<Item> SCRIPTORIUM_ITEM =
            registerBlockItem("scriptorium", SCRIPTORIUM, "tooltip.mystical_index.scriptorium");

    public static final RegistrySupplier<Item> LIBRARY_ITEM = registerBlockItem("library", LIBRARY, "tooltip.mystical_index.library");

    public static final RegistrySupplier<Item> MYSTICAL_LECTERN_ITEM =
            registerBlockItem("mystical_lectern", MYSTICAL_LECTERN, "tooltip.mystical_index.mystical_lectern");

    /**
     * Blocks now carry their registry id on their settings, exactly like items, so the key is built
     * from the same name the entry registers under.
     */
    private static RegistrySupplier<Block> registerBlock(String name, Function<BlockBehaviour.Properties, Block> factory){
        var key = ResourceKey.create(Registries.BLOCK,
                Identifier.fromNamespaceAndPath(MysticalIndex.MOD_ID, name));
        return BLOCKS.register(name, () -> factory.apply(BlockBehaviour.Properties.of().setId(key)));
    }

    private static RegistrySupplier<Item> registerBlockItem(String name, RegistrySupplier<Block> block, String tooltipKey){
        var key = ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(MysticalIndex.MOD_ID, name));
        return BLOCK_ITEMS.register(name,
                () -> new TooltipBlockItem(block.get(), new Item.Properties().stacksTo(64).setId(key), tooltipKey));
    }

    /**
     * Block no longer contributes to item tooltips in this version, so the line each block used to
     * append from its own appendHoverText is carried by its item instead. Same translation keys,
     * same single line, just raised to the layer that still has the hook.
     */
    private static class TooltipBlockItem extends BlockItem {
        private final String tooltipKey;

        TooltipBlockItem(Block block, Item.Properties properties, String tooltipKey) {
            super(block, properties);
            this.tooltipKey = tooltipKey;
        }

        @Override
        public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                    Consumer<Component> tooltip, TooltipFlag type) {
            tooltip.accept(Component.translatable(tooltipKey));
        }
    }

    public static void registerModBlocks(){
        MysticalIndex.LOGGER.info("Registering Mod Blocks");
        BLOCKS.register();
        BLOCK_ITEMS.register();
    }

}
