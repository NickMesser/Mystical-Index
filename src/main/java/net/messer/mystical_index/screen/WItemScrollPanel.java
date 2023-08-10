package net.messer.mystical_index.screen;

import io.github.cottonmc.cotton.gui.SyncedGuiDescription;
import io.github.cottonmc.cotton.gui.networking.NetworkSide;
import io.github.cottonmc.cotton.gui.networking.ScreenNetworking;
import io.github.cottonmc.cotton.gui.widget.WScrollPanel;
import io.github.cottonmc.cotton.gui.widget.WWidget;
import io.github.cottonmc.cotton.gui.widget.data.InputResult;
import net.messer.mystical_index.MysticalIndex;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public class WItemScrollPanel extends WScrollPanel {
    public WItemScrollPanel(WWidget widget) {
        super(widget);
    }

    @Override
    public InputResult onMouseScroll(int x, int y, double amount) {
        return super.onMouseScroll(x, y, amount);
    }

    @Override
    public void paint(DrawContext context, int x, int y, int mouseX, int mouseY) {
        super.paint(context, x, y, mouseX, mouseY);
        int lastScroll = -1;
        if(verticalScrollBar.getValue() != lastScroll) {
            lastScroll = verticalScrollBar.getValue();
            ((SyncedGuiDescription)getHost()).slots.clear();
            getHost().getRootPanel().validate(getHost());
        }
    }
}
