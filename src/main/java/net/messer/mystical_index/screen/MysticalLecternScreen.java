package net.messer.mystical_index.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.messer.mystical_index.item.inventory.LibraryNetwork;
import net.messer.mystical_index.network.LecternClientNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class MysticalLecternScreen extends HandledScreen<MysticalLecternScreenHandler> {
    private static final Identifier TEXTURE = new Identifier("mystical_index:textures/gui/lectern_gui.png");
    private static final Identifier SCROLLER_TEXTURE = new Identifier("textures/gui/container/creative_inventory/tabs.png");

    private static final int COLUMNS = 9;
    private static final int ROWS = 3;
    private static final int NETWORK_X = 8;
    private static final int NETWORK_Y = 18;

    private static final int SCROLLBAR_X = 172;
    private static final int SCROLLBAR_Y = 17;
    private static final int SCROLLBAR_TRAVEL = 38;

    // The search field sits in the header, right of the "Mystical Lectern" title and left of the
    // sort button; it never overlaps a slot.
    private static final int SEARCH_X = 100;
    private static final int SEARCH_Y = 4;
    private static final int SEARCH_W = 44;
    private static final int SEARCH_H = 12;

    // The sort toggle tucks into the header between the search field and the scrollbar groove, so it
    // stays out of the crafting flow entirely.
    private static final int SORT_X = 146;
    private static final int SORT_Y = 3;
    private static final int SORT_W = 22;
    private static final int SORT_H = 14;

    private enum SortMode { NAME, COUNT }

    private int scrollRow;
    private boolean scrolling;

    private String searchQuery = "";
    private SortMode sortMode = SortMode.NAME;

    private TextFieldWidget searchField;
    private ButtonWidget sortButton;

    // The filtered + sorted view of the network, rebuilt only when the source list, the query, or
    // the sort mode changes. The handler hands back a fresh list only when the contents actually
    // change, so comparing the source by reference keeps this off the per-frame hot path.
    private List<LibraryNetwork.Entry> cachedView = List.of();
    private List<LibraryNetwork.Entry> cachedSource;
    private String cachedQuery;
    private SortMode cachedSort;

    public MysticalLecternScreen(MysticalLecternScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 195;
        this.backgroundHeight = 221;
        this.playerInventoryTitleY = 127;
    }

    @Override
    protected void init() {
        super.init();

        searchField = new TextFieldWidget(textRenderer, x + SEARCH_X, y + SEARCH_Y, SEARCH_W, SEARCH_H, Text.translatable("gui.mystical_index.lectern.search"));
        searchField.setMaxLength(50);
        searchField.setDrawsBackground(true);
        searchField.setPlaceholder(Text.translatable("gui.mystical_index.lectern.search_placeholder").formatted(Formatting.DARK_GRAY));
        // Restore the query before wiring the listener so the re-init that happens on resize does
        // not fire onSearchChanged and reset the scroll.
        searchField.setText(searchQuery);
        searchField.setChangedListener(this::onSearchChanged);
        addDrawableChild(searchField);

        sortButton = ButtonWidget.builder(sortLabel(), button -> cycleSort())
                .dimensions(x + SORT_X, y + SORT_Y, SORT_W, SORT_H)
                .tooltip(Tooltip.of(Text.translatable("gui.mystical_index.lectern.sort_tooltip")))
                .build();
        addDrawableChild(sortButton);
    }

    private void onSearchChanged(String query) {
        if (query.equals(searchQuery))
            return;

        searchQuery = query;
        // A new filter changes which entries occupy which cell, so anchor the view back at the top.
        scrollRow = 0;
    }

    private Text sortLabel() {
        return Text.translatable(sortMode == SortMode.NAME
                ? "gui.mystical_index.lectern.sort_name"
                : "gui.mystical_index.lectern.sort_count");
    }

    private void cycleSort() {
        sortMode = sortMode == SortMode.NAME ? SortMode.COUNT : SortMode.NAME;
        if (sortButton != null)
            sortButton.setMessage(sortLabel());
        scrollRow = 0;
    }

    // The single source of truth for what the network area shows: filtered by the search query,
    // then ordered by the current sort mode. Everything that reads the network (rendering, hit
    // testing, scroll bounds) goes through here so indices stay consistent.
    private List<LibraryNetwork.Entry> visibleEntries() {
        var source = handler.getClientEntries();
        if (source == cachedSource && sortMode == cachedSort && searchQuery.equals(cachedQuery))
            return cachedView;

        List<LibraryNetwork.Entry> view;
        if (searchQuery.isEmpty()) {
            view = new ArrayList<>(source);
        } else {
            String needle = searchQuery.toLowerCase(Locale.ROOT);
            view = new ArrayList<>();
            for (var entry : source) {
                String name = entry.variant().toStack(1).getName().getString().toLowerCase(Locale.ROOT);
                if (name.contains(needle))
                    view.add(entry);
            }
        }

        switch (sortMode) {
            case NAME -> view.sort(Comparator.comparing(
                    (LibraryNetwork.Entry entry) -> entry.variant().toStack(1).getName().getString().toLowerCase(Locale.ROOT)));
            case COUNT -> view.sort(Comparator.comparingLong(LibraryNetwork.Entry::count).reversed());
        }

        cachedSource = source;
        cachedSort = sortMode;
        cachedQuery = searchQuery;
        cachedView = view;
        return view;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        context.drawTexture(TEXTURE, x, y, 0, 0, backgroundWidth, backgroundHeight);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        super.drawForeground(context, mouseX, mouseY);

        var entries = visibleEntries();
        scrollRow = MathHelper.clamp(scrollRow, 0, maxScrollRow());

        int hoveredX = -1;
        int hoveredY = -1;

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int index = (scrollRow + row) * COLUMNS + col;
                if (index >= entries.size())
                    continue;

                var entry = entries.get(index);
                int cellX = NETWORK_X + col * 18;
                int cellY = NETWORK_Y + row * 18;

                context.drawItem(entry.variant().toStack(1), cellX, cellY);
                drawCount(context, formatCount(entry.count()), cellX, cellY);

                // Test the hover in absolute space against the same strict 16x16 item rect that
                // entryAt (click/tooltip) uses, so the highlight only lights the cell you can click.
                if (mouseX >= x + cellX && mouseX < x + cellX + 16 && mouseY >= y + cellY && mouseY < y + cellY + 16) {
                    hoveredX = cellX;
                    hoveredY = cellY;
                }
            }
        }

        if (hoveredX >= 0) {
            context.getMatrices().push();
            context.getMatrices().translate(0.0F, 0.0F, 200.0F);
            context.fill(hoveredX, hoveredY, hoveredX + 16, hoveredY + 16, 0x80FFFFFF);
            context.getMatrices().pop();
        }

        drawScrollbar(context);
    }

    private void drawScrollbar(DrawContext context) {
        // Track is 14px wide: a 1px dark border framing a 12px grey channel that exactly matches the
        // 12px-wide thumb sprite. Drawing the channel 13px wide left a 1px grey sliver beside the thumb.
        context.fill(SCROLLBAR_X, SCROLLBAR_Y, SCROLLBAR_X + 14, SCROLLBAR_Y + 55, 0xFF373737);
        context.fill(SCROLLBAR_X + 1, SCROLLBAR_Y + 1, SCROLLBAR_X + 13, SCROLLBAR_Y + 54, 0xFF8B8B8B);

        int max = maxScrollRow();
        int thumbOffset = max == 0 ? 0 : (int) (SCROLLBAR_TRAVEL * (scrollRow / (float) max));
        context.drawTexture(SCROLLER_TEXTURE, SCROLLBAR_X + 1, SCROLLBAR_Y + 1 + thumbOffset, max > 0 ? 232 : 244, 0, 12, 15);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);

        var entry = entryAt(mouseX, mouseY);
        if (entry == null)
            return;

        var stack = entry.variant().toStack(1);
        List<Text> tooltip = new ArrayList<>();
        if (handler.getCursorStack().isEmpty()) {
            // Empty hand: the full vanilla item tooltip plus the exact stored count.
            tooltip.addAll(getTooltipFromItem(client, stack));
            tooltip.add(Text.translatable("gui.mystical_index.lectern.stored", entry.count()).formatted(Formatting.GRAY));
        } else {
            // Holding a stack: a minimal name + exact count, so the stored amount is still readable
            // while the cursor is full instead of the tooltip vanishing entirely.
            tooltip.add(stack.getName());
            tooltip.add(Text.translatable("gui.mystical_index.lectern.stored", entry.count()).formatted(Formatting.GRAY));
        }
        context.drawTooltip(textRenderer, tooltip, Optional.empty(), mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Any click that is not on the search field drops its focus, so hotbar number keys and the
        // inventory key start working again the instant you interact with anything else.
        if (searchField != null && searchField.isFocused() && !searchField.isMouseOver(mouseX, mouseY))
            searchField.setFocused(false);

        if (isOverNetwork(mouseX, mouseY)) {
            // Only left (0) and right (1) act. Ignore middle/other buttons so a middle-click can no
            // longer dump the whole cursor stack into the network.
            if (button != 0 && button != 1)
                return true;

            if (!handler.getCursorStack().isEmpty()) {
                LecternClientNetworking.sendInsert(handler.syncId, button);
            } else {
                var entry = entryAt(mouseX, mouseY);
                if (entry != null)
                    LecternClientNetworking.sendExtract(handler.syncId, entry.variant(), button, hasShiftDown());
            }
            return true;
        }

        if (isOverScrollbar(mouseX, mouseY)) {
            scrolling = true;
            updateScrollFromMouse(mouseY);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // While the search field has focus, route typing to it and swallow the key so hotbar number
        // keys do not fire mid-word. Escape still falls through so it can close the screen.
        if (searchField != null && searchField.isFocused() && keyCode != GLFW.GLFW_KEY_ESCAPE) {
            searchField.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchField != null && searchField.isFocused())
            return searchField.charTyped(chr, modifiers);
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (scrolling) {
            updateScrollFromMouse(mouseY);
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        scrolling = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int max = maxScrollRow();
        if (isOverNetwork(mouseX, mouseY) && max > 0) {
            scrollRow = MathHelper.clamp(scrollRow - (int) Math.signum(amount), 0, max);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    private int maxScrollRow() {
        int rows = (visibleEntries().size() + COLUMNS - 1) / COLUMNS;
        return Math.max(0, rows - ROWS);
    }

    private boolean isOverScrollbar(double mouseX, double mouseY) {
        return mouseX >= x + SCROLLBAR_X && mouseX < x + SCROLLBAR_X + 15
                && mouseY >= y + SCROLLBAR_Y && mouseY < y + SCROLLBAR_Y + 55;
    }

    private void updateScrollFromMouse(double mouseY) {
        int max = maxScrollRow();
        // Centre the thumb on the cursor: half of the 15px thumb, over the 38px of travel.
        scrollRow = MathHelper.clamp((int) Math.round((mouseY - (y + SCROLLBAR_Y + 1) - 7.5) / SCROLLBAR_TRAVEL * max), 0, max);
    }

    private boolean isOverNetwork(double mouseX, double mouseY) {
        return mouseX >= x + NETWORK_X && mouseX < x + NETWORK_X + COLUMNS * 18
                && mouseY >= y + NETWORK_Y && mouseY < y + NETWORK_Y + ROWS * 18;
    }

    private LibraryNetwork.Entry entryAt(double mouseX, double mouseY) {
        var entries = visibleEntries();

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int cellX = x + NETWORK_X + col * 18;
                int cellY = y + NETWORK_Y + row * 18;
                if (mouseX < cellX || mouseX >= cellX + 16 || mouseY < cellY || mouseY >= cellY + 16)
                    continue;

                int index = (scrollRow + row) * COLUMNS + col;
                return index < entries.size() ? entries.get(index) : null;
            }
        }

        return null;
    }

    private void drawCount(DrawContext context, String text, int cellX, int cellY) {
        var matrices = context.getMatrices();
        matrices.push();
        matrices.translate(0.0F, 0.0F, 200.0F);
        matrices.scale(0.5F, 0.5F, 1.0F);
        context.drawTextWithShadow(textRenderer, text, cellX * 2 + 33 - textRenderer.getWidth(text), cellY * 2 + 24, 0xFFFFFF);
        matrices.pop();
    }

    private static String formatCount(long count) {
        if (count < 1000)
            return Long.toString(count);
        if (count < 10000)
            return String.format(Locale.ROOT, "%.1fK", count / 1000.0);
        if (count < 1000000)
            return (count / 1000) + "K";
        if (count < 10000000)
            return String.format(Locale.ROOT, "%.1fM", count / 1000000.0);
        return (count / 1000000) + "M";
    }
}
