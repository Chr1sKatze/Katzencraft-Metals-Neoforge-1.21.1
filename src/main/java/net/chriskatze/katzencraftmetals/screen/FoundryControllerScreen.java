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
        imageHeight = FoundryControllerUiLayout.HEIGHT;
    }

    @Override
    protected void init() {
        super.init();

        titleLabelX = -1000;
        inventoryLabelX = -1000;

        alloyQuantityBox =
                new EditBox(
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
        alloyQuantityBox.setTextColor(
                FoundryControllerUiDrawing.TEXT
        );

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
        graphics.blit(
                TEXTURE,
                leftPos,
                topPos,
                0.0f,
                0.0f,
                imageWidth,
                imageHeight,
                FoundryControllerUiLayout.WIDTH,
                FoundryControllerUiLayout.HEIGHT
        );

        overviewRenderer.render(
                graphics,
                mouseX,
                mouseY
        );
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
            alloyQuantityBox.setValue("");
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
