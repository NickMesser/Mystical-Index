package net.messer.util.neoforge;

import net.messer.util.MenuScreenRegistry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * NeoForge only accepts screen bindings inside {@link RegisterMenuScreensEvent}, which fires after
 * mod construction - later than the point at which the menu types become available and the shared
 * client init asks for them. Each request is therefore parked and replayed when the event arrives.
 *
 * <p>The ordering is safe rather than lucky: menu types are created during the registry events,
 * which always precede client setup, so every binding is queued before the drain runs.
 */
public class MenuScreenRegistryImpl {

    private static final List<Consumer<RegisterMenuScreensEvent>> PENDING = new ArrayList<>();

    public static <M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> void register(
            MenuType<? extends M> type, MenuScreenRegistry.Factory<M, S> factory) {
        PENDING.add(event -> event.register(type, factory::create));
    }

    public static void drain(RegisterMenuScreensEvent event) {
        PENDING.forEach(pending -> pending.accept(event));
        PENDING.clear();
    }
}
