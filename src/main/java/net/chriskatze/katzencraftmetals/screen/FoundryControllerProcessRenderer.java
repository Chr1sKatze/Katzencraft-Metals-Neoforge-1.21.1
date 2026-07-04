package net.chriskatze.katzencraftmetals.screen;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
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
        renderTemperature(graphics);
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

    private void renderTemperature(
            GuiGraphics graphics
    ) {
        int fill = screen.controllerMenu().getScaledTemperature(
                FoundryControllerUiLayout.TEMPERATURE_GAUGE_HEIGHT
        );

        int left = screen.guiLeft() + FoundryControllerUiLayout.TEMPERATURE_GAUGE_X;
        int bottom = screen.guiTop()
                + FoundryControllerUiLayout.TEMPERATURE_GAUGE_Y
                + FoundryControllerUiLayout.TEMPERATURE_GAUGE_HEIGHT;

        if (fill > 0) {
            int top = bottom - fill;
            graphics.fill(
                    left,
                    top,
                    left + FoundryControllerUiLayout.TEMPERATURE_GAUGE_WIDTH,
                    bottom,
                    getTemperatureColor()
            );
        }

        Component temperature = Component.literal(
                screen.controllerMenu().getCurrentTemperature() + "°C"
        );

        FoundryControllerUiDrawing.drawCentered(
                graphics,
                screen.uiFont(),
                temperature,
                screen.guiLeft() + 2,
                screen.guiTop() + 152,
                34,
                12,
                FoundryControllerUiDrawing.TEXT
        );
    }

    private int getTemperatureColor() {
        int current = screen.controllerMenu().getCurrentTemperature();
        int maximum = screen.controllerMenu().getMaximumTemperature();
        float ratio = maximum <= 0 ? 0.0f : Math.min(1.0f, current / (float) maximum);

        if (ratio < 0.33f) {
            return 0xFFCC5A24;
        }
        if (ratio < 0.66f) {
            return 0xFFFF941F;
        }
        return 0xFFFFC34A;
    }

}
