package net.messer.mystical_index.event;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.item.custom.book.MysticalBookItem;
import net.minecraft.util.Identifier;

public class ServerNetworkListeners {
    public static final Identifier SCROLL_ITEM = MysticalIndex.id("scroll_item");

    public static void registerListeners() {
        ServerPlayNetworking.registerGlobalReceiver(SCROLL_ITEM, (server, player, handler, buf, responseSender) -> {
            var syncId = buf.readByte();
            var slotId = buf.readShort();
            var scroll = (byte) (buf.readByte() > 0 ? 1 : -1);

            server.execute(() -> {
                var screen = player.currentScreenHandler;

                if (screen.syncId == syncId && screen.isValid(slotId)) {
                    var slot = screen.getSlot(slotId);
                    var stack = slot.getStack();

                    if (stack.getItem() instanceof MysticalBookItem book) {
                        book.onInventoryScroll(stack, player, scroll);
                    }
                }
            });
        });
    }
}
