package net.chriskatze.katzencraftmetals.screen;

import net.chriskatze.katzencraftmetals.menu.FoundryFaucetOutputMenu;
import net.chriskatze.katzencraftmetals.metal.MoltenMetalDefinition;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class FoundryFaucetOutputScreen
        extends AbstractContainerScreen<FoundryFaucetOutputMenu> {

    private static final int PANEL = 0xFF171717;
    private static final int PANEL_INNER = 0xFF0F0F0F;
    private static final int BORDER_LIGHT = 0xFF2F2F2F;
    private static final int BORDER_DARK = 0xFF050505;
    private static final int HOVER_OVERLAY = 0x18FFFFFF;
    private static final int TEXT = 0xFFE8E8E4;
    private static final int MUTED_TEXT = 0xFF9A9A9A;
    private static final int GREEN = 0xFF86B85C;

    private static final int ROW_X = 8;
    private static final int ROW_Y = 22;
    private static final int ROW_WIDTH = 86;
    private static final int ROW_HEIGHT = 19;
    private static final int ROW_STRIDE = 21;
    private static final int VISIBLE_ROWS = 5;

    private static final int SCROLL_X = 99;
    private static final int SCROLL_Y = ROW_Y;
    private static final int SCROLL_HEIGHT =
            VISIBLE_ROWS * ROW_STRIDE - 2;

    private static final int TITLE_X = 8;
    private static final int TITLE_Y = 7;

    private int scrollOffset;

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

        imageWidth = 113;
        imageHeight = 133;
    }

    @Override
    protected void init() {
        super.init();

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

        graphics.drawString(
                font,
                Component.literal("FAUCET LOCK"),
                leftPos + TITLE_X,
                topPos + TITLE_Y,
                TEXT,
                false
        );

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
                    SCROLL_HEIGHT,
                    getTotalRows(),
                    VISIBLE_ROWS,
                    scrollOffset
            );
        }
    }

    private void renderPanel(
            GuiGraphics graphics
    ) {
        graphics.fill(
                leftPos,
                topPos,
                leftPos + imageWidth,
                topPos + imageHeight,
                PANEL
        );

        graphics.fill(
                leftPos + 1,
                topPos + 1,
                leftPos + imageWidth - 1,
                topPos + imageHeight - 1,
                PANEL_INNER
        );

        graphics.fill(
                leftPos,
                topPos,
                leftPos + imageWidth,
                topPos + 1,
                BORDER_LIGHT
        );

        graphics.fill(
                leftPos,
                topPos,
                leftPos + 1,
                topPos + imageHeight,
                BORDER_LIGHT
        );

        graphics.fill(
                leftPos,
                topPos + imageHeight - 1,
                leftPos + imageWidth,
                topPos + imageHeight,
                BORDER_DARK
        );

        graphics.fill(
                leftPos + imageWidth - 1,
                topPos,
                leftPos + imageWidth,
                topPos + imageHeight,
                BORDER_DARK
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
                visibleRow < VISIBLE_ROWS;
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
                renderAutomaticRow(
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

        if (
                !selected
                        && isMouseOverVisibleRow(
                        mouseX,
                        mouseY,
                        visibleRow
                )
        ) {
            graphics.fill(
                    left + 2,
                    top + 1,
                    left + ROW_WIDTH - 2,
                    top + ROW_HEIGHT - 1,
                    HOVER_OVERLAY
            );
        }
    }

    private void renderAutomaticRow(
            GuiGraphics graphics,
            int left,
            int top
    ) {
        int color =
                menu.isAutomaticSelected()
                        ? GREEN
                        : TEXT;

        FoundryControllerMetalsRenderer.drawScaledListText(
                graphics,
                Component.literal("Follow output"),
                left + 6,
                top + 6,
                color
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

        FoundryControllerMetalsRenderer.drawScaledListText(
                graphics,
                Component.literal(name),
                left + 25,
                top + 6,
                menu.isSelected(definition)
                        ? GREEN
                        : TEXT
        );
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
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
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double scrollX,
            double scrollY
    ) {
        int maxOffset =
                Math.max(
                        0,
                        getTotalRows() - VISIBLE_ROWS
                );

        if (
                maxOffset <= 0
                        || !isMouseOverList(
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
                        "Unlock Faucet: follow the Controller pouring output."
                )
                        : Component.literal(
                        "Lock Faucet to "
                                + FoundryControllerMetalsRenderer
                                .displayName(
                                        menu.getMetals()
                                                .get(absoluteRow - 1)
                                                .id()
                                )
                );

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
        if (!isMouseOverList(
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
                        || visibleRow >= VISIBLE_ROWS
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

    private boolean isMouseOverList(
            double mouseX,
            double mouseY
    ) {
        return mouseX >= leftPos + ROW_X
                && mouseX < leftPos + ROW_X + ROW_WIDTH
                && mouseY >= topPos + ROW_Y
                && mouseY < topPos + ROW_Y
                + VISIBLE_ROWS * ROW_STRIDE;
    }

    private int getTotalRows() {
        return menu.getMetals().size() + 1;
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
