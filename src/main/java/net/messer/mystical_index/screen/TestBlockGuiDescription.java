package net.messer.mystical_index.screen;

import io.github.cottonmc.cotton.gui.SyncedGuiDescription;
import io.github.cottonmc.cotton.gui.widget.*;
import io.github.cottonmc.cotton.gui.widget.data.Insets;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerContext;

import java.util.List;

public class TestBlockGuiDescription extends SyncedGuiDescription {
    public TestBlockGuiDescription(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
        super(ModScreenHandlers.TEST_BLOCK_SCREEN_HANDLER, syncId, playerInventory, getBlockInventory(context, 2), getBlockPropertyDelegate(context));

        var testItem1 = new ItemStack(Items.DIAMOND, 64);
        var testItem2 = new ItemStack(Items.EMERALD, 64);
        var testItemsList = List.of(testItem1, testItem2);

        WGridPanel root = new WGridPanel();
        setRootPanel(root);
        root.setSize(200, 200);
        root.setInsets(Insets.ROOT_PANEL);

        WPlainPanel plainPanel = new WPlainPanel();
        root.add(plainPanel, 0, 0, 50,50);

        WItem items = new WItem(testItemsList);

        WScrollPanel scrollPanel = new WScrollPanel(items);
        root.add(scrollPanel, 0, 1);

        WItemSlot itemSlot = WItemSlot.of(blockInventory, 0);
        root.add(itemSlot, 4, 1);

        root.add(this.createPlayerInventoryPanel(), 0, 3);

        root.validate(this);
    }
}
