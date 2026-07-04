package net.chriskatze.katzencraftmetals.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

final class FoundryControllerProcessPanelRenderer {

    private final FoundryControllerScreen screen;

    FoundryControllerProcessPanelRenderer(
            FoundryControllerScreen screen
    ) {
        this.screen =
                screen;
    }

    void render(
            GuiGraphics graphics
    ) {
        int left =
                screen.guiLeft();

        int top =
                screen.guiTop();

        Font font =
                screen.uiFont();

        graphics.drawString(
                font,
                Component.literal(
                        "PROCESS"
                ),
                left + 13,
                top + 64,
                FoundryControllerUiDrawing.TEXT,
                false
        );

        graphics.drawString(
                font,
                Component.literal(
                        "INPUT"
                ),
                left + 14,
                top + 76,
                FoundryControllerUiDrawing.MUTED_TEXT,
                false
        );

        graphics.drawString(
                font,
                Component.literal(
                        "HEAT"
                ),
                left + 60,
                top + 76,
                FoundryControllerUiDrawing.MUTED_TEXT,
                false
        );

        graphics.drawString(
                font,
                Component.literal(
                        "FUEL"
                ),
                left + 13,
                top + 115,
                FoundryControllerUiDrawing.MUTED_TEXT,
                false
        );

        graphics.drawString(
                font,
                Component.literal(
                        "BURN TIME"
                ),
                left + 13,
                top + 146,
                FoundryControllerUiDrawing.MUTED_TEXT,
                false
        );

        renderHeatGauge(
                graphics
        );

        renderFuelBar(
                graphics
        );
    }


    private void renderHeatGauge(
            GuiGraphics graphics
    ) {
        int left =
                screen.guiLeft()
                        + FoundryControllerUiLayout.HEAT_X;

        int top =
                screen.guiTop()
                        + FoundryControllerUiLayout.HEAT_Y;

        int heat =
                screen.controllerMenu()
                        .getScaledBurnTime(
                                FoundryControllerUiLayout.HEAT_HEIGHT
                                        - 2
                        );

        if (heat <= 0) {
            return;
        }

        int fillTop =
                top
                        + FoundryControllerUiLayout.HEAT_HEIGHT
                        - 1
                        - heat;

        graphics.fill(
                left + 1,
                fillTop,
                left + FoundryControllerUiLayout.HEAT_WIDTH - 1,
                top + FoundryControllerUiLayout.HEAT_HEIGHT - 1,
                FoundryControllerUiDrawing.ORANGE_DARK
        );

        int pulse =
                getAnimationTick()
                        % 4;

        graphics.fill(
                left + 2,
                Math.min(
                        top + FoundryControllerUiLayout.HEAT_HEIGHT - 2,
                        fillTop + pulse
                ),
                left + FoundryControllerUiLayout.HEAT_WIDTH - 2,
                top + FoundryControllerUiLayout.HEAT_HEIGHT - 2,
                FoundryControllerUiDrawing.ORANGE
        );
    }

    private void renderFuelBar(
            GuiGraphics graphics
    ) {
        int left =
                screen.guiLeft()
                        + FoundryControllerUiLayout.FUEL_BAR_X;

        int top =
                screen.guiTop()
                        + FoundryControllerUiLayout.FUEL_BAR_Y;

        int fuel =
                screen.controllerMenu()
                        .getScaledBurnTime(
                                FoundryControllerUiLayout.FUEL_BAR_WIDTH
                        );

        graphics.fill(
                left,
                top,
                left + fuel,
                top + FoundryControllerUiLayout.FUEL_BAR_HEIGHT,
                FoundryControllerUiDrawing.ORANGE
        );
    }

    private int getAnimationTick() {
        if (
                screen.minecraftClient() == null
                        || screen.minecraftClient().level == null
        ) {
            return 0;
        }

        return (int) (
                screen.minecraftClient()
                        .level
                        .getGameTime()
                        & Integer.MAX_VALUE
        );
    }
}
