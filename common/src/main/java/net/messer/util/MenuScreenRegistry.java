package net.messer.util;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

/**
 * Binds a menu type to the screen that displays it.
 *
 * <p>Architectury's {@code MenuRegistry.registerScreenFactory} no longer exists in the 20.x line,
 * and vanilla's {@code MenuScreens.register} is package-private along with the constructor interface
 * it takes, so shared code cannot reach either. Fabric reopens both through the transitive access
 * wideners its menu API ships; NeoForge instead hands them out only inside
 * {@code RegisterMenuScreensEvent}, which fires later than mod construction - hence the split.
 *
 * <p>The factory below stands in for vanilla's unreachable {@code ScreenConstructor}. Its shape is
 * identical, so each loader forwards it as a method reference with no adaptation.
 */
@Environment(EnvType.CLIENT)
public class MenuScreenRegistry {

    public interface Factory<M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> {
        S create(M menu, Inventory inventory, Component title);
    }

    @ExpectPlatform
    public static <M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> void register(
            MenuType<? extends M> type, Factory<M, S> factory) {
        throw new AssertionError();
    }
}
