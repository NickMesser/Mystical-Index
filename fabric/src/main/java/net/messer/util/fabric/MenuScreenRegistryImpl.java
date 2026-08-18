package net.messer.util.fabric;

import net.messer.util.MenuScreenRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

public class MenuScreenRegistryImpl {

    // Fabric's menu API widens MenuScreens.register and its ScreenConstructor transitively, so the
    // vanilla call is available directly and can happen the moment the menu type exists.
    public static <M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> void register(
            MenuType<? extends M> type, MenuScreenRegistry.Factory<M, S> factory) {
        MenuScreens.register(type, factory::create);
    }
}
