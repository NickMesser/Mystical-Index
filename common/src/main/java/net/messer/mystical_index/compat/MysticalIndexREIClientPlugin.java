package net.messer.mystical_index.compat;

import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandlerRegistry;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.item.Items;

public class MysticalIndexREIClientPlugin implements REIClientPlugin {
    @Override
    public void registerCategories(CategoryRegistry registry) {
        REIClientPlugin.super.registerCategories(registry);

        registry.add(new PistonCraftingCategory());
        registry.addWorkstations(PistonCraftingCategory.PISTON_CRAFTING,
                EntryStacks.of(Items.PISTON), EntryStacks.of(Items.IRON_BLOCK));
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        REIClientPlugin.super.registerDisplays(registry);

        for (var display : PistonCraftingDisplay.buildAll())
            registry.add(display);

        for (var display : MysticalIndexCraftingDisplays.buildAll())
            registry.add(display);
    }

    @Override
    public void registerScreens(ScreenRegistry registry) {
        REIClientPlugin.super.registerScreens(registry);
    }

    @Override
    public void registerTransferHandlers(TransferHandlerRegistry registry) {
        registry.register(new LecternTransferHandler());
    }
}
