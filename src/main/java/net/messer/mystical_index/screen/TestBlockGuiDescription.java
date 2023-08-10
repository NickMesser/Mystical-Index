package net.messer.mystical_index.screen;

import io.github.cottonmc.cotton.gui.SyncedGuiDescription;
import io.github.cottonmc.cotton.gui.networking.NetworkSide;
import io.github.cottonmc.cotton.gui.networking.ScreenNetworking;
import io.github.cottonmc.cotton.gui.widget.*;
import io.github.cottonmc.cotton.gui.widget.data.Axis;
import io.github.cottonmc.cotton.gui.widget.data.InputResult;
import io.github.cottonmc.cotton.gui.widget.data.Insets;
import net.fabricmc.fabric.api.util.TriState;
import net.messer.mystical_index.MysticalIndex;
import net.messer.util.ImplementedInventory;
import net.messer.util.MysticalUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.random.Random;

import java.util.List;

public class TestBlockGuiDescription extends SyncedGuiDescription {
    private static final int INVENTORY_SIZE = 27;
    private static final Identifier ON_SCROLL = new Identifier(MysticalIndex.MOD_ID, "inventory_scroll");
    public int currentIndex = 0;
    public final SimpleInventory INVENTORY = new SimpleInventory(INVENTORY_SIZE);


    public TestBlockGuiDescription(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
        super(ModScreenHandlers.TEST_BLOCK_SCREEN_HANDLER, syncId, playerInventory, getBlockInventory(context, 100), getBlockPropertyDelegate(context));
        context.get((world, pos) -> {
            var blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof ImplementedInventory) {
                var blockInventory = ((ImplementedInventory) blockEntity).getItems();
                for (int i = 0; i < INVENTORY_SIZE; i++) {
                    var itemStack = blockInventory.get(i);
                    INVENTORY.setStack(i, itemStack);
                }
            }
        )};

        var blockInventory = getBlockInventory(context, 100);
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            var itemStack = blockInventory.getStack(i);
            INVENTORY.setStack(i, itemStack);
        }

        WGridPanel root = new WGridPanel();
        setRootPanel(root);
        root.setSize(198, 198);
        root.setInsets(Insets.ROOT_PANEL);

        WScrollBar scrollBar = new WScrollBar(Axis.VERTICAL);

        WItemSlot items = new WItemSlot(INVENTORY, 0, 9, 3, false);
        items.addChangeListener((slot, inventory, index, stack) -> {
            handleItemSlotChange(inventory, index, stack);
        });
        WPlainPanel itemSlotsPanel = new WPlainPanel(){
            @Override
            public InputResult onMouseScroll(int x, int y, double amount) {
                clientHandleScroll(amount);
                return super.onMouseScroll(x, y, amount);
            }
        };

        itemSlotsPanel.add(items, 0, 0);
        root.add(itemSlotsPanel, 0, 1, 11, 4);
        root.add(scrollBar, 9, 1, 1, 3);
        root.add(this.createPlayerInventoryPanel(), 0, 4);

        ScreenNetworking.of(this, NetworkSide.SERVER).receive(ON_SCROLL, buf -> {
            if (world.isClient()) return;
            var amount = buf.readDouble();
            if(amount > 0){
                currentIndex += 9;
            }
            if(amount < 0){
                currentIndex -= 9;
            }
            if(currentIndex >= 81 )
                currentIndex = 81;
            if(currentIndex <= 0)
                currentIndex = 0;

            for(int i = currentIndex; i < currentIndex + INVENTORY_SIZE; i++){
                var itemStack = blockInventory.getStack(i);
                var newI = i - currentIndex;
                INVENTORY.setStack(newI, itemStack);
            }
        });

        root.validate(this);
    }

    public void handleItemSlotChange(Inventory inventory, int index, ItemStack stack){
        blockInventory.setStack(currentIndex + index, stack);
        blockInventory.markDirty();
    }

    public void clientHandleScroll(double amount){
        ScreenNetworking.of(this, NetworkSide.CLIENT).send(ON_SCROLL, buf -> buf.writeDouble(amount));
    }
}
