package net.messer.mystical_index.client.event;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.messer.mystical_index.item.ModItems;
import net.messer.mystical_index.item.custom.book.MysticalBookItem;
import net.messer.mystical_index.mixin.HandledScreenAccessor;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;

@Environment(EnvType.CLIENT)
public class ClientEvents {
    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof HandledScreen<?>) {
                ScreenMouseEvents.afterMouseScroll(screen).register(ClientEvents::scrollOnBook);
            }
        });
    }

    private static void scrollOnBook(Screen screen, double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        var handledScreen = (HandledScreenAccessor) screen;
        var focusedSlot = handledScreen.getFocusedSlot();

        if (focusedSlot != null && focusedSlot.getStack().isOf(ModItems.MYSTICAL_BOOK)) {
            var book = focusedSlot.getStack();
            var bookItem = ((MysticalBookItem) book.getItem());

            // TODO packet to server to scroll book.
        }
    }
}
