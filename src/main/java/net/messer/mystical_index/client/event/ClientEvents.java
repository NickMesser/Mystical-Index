package net.messer.mystical_index.client.event;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.messer.mystical_index.item.custom.book.MysticalBookItem;
import net.messer.mystical_index.mixin.HandledScreenAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;

import static net.messer.mystical_index.event.ServerNetworkListeners.SCROLL_ITEM;

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
        var handledScreen = (HandledScreenAccessor<?>) screen;
        var focusedSlot = handledScreen.getFocusedSlot();

        if (verticalAmount != 0 && focusedSlot != null) {
            var stack = focusedSlot.getStack();
            var scroll = (byte) (verticalAmount > 0 ? 1 : -1);

            if (stack.getItem() instanceof MysticalBookItem book) {
                book.onInventoryScroll(stack, MinecraftClient.getInstance().player, scroll);
            }

            var buf = PacketByteBufs.create();

            buf.writeByte(handledScreen.getHandler().syncId);
            buf.writeShort(focusedSlot.id);
            buf.writeByte(scroll);

            ClientPlayNetworking.send(SCROLL_ITEM, buf);
        }
    }
}
