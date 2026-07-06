package net.chriskatze.katzencraftmetals.screen;

import net.chriskatze.katzencraftmetals.client.renderer.MoltenIronAnimation;
import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.chriskatze.katzencraftmetals.metal.MoltenMetalDefinition;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

final class FoundryControllerTankRenderer {

    private static final int MOLTEN_TEXTURE_WIDTH = 16;
    private static final int MOLTEN_FRAME_HEIGHT = 16;
    private static final int MOLTEN_TEXTURE_HEIGHT =
            MOLTEN_FRAME_HEIGHT
                    * MoltenIronAnimation.TEXTURE_FRAME_COUNT;

    /*
     * These bands were generated from foundry_controller_tank_mask.png.
     *
     * Each entry is:
     * { relativeYStart, relativeYEnd, relativeXStart, relativeXEnd }
     *
     * X/Y end values are exclusive. The bands allow the molten texture to be
     * rendered only where the white tank mask has opaque pixels, so the small
     * transparent notches/details inside the tank mask stay transparent.
     */
    private static final int[][] TANK_MASK_BANDS = {
            {0, 2, 0, 28},
            {2, 3, 0, 4},
            {2, 3, 5, 28},
            {3, 4, 0, 3},
            {3, 4, 4, 28},
            {4, 5, 0, 2},
            {4, 5, 3, 28},
            {5, 23, 0, 28},
            {23, 24, 0, 4},
            {23, 24, 24, 28},
            {23, 26, 5, 23},
            {25, 26, 0, 4},
            {25, 26, 24, 28},
            {26, 47, 0, 28},
            {47, 48, 0, 3},
            {47, 48, 25, 28},
            {47, 50, 4, 24},
            {49, 50, 0, 3},
            {49, 50, 25, 28},
            {50, 67, 0, 28},
            {67, 68, 0, 25},
            {67, 68, 26, 28},
            {68, 69, 0, 24},
            {68, 69, 25, 28},
            {69, 70, 0, 23},
            {69, 70, 24, 28},
            {70, 72, 0, 28}
    };

    private final FoundryControllerScreen screen;

    FoundryControllerTankRenderer(
            FoundryControllerScreen screen
    ) {
        this.screen = screen;
    }

    void render(
            GuiGraphics graphics
    ) {
        renderTankLayers(graphics);
        renderTankCount(graphics);
    }

    private void renderTankLayers(
            GuiGraphics graphics
    ) {
        int capacity =
                screen.controllerMenu()
                        .getTankCapacity();

        if (capacity <= 0) {
            return;
        }

        int x =
                screen.guiLeft()
                        + FoundryControllerUiLayout.TANK_GAUGE_X;

        int top =
                screen.guiTop()
                        + FoundryControllerUiLayout.TANK_GAUGE_Y;

        int width =
                FoundryControllerUiLayout.TANK_GAUGE_WIDTH;

        int height =
                FoundryControllerUiLayout.TANK_GAUGE_HEIGHT;

        int bottom = top + height;
        int accumulated = 0;

        for (
                MoltenMetalDefinition definition :
                ModMoltenMetals.heaviestFirst()
        ) {
            int amount =
                    screen.controllerMenu()
                            .getMetalAmount(definition);

            if (amount <= 0) {
                continue;
            }

            int lowerPixels =
                    accumulated
                            * height
                            / capacity;

            accumulated += amount;

            int upperPixels =
                    Math.max(
                            lowerPixels + 1,
                            accumulated
                                    * height
                                    / capacity
                    );

            upperPixels =
                    Math.min(
                            height,
                            upperPixels
                    );

            renderMoltenSegment(
                    graphics,
                    definition.animatedTexture(),
                    x,
                    width,
                    top,
                    bottom - upperPixels,
                    bottom - lowerPixels
            );
        }
    }

    private void renderMoltenSegment(
            GuiGraphics graphics,
            ResourceLocation texture,
            int tankLeft,
            int tankWidth,
            int tankTop,
            int segmentTop,
            int segmentBottom
    ) {
        if (
                tankWidth <= 0
                        || segmentBottom <= segmentTop
        ) {
            return;
        }

        for (int[] band : TANK_MASK_BANDS) {
            int bandTop =
                    tankTop
                            + band[0];

            int bandBottom =
                    tankTop
                            + band[1];

            int clippedTop =
                    Math.max(
                            segmentTop,
                            bandTop
                    );

            int clippedBottom =
                    Math.min(
                            segmentBottom,
                            bandBottom
                    );

            if (clippedBottom <= clippedTop) {
                continue;
            }

            int bandLeft =
                    tankLeft
                            + band[2];

            int bandRight =
                    tankLeft
                            + band[3];

            graphics.enableScissor(
                    bandLeft,
                    clippedTop,
                    bandRight,
                    clippedBottom
            );

            renderMoltenTiles(
                    graphics,
                    texture,
                    tankLeft,
                    tankWidth,
                    segmentTop,
                    segmentBottom
            );

            graphics.disableScissor();
        }
    }

    private void renderMoltenTiles(
            GuiGraphics graphics,
            ResourceLocation texture,
            int x,
            int width,
            int top,
            int bottom
    ) {
        int frame =
                MoltenIronAnimation.getFrame(
                        getGameTime()
                ).textureFrame();

        for (
                int tileX = x;
                tileX < x + width;
                tileX += MOLTEN_TEXTURE_WIDTH
        ) {
            for (
                    int tileY = top;
                    tileY < bottom;
                    tileY += MOLTEN_FRAME_HEIGHT
            ) {
                graphics.blit(
                        texture,
                        tileX,
                        tileY,
                        0.0f,
                        frame * MOLTEN_FRAME_HEIGHT,
                        MOLTEN_TEXTURE_WIDTH,
                        MOLTEN_FRAME_HEIGHT,
                        MOLTEN_TEXTURE_WIDTH,
                        MOLTEN_TEXTURE_HEIGHT
                );
            }
        }
    }

    private void renderTankCount(
            GuiGraphics graphics
    ) {
        Component count =
                Component.literal(
                        screen.controllerMenu()
                                .getTankCount()
                                + " / "
                                + screen.controllerMenu()
                                .getMaximumTankCount()
                );

        FoundryControllerUiDrawing.drawCentered(
                graphics,
                screen.uiFont(),
                count,
                screen.guiLeft()
                        + FoundryControllerUiLayout.TANK_COUNT_X,
                screen.guiTop()
                        + FoundryControllerUiLayout.TANK_COUNT_Y,
                FoundryControllerUiLayout.TANK_COUNT_WIDTH,
                FoundryControllerUiLayout.TANK_COUNT_HEIGHT,
                FoundryControllerUiDrawing.TEXT
        );
    }

    private long getGameTime() {
        if (
                screen.minecraftClient() == null
                        || screen.minecraftClient().level == null
        ) {
            return 0L;
        }

        return screen.minecraftClient()
                .level
                .getGameTime();
    }
}
