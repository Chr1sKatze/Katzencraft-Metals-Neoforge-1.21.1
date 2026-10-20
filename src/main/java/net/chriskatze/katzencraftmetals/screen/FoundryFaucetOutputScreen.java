package net.chriskatze.katzencraftmetals.screen;

import net.chriskatze.katzencraftmetals.metal.MetalNames;
import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.menu.FoundryFaucetOutputMenu;
import net.chriskatze.katzencraftmetals.metal.MoltenMetalDefinition;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class FoundryFaucetOutputScreen
        extends AbstractContainerScreen<FoundryFaucetOutputMenu> {

    private static final ResourceLocation FRAME_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    KatzencraftMetalsMod.MODID,
                    "textures/gui/foundry_controller.png"
            );
    private static final int HOVER_OVERLAY = 0x18FFFFFF;
    private static final int TEXT = FoundryControllerUiDrawing.TEXT;

    private static final int ROW_X = 8;
    private static final int ROW_Y = 28;
    private static final int ROW_WIDTH = 86;
    private static final int ROW_HEIGHT = 19;
    private static final int ROW_STRIDE = 21;

    /*
     * Default precedes the discovered metals. Four entries are visible at
     * once; scrolling retains the existing combined-list behavior.
     */
    private static final int VISIBLE_ROWS = 4;

    private static final int SCROLL_X = 96;
    private static final int SCROLL_WIDTH = 6;
    private static final int SCROLL_Y = ROW_Y;
    private static final int MAX_HEIGHT = 118;
    private static final int THUMB_HEIGHT = 17;

    private static final int TITLE_Y = 8;

    private int scrollOffset;
    private boolean draggingScrollbar;
    private double thumbGrabOffset;

    public FoundryFaucetOutputScreen(
            FoundryFaucetOutputMenu menu,
            Inventory playerInventory,
            Component title
    ) {
        super(
                menu,
                playerInventory,
                title
        );

        imageWidth = 110;
        imageHeight = MAX_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();

        updateLayout();
        draggingScrollbar = false;
        titleLabelX = -1000;
        inventoryLabelX = -1000;
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        updateLayout();
        renderBackground(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );

        super.render(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );

        renderTooltip(
                graphics,
                mouseX,
                mouseY
        );

        renderRowTooltip(
                graphics,
                mouseX,
                mouseY
        );
    }

    @Override
    protected void renderBg(
            GuiGraphics graphics,
            float partialTick,
            int mouseX,
            int mouseY
    ) {
        clampScroll();

        renderPanel(graphics);
        renderTitle(graphics);

        renderRows(
                graphics,
                mouseX,
                mouseY
        );

        if (getTotalRows() > VISIBLE_ROWS) {
            FoundryControllerListDrawing.drawScrollbar(
                    graphics,
                    leftPos + SCROLL_X,
                    topPos + SCROLL_Y,
                    getScrollHeight(),
                    getTotalRows(),
                    VISIBLE_ROWS,
                    scrollOffset
            );
            renderScrollbarFinish(graphics);
        } else {
            FoundryControllerListDrawing.drawScrollbarTrack(
                    graphics, leftPos + SCROLL_X, topPos + SCROLL_Y,
                    getScrollHeight()
            );
        }
    }

    private void renderScrollbarFinish(
            GuiGraphics graphics
    ) {
        int left = leftPos + SCROLL_X;
        int top = topPos + SCROLL_Y;
        // Preserve the shared textured track and its existing 6 x 17 thumb.
        graphics.renderOutline(left, top, SCROLL_WIDTH, getScrollHeight(),
                FoundryControllerListDrawing.ROW_OUTLINE);
        int thumbTop = getThumbTop();
        graphics.renderOutline(left, thumbTop, SCROLL_WIDTH, THUMB_HEIGHT, 0xFF292929);
        graphics.fill(left + 1, thumbTop + 1, left + SCROLL_WIDTH - 1, thumbTop + 2,
                0xFF747474);
        graphics.fill(left + 2, thumbTop + THUMB_HEIGHT / 2,
                left + SCROLL_WIDTH - 2, thumbTop + THUMB_HEIGHT / 2 + 1,
                0xFF383838);
    }

    private void renderPanel(
            GuiGraphics graphics
    ) {
        // A two-pixel rim outside the unchanged menu bounds, without decoration.
        graphics.renderOutline(leftPos - 2, topPos - 2,
                imageWidth + 4, imageHeight + 4, 0xFF161616);
        graphics.renderOutline(leftPos - 1, topPos - 1,
                imageWidth + 2, imageHeight + 2, 0xFF626262);
        // Match the Controller's outer metal surround and dark inset sections.
        graphics.fill(
                leftPos,
                topPos,
                leftPos + imageWidth,
                topPos + imageHeight,
                0xFF3B3B3B
        );
        drawInsetFrame(graphics, leftPos + 2, topPos + 2, imageWidth - 4, 19);
        drawInsetFrame(graphics, leftPos + 2, topPos + 23, imageWidth - 4, imageHeight - 25);
    }

    private static void drawInsetFrame(
            GuiGraphics graphics,
            int left,
            int top,
            int width,
            int height
    ) {
        // Nine-slice the Controller's empty status-bar frame. Preserve its
        // four-pixel corners at native resolution; stretch only plain edges.
        int[] sourceX = {2, 6, 168};
        int[] sourceY = {21, 25, 33};
        int[] sourceWidths = {4, 162, 4};
        int[] sourceHeights = {4, 8, 4};
        int[] targetWidths = {4, width - 8, 4};
        int[] targetHeights = {4, height - 8, 4};
        int y = top;
        for (int row = 0; row < 3; row++) {
            int x = left;
            for (int column = 0; column < 3; column++) {
                graphics.blit(
                        FRAME_TEXTURE, x, y,
                        targetWidths[column], targetHeights[row],
                        (float) sourceX[column], (float) sourceY[row],
                        sourceWidths[column], sourceHeights[row],
                        FoundryControllerUiLayout.WIDTH,
                        FoundryControllerUiLayout.HEIGHT
                );
                x += targetWidths[column];
            }
            y += targetHeights[row];
        }
    }

    private void renderTitle(
            GuiGraphics graphics
    ) {
        Component title =
                Component.literal("FAUCET LOCK");

        graphics.drawString(
                font,
                title,
                leftPos
                        + (imageWidth - font.width(title)) / 2,
                topPos + TITLE_Y,
                TEXT,
                false
        );
    }

    private void renderRows(
            GuiGraphics graphics,
            int mouseX,
            int mouseY
    ) {
        int totalRows =
                getTotalRows();

        for (
                int visibleRow = 0;
                visibleRow < getVisibleRows();
                visibleRow++
        ) {
            int rowIndex =
                    scrollOffset + visibleRow;

            if (rowIndex >= totalRows) {
                break;
            }

            int rowLeft =
                    leftPos + ROW_X;

            int rowTop =
                    topPos + ROW_Y
                            + visibleRow * ROW_STRIDE;

            renderRowBackground(
                    graphics,
                    rowLeft,
                    rowTop,
                    visibleRow,
                    rowIndex,
                    mouseX,
                    mouseY
            );

            if (rowIndex == 0) {
                renderDefaultRow(
                        graphics,
                        rowLeft,
                        rowTop
                );
                continue;
            }

            renderMetalRow(
                    graphics,
                    rowLeft,
                    rowTop,
                    rowIndex - 1
            );
        }
    }

    private void renderRowBackground(
            GuiGraphics graphics,
            int left,
            int top,
            int visibleRow,
            int absoluteRow,
            int mouseX,
            int mouseY
    ) {
        boolean selected =
                isRowSelected(absoluteRow);

        FoundryControllerListDrawing.drawRow(
                graphics,
                left,
                top,
                selected,
                true
        );

        boolean hovered = isMouseOverVisibleRow(
                mouseX,
                mouseY,
                visibleRow
        );
        if (hovered && !selected) {
            graphics.fill(
                    left + 2,
                    top + 1,
                    left + ROW_WIDTH - 2,
                    top + ROW_HEIGHT - 1,
                    HOVER_OVERLAY
            );
        }

        if (selected) {
            // Recolor the existing two-pixel inset strip, preserving the
            // authored row texture's checker pattern and dark outer border.
            for (int y = 1; y < ROW_HEIGHT - 1; y++) {
                for (int x = 1; x <= 2; x++) {
                    graphics.fill(
                            left + x, top + y, left + x + 1, top + y + 1,
                            (x + y) % 2 == 0 ? 0xFF4C361C : 0xFFB88432
                    );
                }
            }
        }
    }

    private void renderDefaultRow(
            GuiGraphics graphics,
            int left,
            int top
    ) {
        /*
         * Keep text neutral; the card highlight and amber inset show selection.
         */
        FoundryControllerMetalsRenderer.drawScaledListText(
                graphics,
                Component.literal("Default"),
                left + Math.round((ROW_WIDTH - font.width("Default")
                        * FoundryControllerMetalsRenderer.LIST_TEXT_SCALE) / 2.0F),
                top + 7,
                TEXT
        );
    }

    private void renderMetalRow(
            GuiGraphics graphics,
            int left,
            int top,
            int metalIndex
    ) {
        List<MoltenMetalDefinition> metals =
                menu.getMetals();

        if (
                metalIndex < 0
                        || metalIndex >= metals.size()
        ) {
            return;
        }

        MoltenMetalDefinition definition =
                metals.get(metalIndex);

        ItemStack icon =
                FoundryControllerMetalsRenderer.getDisplayIcon(
                        definition
                );

        if (!icon.isEmpty()) {
            graphics.renderItem(
                    icon,
                    left + 5,
                    top + 2
            );
        }

        String name =
                FoundryControllerMetalsRenderer.fitText(
                        font,
                        FoundryControllerMetalsRenderer.displayName(
                                definition.id()
                        ),
                        58
                );

        /*
         * Keep text neutral; the card highlight and amber inset show selection.
         */
        FoundryControllerMetalsRenderer.drawScaledListText(
                graphics,
                Component.literal(name),
                left + 25,
                top + 6,
                TEXT
        );
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        updateLayout();
        if (button == 0 && getTotalRows() > VISIBLE_ROWS
                && mouseX >= leftPos + SCROLL_X
                && mouseX < leftPos + SCROLL_X + SCROLL_WIDTH
                && mouseY >= getThumbTop()
                && mouseY < getThumbTop() + THUMB_HEIGHT) {
            draggingScrollbar = true;
            thumbGrabOffset = mouseY - getThumbTop();
            return true;
        }
        if (button == 0) {
            int clickedRow =
                    getClickedRow(
                            mouseX,
                            mouseY
                    );

            if (clickedRow >= 0) {
                int absoluteRow =
                        scrollOffset + clickedRow;

                if (absoluteRow < getTotalRows()) {
                    sendButton(
                            absoluteRow == 0
                                    ? FoundryFaucetOutputMenu
                                    .CLEAR_SELECTION_BUTTON
                                    : FoundryFaucetOutputMenu
                                    .SELECT_METAL_BUTTON_BASE
                                    + absoluteRow
                                    - 1
                    );

                    return true;
                }
            }
        }

        return super.mouseClicked(
                mouseX,
                mouseY,
                button
        );
    }

    @Override
    public boolean mouseDragged(
            double mouseX, double mouseY, int button,
            double dragX, double dragY
    ) {
        updateLayout();
        if (button == 0 && draggingScrollbar) {
            int maxOffset = getTotalRows() - VISIBLE_ROWS;
            int travel = getScrollHeight() - THUMB_HEIGHT;
            scrollOffset = (int) Math.round(
                    (mouseY - topPos - SCROLL_Y - thumbGrabOffset)
                            * maxOffset / travel
            );
            clampScroll();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingScrollbar) {
            draggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double scrollX,
            double scrollY
    ) {
        updateLayout();
        int maxOffset =
                Math.max(
                        0,
                        getTotalRows() - VISIBLE_ROWS
                );

        if (
                maxOffset <= 0
                        || !isMouseOverScrollableArea(
                        mouseX,
                        mouseY
                )
        ) {
            return super.mouseScrolled(
                    mouseX,
                    mouseY,
                    scrollX,
                    scrollY
            );
        }

        if (scrollY > 0.0D) {
            scrollOffset =
                    Math.max(
                            0,
                            scrollOffset - 1
                    );
        } else if (scrollY < 0.0D) {
            scrollOffset =
                    Math.min(
                            maxOffset,
                            scrollOffset + 1
                    );
        }

        return true;
    }

    private void renderRowTooltip(
            GuiGraphics graphics,
            int mouseX,
            int mouseY
    ) {
        int row =
                getClickedRow(
                        mouseX,
                        mouseY
                );

        if (row < 0) {
            return;
        }

        int absoluteRow =
                scrollOffset + row;

        if (absoluteRow >= getTotalRows()) {
            return;
        }

        Component tooltip =
                absoluteRow == 0
                        ? Component.literal(
                        "Lock Faucet to Controller output."
                )
                        : Component.literal("Lock Faucet to ").append(
                        MetalNames.name(
                                menu.getMetals().get(absoluteRow - 1).id()));

        graphics.renderTooltip(
                font,
                tooltip,
                mouseX,
                mouseY
        );
    }

    private boolean isRowSelected(
            int absoluteRow
    ) {
        if (absoluteRow == 0) {
            return menu.isAutomaticSelected();
        }

        int metalIndex =
                absoluteRow - 1;

        List<MoltenMetalDefinition> metals =
                menu.getMetals();

        return metalIndex >= 0
                && metalIndex < metals.size()
                && menu.isSelected(
                metals.get(metalIndex)
        );
    }

    private int getClickedRow(
            double mouseX,
            double mouseY
    ) {
        if (!isMouseOverRows(
                mouseX,
                mouseY
        )) {
            return -1;
        }

        int localY =
                (int) mouseY
                        - topPos
                        - ROW_Y;

        int visibleRow =
                localY / ROW_STRIDE;

        if (
                visibleRow < 0
                        || visibleRow >= getVisibleRows()
        ) {
            return -1;
        }

        int rowInnerY =
                localY % ROW_STRIDE;

        return rowInnerY < ROW_HEIGHT
                ? visibleRow
                : -1;
    }

    private boolean isMouseOverVisibleRow(
            double mouseX,
            double mouseY,
            int visibleRow
    ) {
        return getClickedRow(
                mouseX,
                mouseY
        ) == visibleRow;
    }

    private boolean isMouseOverRows(
            double mouseX,
            double mouseY
    ) {
        return mouseX >= leftPos + ROW_X
                && mouseX < leftPos + ROW_X + ROW_WIDTH
                && mouseY >= topPos + ROW_Y
                && mouseY < topPos + ROW_Y
                + getScrollHeight();
    }

    private boolean isMouseOverScrollableArea(
            double mouseX,
            double mouseY
    ) {
        return mouseX >= leftPos + ROW_X
                && mouseX < leftPos + SCROLL_X + SCROLL_WIDTH
                && mouseY >= topPos + ROW_Y
                && mouseY < topPos + ROW_Y
                + getScrollHeight();
    }

    private int getTotalRows() {
        return menu.getMetals().size() + 1;
    }

    private int getVisibleRows() {
        return Math.min(VISIBLE_ROWS, getTotalRows());
    }

    private int getScrollHeight() {
        return getVisibleRows() * ROW_STRIDE - 2;
    }

    private int getThumbTop() {
        int maxOffset = Math.max(0, getTotalRows() - VISIBLE_ROWS);
        return topPos + SCROLL_Y + (maxOffset == 0 ? 0
                : (getScrollHeight() - THUMB_HEIGHT) * scrollOffset / maxOffset);
    }

    private void updateLayout() {
        int newHeight = MAX_HEIGHT - (VISIBLE_ROWS - getVisibleRows()) * ROW_STRIDE;
        if (imageHeight != newHeight) {
            imageHeight = newHeight;
            draggingScrollbar = false;
        }
        leftPos = (width - imageWidth) / 2;
        topPos = (height - imageHeight) / 2;
        clampScroll();
        if (getTotalRows() <= VISIBLE_ROWS) {
            draggingScrollbar = false;
        }
    }

    private void clampScroll() {
        scrollOffset =
                Math.max(
                        0,
                        Math.min(
                                scrollOffset,
                                Math.max(
                                        0,
                                        getTotalRows() - VISIBLE_ROWS
                                )
                        )
                );
    }

    private void sendButton(
            int buttonId
    ) {
        if (
                minecraft == null
                        || minecraft.gameMode == null
        ) {
            return;
        }

        minecraft.gameMode.handleInventoryButtonClick(
                menu.containerId,
                buttonId
        );
    }
}
