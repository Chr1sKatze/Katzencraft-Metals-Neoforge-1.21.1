package net.chriskatze.katzencraftmetals.screen;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.menu.FoundryControllerMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

public class FoundryControllerScreen
        extends AbstractContainerScreen<FoundryControllerMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    KatzencraftMetalsMod.MODID,
                    "textures/gui/foundry_controller.png"
            );

    private final FoundryControllerOverviewRenderer overviewRenderer =
            new FoundryControllerOverviewRenderer(this);

    private EditBox alloyQuantityBox;
    private int alloyQuantityLimit = 1;
    private boolean alloyQuantityEnabled;

    public FoundryControllerScreen(
            FoundryControllerMenu menu,
            Inventory playerInventory,
            Component title
    ) {
        super(
                menu,
                playerInventory,
                title
        );

        imageWidth = FoundryControllerUiLayout.WIDTH;
        imageHeight = FoundryControllerUiLayout.GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();

        titleLabelX = -1000;
        inventoryLabelX = -1000;

        alloyQuantityBox =
                new FoundryQuantityEditBox(
                        font,
                        leftPos
                                + FoundryControllerUiLayout.QUANTITY_EDITBAR_X,
                        topPos
                                + FoundryControllerUiLayout.QUANTITY_TEXT_Y,
                        FoundryControllerUiLayout.QUANTITY_EDITBAR_WIDTH,
                        FoundryControllerUiLayout.QUANTITY_TEXT_HEIGHT,
                        Component.literal(
                                "Alloy batch amount"
                        )
                );

        alloyQuantityBox.setBordered(false);
        alloyQuantityBox.setMaxLength(2);
        alloyQuantityBox.setValue("");
        alloyQuantityBox.setTextColor(FoundryControllerUiDrawing.HEADING_TEXT);
        alloyQuantityBox.setTextColorUneditable(FoundryControllerUiDrawing.HEADING_TEXT);
        alloyQuantityBox.setTextShadow(false);

        alloyQuantityBox.setFilter(
                value -> {
                    if (value.isEmpty()) {
                        return true;
                    }

                    try {
                        int parsed =
                                Integer.parseInt(value);

                        return parsed >= 1
                                && parsed
                                <= FoundryControllerMenu.MAX_ALLOY_BATCHES;
                    } catch (NumberFormatException ignored) {
                        return false;
                    }
                }
        );

        addRenderableWidget(alloyQuantityBox);
        centerAlloyQuantityText();
    }

    @Override
    protected void renderBg(
            GuiGraphics graphics,
            float partialTick,
            int mouseX,
            int mouseY
    ) {
        FoundryControllerListDrawing.drawOuterFrame(
                graphics, leftPos, topPos, imageWidth, imageHeight
        );
        // The atlas perimeter contains old inset-panel junctions. Stretching
        // it repeats those edges between recomposed panels; use its flat base.
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF3B3B3B);

        renderExpandedPanels(graphics);
        overviewRenderer.render(
                graphics,
                mouseX,
                mouseY
        );
    }

    private void renderExpandedPanels(GuiGraphics graphics) {
        // Preserve the native inventory artwork and slot spacing, translating
        // the entire lower section by the same amount as its menu hitboxes.
        int panelTop = FoundryControllerUiLayout.PANEL_TOP;
        int panelHeight = FoundryControllerUiLayout.PANEL_HEIGHT;
        drawTextureRegion(graphics, 0, 145, 174, 87, 0, FoundryControllerUiLayout.PANEL_BOTTOM);
        graphics.fill(leftPos + 2, topPos + 17, leftPos + 172,
                topPos + FoundryControllerUiLayout.PANEL_BOTTOM, 0xFF3B3B3B);
        drawPanelFrame(graphics, 2, 39, 118, 106, 2, panelTop, 118, panelHeight);
        drawPanelFrame(graphics, 122, 39, 50, 106, 122, panelTop, 50, panelHeight);
        drawPanelFrame(graphics, 174, 21, 106, 209, 174, 2, 106, imageHeight - 4);

        // The input/fuel/burn cluster moves as a unit. Its first visible slot
        // frame and the thermometer now share the section-relative content edge.
        int processOffsetX = FoundryControllerUiLayout.PROCESS_CLUSTER_OFFSET_X;
        int processOffsetY = FoundryControllerUiLayout.PROCESS_CLUSTER_OFFSET_Y;
        drawTextureRegion(graphics, 13, 55, 103, 86, 13 + processOffsetX, 55 + processOffsetY);
        graphics.fill(leftPos + 13 + processOffsetX, topPos + 55 + processOffsetY,
                leftPos + 28 + processOffsetX, topPos + 129 + processOffsetY, 0xFF232323);
        for (int y = 0; y < FoundryControllerUiLayout.PROGRESS_GAUGE_HEIGHT; y++) {
            drawTextureRegion(graphics, 14, 56 + FoundryControllerUiLayout.processMaskSourceY(y),
                    13, 1, FoundryControllerUiLayout.PROGRESS_GAUGE_X,
                    FoundryControllerUiLayout.PROGRESS_GAUGE_Y + y);
        }

        // Frame cap and base stay native-sized; the interior follows the shared label edge.
        drawTextureRegion(graphics, 132, 55, 30, 1, 132, FoundryControllerUiLayout.TANK_FRAME_Y);
        for (int y = 0; y < FoundryControllerUiLayout.TANK_GAUGE_HEIGHT; y++) {
            int sourceY = ((y + 1) * 72 - 1) / FoundryControllerUiLayout.TANK_GAUGE_HEIGHT;
            drawTextureRegion(graphics, 132, 56 + sourceY, 30, 1, 132,
                    FoundryControllerUiLayout.TANK_GAUGE_Y + y);
        }
        drawTextureRegion(graphics, 132, 128, 30, 1, 132,
                FoundryControllerUiLayout.TANK_GAUGE_Y + FoundryControllerUiLayout.TANK_GAUGE_HEIGHT);

        drawHeading(graphics, "PROCESS", FoundryControllerUiLayout.PROCESS_UNDERLINE_X,
                FoundryControllerUiLayout.PROCESS_UNDERLINE_WIDTH, FoundryControllerUiLayout.PROCESS_HEADING_Y);
        drawHeading(graphics, "TANK", FoundryControllerUiLayout.TANK_UNDERLINE_X,
                FoundryControllerUiLayout.TANK_UNDERLINE_WIDTH, FoundryControllerUiLayout.TANK_HEADING_Y);
        drawHeading(graphics, "METAL CONTENT", 179, 94, FoundryControllerUiLayout.METAL_HEADING_Y);
        drawHeading(graphics, "ALLOY RECIPES", 179, 94, FoundryControllerUiLayout.ALLOY_HEADING_Y);
        FoundryControllerUiDrawing.drawClock(graphics, leftPos + FoundryControllerUiLayout.CLOCK_X,
                topPos + FoundryControllerUiLayout.CLOCK_Y);
        drawTextureRegion(graphics, 40, 51, FoundryControllerUiLayout.PROCESS_UNDERLINE_WIDTH, 1,
                FoundryControllerUiLayout.PROCESS_UNDERLINE_X, FoundryControllerUiLayout.PROCESS_UNDERLINE_Y);
        drawTextureRegion(graphics, 127, 51, FoundryControllerUiLayout.TANK_UNDERLINE_WIDTH, 1,
                FoundryControllerUiLayout.TANK_UNDERLINE_X, FoundryControllerUiLayout.TANK_UNDERLINE_Y);
        drawTextureRegion(graphics, 179, 33, 94, 1, 179, FoundryControllerUiLayout.METAL_UNDERLINE_Y);
        drawTextureRegion(graphics, 179, 133, 94, 1, 179, FoundryControllerUiLayout.ALLOY_UNDERLINE_Y);
        drawTextureRegion(graphics, 179, 183, 96, 19, 179, FoundryControllerUiLayout.ALLOY_RECIPE_SLOT_Y - 1);
        drawTextureRegion(graphics, 179, 207, 96, 15, 179, FoundryControllerUiLayout.QUANTITY_BUTTON_Y);

        // Faucet Lock's exact inset frame source and four-pixel corner slices,
        // adapted to the existing Controller title area (not its right column).
        drawPanelFrame(graphics, 2, 21, 170, 16, 2, 2, 170, FoundryControllerUiLayout.HEADER_HEIGHT);
        Component controllerTitle = Component.literal("FOUNDRY CONTROLLER");
        graphics.drawString(font, controllerTitle,
                leftPos + 2 + (170 - font.width(controllerTitle)) / 2,
                topPos + 2 + (FoundryControllerUiLayout.HEADER_HEIGHT - 7) / 2,
                FoundryControllerUiDrawing.HEADING_TEXT, false);

        // Replace only the baked label, keeping its glyph top aligned with
        // the tank count and its center on the processing slot/bar group.
        graphics.fill(leftPos + FoundryControllerUiLayout.PROCESS_UNDERLINE_X,
                topPos + FoundryControllerUiLayout.BURN_LABEL_Y,
                leftPos + FoundryControllerUiLayout.PROCESS_UNDERLINE_X
                        + FoundryControllerUiLayout.PROCESS_UNDERLINE_WIDTH,
                topPos + FoundryControllerUiLayout.BURN_LABEL_Y + 9, 0xFF232323);
        Component fuelTimer = Component.literal("FUEL TIMER");
        graphics.drawString(font, fuelTimer,
                leftPos + FoundryControllerUiLayout.BURN_BAR_X
                        + (FoundryControllerUiLayout.BURN_BAR_WIDTH - font.width(fuelTimer)) / 2,
                topPos + FoundryControllerUiLayout.BURN_LABEL_Y,
                FoundryControllerUiDrawing.HEADING_TEXT, false);
    }

    private void drawHeading(GuiGraphics graphics, String text, int x, int width, int y) {
        graphics.drawString(font, text, leftPos + x + (width - font.width(text)) / 2,
                topPos + y, FoundryControllerUiDrawing.HEADING_TEXT, false);
    }

    private void drawPanelFrame(
            GuiGraphics graphics, int sourceX, int sourceY, int sourceWidth, int sourceHeight,
            int x, int y, int width, int height
    ) {
        graphics.fill(leftPos + x, topPos + y, leftPos + x + width, topPos + y + height, 0xFF232323);
        int[] sourceXs = {sourceX, sourceX + 4, sourceX + sourceWidth - 4};
        int[] sourceYs = {sourceY, sourceY + 4, sourceY + sourceHeight - 4};
        int[] sourceWidths = {4, sourceWidth - 8, 4};
        int[] sourceHeights = {4, sourceHeight - 8, 4};
        int[] widths = {4, width - 8, 4};
        int[] heights = {4, height - 8, 4};
        int destY = topPos + y;
        for (int row = 0; row < 3; row++) {
            int destX = leftPos + x;
            for (int column = 0; column < 3; column++) {
                if (row != 1 || column != 1) {
                    graphics.blit(TEXTURE, destX, destY, widths[column], heights[row],
                            (float) sourceXs[column], (float) sourceYs[row],
                            sourceWidths[column], sourceHeights[row],
                            FoundryControllerUiLayout.WIDTH, FoundryControllerUiLayout.HEIGHT);
                }
                destX += widths[column];
            }
            destY += heights[row];
        }
    }

    private void drawTextureRegion(
            GuiGraphics graphics, int sourceX, int sourceY, int width, int height, int x, int y
    ) {
        graphics.blit(TEXTURE, leftPos + x, topPos + y, (float) sourceX, (float) sourceY,
                width, height, FoundryControllerUiLayout.WIDTH, FoundryControllerUiLayout.HEIGHT);
    }

    @Override
    protected void renderLabels(
            GuiGraphics graphics,
            int mouseX,
            int mouseY
    ) {
        /* All labels belong to the authored texture or section renderers. */
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        boolean overQuantityEditBar =
                button == 0
                        && alloyQuantityBox != null
                        && alloyQuantityEnabled
                        && FoundryControllerUiLayout.contains(
                        mouseX,
                        mouseY,
                        leftPos,
                        topPos,
                        FoundryControllerUiLayout.QUANTITY_EDITBAR_X,
                        FoundryControllerUiLayout.QUANTITY_EDITBAR_Y,
                        FoundryControllerUiLayout.QUANTITY_EDITBAR_WIDTH,
                        FoundryControllerUiLayout.QUANTITY_EDITBAR_HEIGHT
                );

        if (overQuantityEditBar) {
            setFocused(alloyQuantityBox);
            alloyQuantityBox.setFocused(true);

            /*
             * Select the complete current value. Typing "99" therefore
             * replaces "1" instead of trying to append to it as "199".
             */
            alloyQuantityBox.setCursorPosition(
                    alloyQuantityBox.getValue().length()
            );
            alloyQuantityBox.setHighlightPos(0);
            return true;
        }

        /*
         * Commit before forwarding the click. This means START receives the
         * clamped amount, and +/- operate on the clamped value first.
         */
        if (
                button == 0
                        && alloyQuantityBox != null
                        && alloyQuantityBox.isFocused()
        ) {
            commitAlloyQuantity();
            stopEditingAlloyQuantity();
        }

        if (
                overviewRenderer.mouseClicked(
                        mouseX,
                        mouseY,
                        button
                )
        ) {
            return true;
        }

        return super.mouseClicked(
                mouseX,
                mouseY,
                button
        );
    }

    @Override
    public boolean charTyped(
            char codePoint,
            int modifiers
    ) {
        if (
                alloyQuantityBox != null
                        && alloyQuantityEnabled
                        && alloyQuantityBox.isFocused()
                        && alloyQuantityBox.charTyped(
                        codePoint,
                        modifiers
                )
        ) {
            centerAlloyQuantityText();
            return true;
        }

        return super.charTyped(
                codePoint,
                modifiers
        );
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return overviewRenderer.mouseDragged(mouseY, button)
                || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean handled = overviewRenderer.mouseReleased(button);
        return super.mouseReleased(mouseX, mouseY, button) || handled;
    }

    @Override
    public boolean keyPressed(
            int keyCode,
            int scanCode,
            int modifiers
    ) {
        if (
                alloyQuantityBox != null
                        && alloyQuantityEnabled
                        && alloyQuantityBox.isFocused()
        ) {
            /*
             * GLFW_KEY_ENTER = 257
             * GLFW_KEY_KP_ENTER = 335
             */
            if (keyCode == 257 || keyCode == 335) {
                commitAlloyQuantity();
                stopEditingAlloyQuantity();
                return true;
            }

            if (
                    alloyQuantityBox.keyPressed(
                            keyCode,
                            scanCode,
                            modifiers
                    )
            ) {
                centerAlloyQuantityText();
                return true;
            }
        }

        return super.keyPressed(
                keyCode,
                scanCode,
                modifiers
        );
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double scrollX,
            double scrollY
    ) {
        if (
                overviewRenderer.mouseScrolled(
                        mouseX,
                        mouseY,
                        scrollY
                )
        ) {
            return true;
        }

        return super.mouseScrolled(
                mouseX,
                mouseY,
                scrollX,
                scrollY
        );
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        centerAlloyQuantityText();

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

        overviewRenderer.renderTooltips(
                graphics,
                mouseX,
                mouseY
        );
    }

    int guiLeft() {
        return leftPos;
    }

    int guiTop() {
        return topPos;
    }

    Font uiFont() {
        return font;
    }

    Minecraft minecraftClient() {
        return minecraft;
    }

    FoundryControllerMenu controllerMenu() {
        return menu;
    }

    int getAlloyQuantity() {
        if (
                alloyQuantityBox == null
                        || alloyQuantityBox.getValue().isEmpty()
        ) {
            return 1;
        }

        try {
            return Math.max(
                    1,
                    Math.min(
                            alloyQuantityLimit,
                            Integer.parseInt(
                                    alloyQuantityBox.getValue()
                            )
                    )
            );
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    void configureAlloyQuantity(
            boolean enabled,
            int maximum,
            boolean reset
    ) {
        if (alloyQuantityBox == null) {
            return;
        }

        alloyQuantityLimit =
                Math.max(
                        1,
                        Math.min(
                                FoundryControllerMenu.MAX_ALLOY_BATCHES,
                                maximum
                        )
                );

        alloyQuantityEnabled = enabled;
        alloyQuantityBox.setEditable(enabled);

        if (!enabled) {
            int activeBatchCount =
                    menu.isAlloyJobActive()
                            ? menu.getActiveAlloyBatchCount()
                            : 0;

            alloyQuantityBox.setValue(
                    activeBatchCount > 0
                            ? Integer.toString(activeBatchCount)
                            : ""
            );

            if (activeBatchCount > 0) {
                alloyQuantityLimit =
                        Math.max(
                                1,
                                Math.min(
                                        FoundryControllerMenu.MAX_ALLOY_BATCHES,
                                        activeBatchCount
                                )
                        );
            }

            stopEditingAlloyQuantity();
            centerAlloyQuantityText();
            return;
        }

        if (reset) {
            alloyQuantityBox.setValue("1");
            stopEditingAlloyQuantity();
            centerAlloyQuantityText();
            return;
        }

        /*
         * configureAlloyQuantity() runs during normal rendering.
         * Preserve the raw text while the user is editing. For example,
         * "99" must remain visible even when only 24 batches are possible.
         */
        if (alloyQuantityBox.isFocused()) {
            centerAlloyQuantityText();
            return;
        }

        /*
         * Outside edit mode, keep the displayed amount valid if available
         * materials or tank space changed.
         */
        commitAlloyQuantity();
    }

    void changeAlloyQuantity(
            int delta
    ) {
        if (
                alloyQuantityBox == null
                        || !alloyQuantityEnabled
        ) {
            return;
        }

        /*
         * getAlloyQuantity() clamps the raw typed value first.
         * Example with a limit of 24:
         *   raw 99 + minus -> 24 -> 23
         *   raw 99 + plus  -> 24 -> 1
         */
        int current = getAlloyQuantity();
        int next = current;

        if (delta < 0) {
            next =
                    current <= 1
                            ? alloyQuantityLimit
                            : current - 1;
        } else if (delta > 0) {
            next =
                    current >= alloyQuantityLimit
                            ? 1
                            : current + 1;
        }

        alloyQuantityBox.setValue(
                Integer.toString(next)
        );

        stopEditingAlloyQuantity();
        centerAlloyQuantityText();
    }

    void playButtonClickSound() {
        if (minecraft == null) {
            return;
        }

        minecraft.getSoundManager().play(
                SimpleSoundInstance.forUI(
                        SoundEvents.UI_BUTTON_CLICK,
                        1.0F
                )
        );
    }

    boolean sendMenuButton(
            int buttonId
    ) {
        if (
                minecraft == null
                        || minecraft.gameMode == null
        ) {
            return false;
        }

        minecraft.gameMode.handleInventoryButtonClick(
                menu.containerId,
                buttonId
        );

        return true;
    }

    private void commitAlloyQuantity() {
        if (alloyQuantityBox == null) {
            return;
        }

        alloyQuantityBox.setValue(
                Integer.toString(
                        getAlloyQuantity()
                )
        );

        centerAlloyQuantityText();
    }

    private void stopEditingAlloyQuantity() {
        if (alloyQuantityBox == null) {
            return;
        }

        alloyQuantityBox.setFocused(false);

        if (getFocused() == alloyQuantityBox) {
            setFocused(null);
        }
    }

    private void centerAlloyQuantityText() {
        if (alloyQuantityBox == null) {
            return;
        }

        String value = alloyQuantityBox.getValue();
        int textWidth = font.width(value);

        int editBarLeft =
                leftPos
                        + FoundryControllerUiLayout.QUANTITY_EDITBAR_X;

        int editBarRight =
                editBarLeft
                        + FoundryControllerUiLayout.QUANTITY_EDITBAR_WIDTH;

        int centeredX =
                editBarLeft
                        + (
                        FoundryControllerUiLayout.QUANTITY_EDITBAR_WIDTH
                                - textWidth
                ) / 2;

        alloyQuantityBox.setX(centeredX);
        alloyQuantityBox.setY(
                topPos
                        + FoundryControllerUiLayout.QUANTITY_TEXT_Y
        );

        /*
         * Keep enough editable width for a second digit and the cursor.
         * Making the widget only as wide as the current text caused EditBox
         * to scroll horizontally as soon as the value reached 10.
         */
        alloyQuantityBox.setWidth(
                Math.max(
                        1,
                        editBarRight - centeredX
                )
        );
    }
}
