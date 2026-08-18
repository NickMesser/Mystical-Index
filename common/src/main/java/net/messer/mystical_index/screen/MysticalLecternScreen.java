package net.messer.mystical_index.screen;

import net.messer.mystical_index.client.LecternRangeVisualizer;
import net.messer.mystical_index.network.LecternClientNetworking;
import net.messer.mystical_index.item.inventory.LibraryNetwork;
import net.messer.mystical_index.network.LecternClientNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class MysticalLecternScreen extends AbstractContainerScreen<MysticalLecternScreenHandler> {
    private static final Identifier TEXTURE = Identifier.parse("mystical_index:textures/gui/lectern_gui.png");
    // 1.20.2+ split the creative tabs sheet into individual GUI sprites, so the thumb is drawn
    // from the sprite atlas instead of by uv offset into a texture that no longer exists.
    private static final Identifier SCROLLER = Identifier.parse("container/creative_inventory/scroller");
    private static final Identifier SCROLLER_DISABLED = Identifier.parse("container/creative_inventory/scroller_disabled");

    private static final int WIDTH = 195;
    private static final int HEIGHT = 221;

    // Size of the PNG file, not of the panel. blit's last two arguments are what the UVs are
    // divided by, so they have to describe the image on disk; passing the panel size instead makes
    // the whole canvas - including the empty area past the artwork - get squashed into the panel.
    private static final int TEXTURE_SIZE = 256;

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
    // Range-toggle button. Placed in the right-margin band under the scrollbar groove - the only
    // interior region free of every slot rect (network grid ends x=170, scrollbar groove ends
    // y=72, capacity row starts y=127). Verified against all 77 slot/widget rects: zero overlaps.
    private static final int RANGE_X = 172;
    private static final int RANGE_Y = 78;
    private static final int RANGE_SIZE = 14;
    private static final int RANGE_ICON_OFF_U = 196;
    private static final int RANGE_ICON_ON_U = 212;
    private static final int RANGE_ICON_V = 0;

    private static final int SORT_X = 146;
    private static final int SORT_Y = 3;
    private static final int SORT_W = 22;
    private static final int SORT_H = 14;

    private enum SortMode { NAME, COUNT }

    private int scrollRow;
    private boolean scrolling;

    private String searchQuery = "";
    private SortMode sortMode = SortMode.NAME;

    private EditBox searchField;
    private Button sortButton;

    // The filtered + sorted view of the network, rebuilt only when the source list, the query, or
    // the sort mode changes. The handler hands back a fresh list only when the contents actually
    // change, so comparing the source by reference keeps this off the per-frame hot path.
    private List<LibraryNetwork.Entry> cachedView = List.of();
    private List<LibraryNetwork.Entry> cachedSource;
    private String cachedQuery;
    private SortMode cachedSort;

    public MysticalLecternScreen(MysticalLecternScreenHandler menu, Inventory inventory, Component title) {
        // imageWidth/imageHeight are final now and can only be set through the constructor.
        super(menu, inventory, title, WIDTH, HEIGHT);
        this.inventoryLabelY = 127;
    }

    @Override
    protected void init() {
        super.init();

        searchField = new EditBox(font, leftPos + SEARCH_X, topPos + SEARCH_Y, SEARCH_W, SEARCH_H, Component.translatable("gui.mystical_index.lectern.search"));
        searchField.setMaxLength(50);
        searchField.setBordered(true);
        searchField.setHint(Component.translatable("gui.mystical_index.lectern.search_placeholder").withStyle(ChatFormatting.DARK_GRAY));
        // Restore the query before wiring the listener so the re-init that happens on resize does
        // not fire onSearchChanged and reset the scroll.
        searchField.setValue(searchQuery);
        searchField.setResponder(this::onSearchChanged);
        addRenderableWidget(searchField);

        sortButton = Button.builder(sortLabel(), button -> cycleSort())
                .bounds(leftPos + SORT_X, topPos + SORT_Y, SORT_W, SORT_H)
                .tooltip(Tooltip.create(Component.translatable("gui.mystical_index.lectern.sort_tooltip")))
                .build();
        addRenderableWidget(sortButton);
    }

    private void onSearchChanged(String query) {
        if (query.equals(searchQuery))
            return;

        searchQuery = query;
        // A new filter changes which entries occupy which cell, so anchor the view back at the top.
        scrollRow = 0;
    }

    private Component sortLabel() {
        return Component.translatable(sortMode == SortMode.NAME
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
        var source = menu.getClientEntries();
        if (source == cachedSource && sortMode == cachedSort && searchQuery.equals(cachedQuery))
            return cachedView;

        List<LibraryNetwork.Entry> view;
        if (searchQuery.isEmpty()) {
            view = new ArrayList<>(source);
        } else {
            String needle = searchQuery.toLowerCase(Locale.ROOT);
            view = new ArrayList<>();
            for (var entry : source) {
                String name = entry.variant().toStack(1).getHoverName().getString().toLowerCase(Locale.ROOT);
                if (name.contains(needle))
                    view.add(entry);
            }
        }

        switch (sortMode) {
            case NAME -> view.sort(Comparator.comparing(
                    (LibraryNetwork.Entry entry) -> entry.variant().toStack(1).getHoverName().getString().toLowerCase(Locale.ROOT)));
            case COUNT -> view.sort(Comparator.comparingLong(LibraryNetwork.Entry::count).reversed());
        }

        cachedSource = source;
        cachedSort = sortMode;
        cachedQuery = searchQuery;
        cachedView = view;
        return view;
    }

    // Screens no longer expose a background hook of their own; the panel is emitted as part of the
    // contents pass, before super draws the slots on top of it. The manual shader/texture binding
    // the old drawBackground needed is gone with the pipeline model - blit names the pipeline.
    @Override
    public void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0.0F, 0.0F,
                WIDTH, HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);

        // Lit variant while this lectern's overlay is on. Same 14x14 geometry, purple palette -
        // the state is legible without a separate frame or label.
        var lectern = lecternPos();
        boolean lit = lectern != null && LecternRangeVisualizer.isToggled(lectern);
        context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                leftPos + RANGE_X, topPos + RANGE_Y,
                (float) (lit ? RANGE_ICON_ON_U : RANGE_ICON_OFF_U), (float) RANGE_ICON_V,
                RANGE_SIZE, RANGE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE);

        super.extractContents(context, mouseX, mouseY, delta);
    }

    /** Hover text over the range toggle, phrased for what the click will do next. */
    private void extractRangeTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        int mx = mouseX - leftPos;
        int my = mouseY - topPos;
        if (mx < RANGE_X || mx >= RANGE_X + RANGE_SIZE || my < RANGE_Y || my >= RANGE_Y + RANGE_SIZE)
            return;

        var lectern = lecternPos();
        boolean lit = lectern != null && LecternRangeVisualizer.isToggled(lectern);
        context.setTooltipForNextFrame(font,
                List.of(Component.translatable(lit
                        ? "gui.mystical_index.lectern.hide_links"
                        : "gui.mystical_index.lectern.show_links")),
                Optional.empty(), mouseX, mouseY);
    }

    /** The lectern this screen is looking at, as reported by the server's links payload. */
    private net.minecraft.core.BlockPos lecternPos() {
        var links = LecternClientNetworking.lastLinks;
        return links == null ? null : links.lectern();
    }

    // Right edge of the panel's usable interior. The capacity readout is right-aligned to it.
    private static final int CAPACITY_RIGHT = WIDTH - 8;

    /**
     * "Slots: used/total", right-aligned on the inventory-label row.
     *
     * <p>That row is the only strip on this panel with dependable free space: the header is fully
     * occupied (title, search field, sort button, scrollbar groove), and the row carrying the
     * "Inventory" label is empty to its right. The x is derived from the real
     * {@code font.width(...)} at draw time rather than an estimate, so it stays right-aligned
     * whatever the numbers or the language do to the string's length.
     */
    private void extractCapacity(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        var capacity = menu.clientCapacity;
        var text = Component.translatable("gui.mystical_index.lectern.slots",
                capacity.used(), capacity.total());

        int width = font.width(text);
        int x = CAPACITY_RIGHT - width;
        int y = this.inventoryLabelY;

        // An empty network still reports, dimmed - "0/0" says the lectern sees nothing, where a
        // blank row is indistinguishable from the feature being broken.
        // Six-arg form with dropShadow=false: the five-arg one shadows by default, which is what
        // made this read as muddy doubling next to the shadowless "Inventory" label. 0xFF404040 is
        // the exact colour vanilla's own extractLabels uses for that label.
        boolean empty = capacity.total() == 0;
        context.text(this.font, text, x, y, empty ? 0xFF909090 : 0xFF404040, false);

        // Labels draw in panel-local space; the mouse arrives in screen space.
        int mx = mouseX - leftPos;
        int my = mouseY - topPos;
        if (mx < x || mx >= x + width || my < y || my >= y + font.lineHeight)
            return;

        List<Component> detail = List.of(Component.translatable("gui.mystical_index.lectern.slots_detail",
                capacity.books(), capacity.libraries()));
        context.setTooltipForNextFrame(font, detail, Optional.empty(), mouseX, mouseY);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        super.extractLabels(context, mouseX, mouseY);

        extractCapacity(context, mouseX, mouseY);
        extractRangeTooltip(context, mouseX, mouseY);

        var entries = visibleEntries();
        scrollRow = Mth.clamp(scrollRow, 0, maxScrollRow());

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

                context.item(entry.variant().toStack(1), cellX, cellY);
                drawCount(context, formatCount(entry.count()), cellX, cellY);

                // Test the hover in absolute space against the same strict 16x16 item rect that
                // entryAt (click/tooltip) uses, so the highlight only lights the cell you can click.
                if (mouseX >= leftPos + cellX && mouseX < leftPos + cellX + 16 && mouseY >= topPos + cellY && mouseY < topPos + cellY + 16) {
                    hoveredX = cellX;
                    hoveredY = cellY;
                }
            }
        }

        if (hoveredX >= 0) {
            // The pose stack is 2D now, so depth cannot be pushed; nextStratum is what lifts
            // subsequent draws above what has already been emitted.
            context.nextStratum();
            context.fill(hoveredX, hoveredY, hoveredX + 16, hoveredY + 16, 0x80FFFFFF);
        }

        drawScrollbar(context);
        extractNetworkTooltip(context, mouseX, mouseY);
    }

    private void drawScrollbar(GuiGraphicsExtractor context) {
        // Track is 14px wide: a 1px dark border framing a 12px grey channel that exactly matches the
        // 12px-wide thumb sprite. Drawing the channel 13px wide left a 1px grey sliver beside the thumb.
        context.fill(SCROLLBAR_X, SCROLLBAR_Y, SCROLLBAR_X + 14, SCROLLBAR_Y + 55, 0xFF373737);
        context.fill(SCROLLBAR_X + 1, SCROLLBAR_Y + 1, SCROLLBAR_X + 13, SCROLLBAR_Y + 54, 0xFF8B8B8B);

        int max = maxScrollRow();
        int thumbOffset = max == 0 ? 0 : (int) (SCROLLBAR_TRAVEL * (scrollRow / (float) max));
        context.blitSprite(RenderPipelines.GUI_TEXTURED, max > 0 ? SCROLLER : SCROLLER_DISABLED,
                SCROLLBAR_X + 1, SCROLLBAR_Y + 1 + thumbOffset, 12, 15);
    }

    // Tooltips are queued for the frame rather than drawn inline, so this moved out of render()
    // (which must not be overridden any more) into the label pass. Coordinates stay absolute.
    private void extractNetworkTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        var entry = entryAt(mouseX, mouseY);
        if (entry == null)
            return;

        var stack = entry.variant().toStack(1);
        List<Component> tooltip = new ArrayList<>();
        if (menu.getCarried().isEmpty()) {
            // Empty hand: the full vanilla item tooltip plus the exact stored count.
            tooltip.addAll(getTooltipFromItem(minecraft, stack));
            tooltip.add(Component.translatable("gui.mystical_index.lectern.stored", entry.count()).withStyle(ChatFormatting.GRAY));
        } else {
            // Holding a stack: a minimal name + exact count, so the stored amount is still readable
            // while the cursor is full instead of the tooltip vanishing entirely.
            tooltip.add(stack.getHoverName());
            tooltip.add(Component.translatable("gui.mystical_index.lectern.stored", entry.count()).withStyle(ChatFormatting.GRAY));
        }
        context.setTooltipForNextFrame(font, tooltip, Optional.empty(), mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        // Range toggle: a plain hit test rather than a Button widget, so the icon can be drawn
        // straight from the panel sheet and stay in the mod's style.
        if (button == 0
                && mouseX >= leftPos + RANGE_X && mouseX < leftPos + RANGE_X + RANGE_SIZE
                && mouseY >= topPos + RANGE_Y && mouseY < topPos + RANGE_Y + RANGE_SIZE) {
            var links = LecternClientNetworking.lastLinks;
            if (links != null) {
                LecternRangeVisualizer.toggle(links);
                return true;
            }
        }

        // Any click that is not on the search field drops its focus, so hotbar number keys and the
        // inventory key start working again the instant you interact with anything else.
        if (searchField != null && searchField.isFocused() && !searchField.isMouseOver(mouseX, mouseY))
            searchField.setFocused(false);

        if (isOverNetwork(mouseX, mouseY)) {
            // Only left (0) and right (1) act. Ignore middle/other buttons so a middle-click can no
            // longer dump the whole cursor stack into the network.
            if (button != 0 && button != 1)
                return true;

            if (!menu.getCarried().isEmpty()) {
                LecternClientNetworking.sendInsert(menu.containerId, button);
            } else {
                var entry = entryAt(mouseX, mouseY);
                if (entry != null)
                    LecternClientNetworking.sendExtract(menu.containerId, entry.variant(), button,
                            Minecraft.getInstance().hasShiftDown());
            }
            return true;
        }

        if (isOverScrollbar(mouseX, mouseY)) {
            scrolling = true;
            updateScrollFromMouse(mouseY);
            return true;
        }

        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // While the search field has focus, route typing to it and swallow the key so hotbar number
        // keys do not fire mid-word. Escape still falls through so it can close the screen.
        if (searchField != null && searchField.isFocused() && event.key() != GLFW.GLFW_KEY_ESCAPE) {
            searchField.keyPressed(event);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (searchField != null && searchField.isFocused())
            return searchField.charTyped(event);
        return super.charTyped(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (scrolling) {
            updateScrollFromMouse(event.y());
            return true;
        }

        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        scrolling = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double amount) {
        int max = maxScrollRow();
        if (isOverNetwork(mouseX, mouseY) && max > 0) {
            scrollRow = Mth.clamp(scrollRow - (int) Math.signum(amount), 0, max);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, amount);
    }

    private int maxScrollRow() {
        int rows = (visibleEntries().size() + COLUMNS - 1) / COLUMNS;
        return Math.max(0, rows - ROWS);
    }

    private boolean isOverScrollbar(double mouseX, double mouseY) {
        return mouseX >= leftPos + SCROLLBAR_X && mouseX < leftPos + SCROLLBAR_X + 15
                && mouseY >= topPos + SCROLLBAR_Y && mouseY < topPos + SCROLLBAR_Y + 55;
    }

    private void updateScrollFromMouse(double mouseY) {
        int max = maxScrollRow();
        // Centre the thumb on the cursor: half of the 15px thumb, over the 38px of travel.
        scrollRow = Mth.clamp((int) Math.round((mouseY - (topPos + SCROLLBAR_Y + 1) - 7.5) / SCROLLBAR_TRAVEL * max), 0, max);
    }

    private boolean isOverNetwork(double mouseX, double mouseY) {
        return mouseX >= leftPos + NETWORK_X && mouseX < leftPos + NETWORK_X + COLUMNS * 18
                && mouseY >= topPos + NETWORK_Y && mouseY < topPos + NETWORK_Y + ROWS * 18;
    }

    private LibraryNetwork.Entry entryAt(double mouseX, double mouseY) {
        var entries = visibleEntries();

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int cellX = leftPos + NETWORK_X + col * 18;
                int cellY = topPos + NETWORK_Y + row * 18;
                if (mouseX < cellX || mouseX >= cellX + 16 || mouseY < cellY || mouseY >= cellY + 16)
                    continue;

                int index = (scrollRow + row) * COLUMNS + col;
                return index < entries.size() ? entries.get(index) : null;
            }
        }

        return null;
    }

    private void drawCount(GuiGraphicsExtractor context, String text, int cellX, int cellY) {
        // Half scale keeps the count legible without covering the item. The pose stack is 2D, so
        // the old z-translation that lifted the text is replaced by nextStratum.
        var pose = context.pose();
        context.nextStratum();
        pose.pushMatrix();
        pose.scale(0.5F, 0.5F);
        context.text(font, text, cellX * 2 + 33 - font.width(text), cellY * 2 + 24, 0xFFFFFFFF);
        pose.popMatrix();
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
