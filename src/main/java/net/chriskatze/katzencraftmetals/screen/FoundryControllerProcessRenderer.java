package net.chriskatze.katzencraftmetals.screen;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

final class FoundryControllerProcessRenderer {

    private static final ResourceLocation UNLOCKED_SLOT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    KatzencraftMetalsMod.MODID,
                    "textures/gui/foundry_controller_unlocked_slot.png"
            );

    private final FoundryControllerScreen screen;

    FoundryControllerProcessRenderer(
            FoundryControllerScreen screen
    ) {
        this.screen = screen;
    }

    void render(
            GuiGraphics graphics
    ) {
        renderUnlockedSlotFrames(graphics);
        renderActiveInputOutline(graphics);
        renderBurnTime(graphics);
        renderProcessProgress(graphics);
    }

    private void renderUnlockedSlotFrames(
            GuiGraphics graphics
    ) {
        for (
                int slot = 0;
                slot < screen.controllerMenu()
                        .getUnlockedInputSlotCount();
                slot++
        ) {
            graphics.blit(
                    UNLOCKED_SLOT_TEXTURE,
                    screen.guiLeft()
                            + FoundryControllerUiLayout.INPUT_SLOT_X[slot]
                            - 1,
                    screen.guiTop()
                            + FoundryControllerUiLayout.INPUT_SLOT_Y[slot]
                            - 1,
                    0.0f,
                    0.0f,
                    18,
                    18,
                    18,
                    18
            );
        }

        for (
                int slot = 0;
                slot < screen.controllerMenu()
                        .getUnlockedFuelSlotCount();
                slot++
        ) {
            graphics.blit(
                    UNLOCKED_SLOT_TEXTURE,
                    screen.guiLeft()
                            + FoundryControllerUiLayout.FUEL_SLOT_START_X
                            + slot * FoundryControllerUiLayout.SLOT_SPACING
                            - 1,
                    screen.guiTop()
                            + FoundryControllerUiLayout.FUEL_SLOT_Y
                            - 1,
                    0.0f,
                    0.0f,
                    18,
                    18,
                    18,
                    18
            );
        }
    }

    private void renderActiveInputOutline(
            GuiGraphics graphics
    ) {
        int activeSlot =
                screen.controllerMenu()
                        .getActiveInputSlot();

        if (
                activeSlot < 0
                        || activeSlot
                        >= FoundryControllerUiLayout.INPUT_SLOT_X.length
        ) {
            return;
        }

        int left =
                screen.guiLeft()
                        + FoundryControllerUiLayout.INPUT_SLOT_X[activeSlot]
                        - 1;

        int top =
                screen.guiTop()
                        + FoundryControllerUiLayout.INPUT_SLOT_Y[activeSlot]
                        - 1;

        graphics.fill(
                left,
                top,
                left + 18,
                top + 1,
                FoundryControllerUiDrawing.ORANGE
        );

        graphics.fill(
                left,
                top,
                left + 1,
                top + 18,
                FoundryControllerUiDrawing.ORANGE
        );

        graphics.fill(
                left + 17,
                top,
                left + 18,
                top + 18,
                FoundryControllerUiDrawing.ORANGE_DARK
        );

        graphics.fill(
                left,
                top + 17,
                left + 18,
                top + 18,
                FoundryControllerUiDrawing.ORANGE_DARK
        );
    }

    private void renderBurnTime(
            GuiGraphics graphics
    ) {
        int fill =
                screen.controllerMenu()
                        .getScaledBurnTime(
                                FoundryControllerUiLayout.BURN_BAR_WIDTH
                        );

        if (fill <= 0) {
            return;
        }

        int left =
                screen.guiLeft()
                        + FoundryControllerUiLayout.BURN_BAR_X;

        int top =
                screen.guiTop()
                        + FoundryControllerUiLayout.BURN_BAR_Y;

        graphics.fill(
                left,
                top,
                left + fill,
                top + FoundryControllerUiLayout.BURN_BAR_HEIGHT,
                FoundryControllerUiDrawing.ORANGE
        );
    }

    private void renderProcessProgress(
            GuiGraphics graphics
    ) {
        int fill =
                screen.controllerMenu()
                        .getScaledProgress(
                                FoundryControllerUiLayout.PROGRESS_GAUGE_HEIGHT
                        );

        if (fill <= 0) {
            return;
        }

        int left =
                screen.guiLeft()
                        + FoundryControllerUiLayout.PROGRESS_GAUGE_X;

        int bottom =
                screen.guiTop()
                        + FoundryControllerUiLayout.PROGRESS_GAUGE_Y
                        + FoundryControllerUiLayout.PROGRESS_GAUGE_HEIGHT;

        int top = bottom - fill;

        graphics.fill(
                left,
                top,
                left + FoundryControllerUiLayout.PROGRESS_GAUGE_WIDTH,
                bottom,
                FoundryControllerUiDrawing.ORANGE
        );

        if (fill > 2) {
            graphics.fill(
                    left + 1,
                    top,
                    left + FoundryControllerUiLayout.PROGRESS_GAUGE_WIDTH - 1,
                    top + 1,
                    0xFFFFC34A
            );
        }
    }
}
