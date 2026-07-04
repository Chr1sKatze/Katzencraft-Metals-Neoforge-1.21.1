package net.chriskatze.katzencraftmetals.screen;

import net.chriskatze.katzencraftmetals.client.renderer.MoltenIronAnimation;
import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.chriskatze.katzencraftmetals.metal.MoltenMetalDefinition;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

final class FoundryControllerTankPanelRenderer {

    private static final int MOLTEN_TEXTURE_WIDTH = 16;
    private static final int MOLTEN_FRAME_HEIGHT = 16;
    private static final int MOLTEN_TEXTURE_HEIGHT =
            MOLTEN_FRAME_HEIGHT
                    * MoltenIronAnimation.TEXTURE_FRAME_COUNT;

    private final FoundryControllerScreen screen;

    FoundryControllerTankPanelRenderer(
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
                        "TANK"
                ),
                left + 94,
                top + 64,
                FoundryControllerUiDrawing.TEXT,
                false
        );

        renderTankLayers(
                graphics
        );

        int tankCount =
                screen.controllerMenu()
                        .getTankCount();

        FoundryControllerUiDrawing.drawCentered(
                graphics,
                font,
                Component.literal(
                        tankCount
                                + (
                                tankCount == 1
                                        ? " Tank"
                                        : " Tanks"
                        )
                ),
                left + FoundryControllerUiLayout.TANK_X,
                top + 154,
                FoundryControllerUiLayout.TANK_WIDTH,
                9,
                FoundryControllerUiDrawing.MUTED_TEXT
        );
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

        /*
         * These offsets match the black inner rectangle of the Tank frame in
         * foundry_controller.png exactly.
         */
        int gaugeInnerX =
                screen.guiLeft()
                        + FoundryControllerUiLayout.TANK_GAUGE_X
                        + 3;

        int gaugeY =
                screen.guiTop()
                        + FoundryControllerUiLayout.TANK_GAUGE_Y
                        + 2;

        int gaugeInnerWidth =
                FoundryControllerUiLayout.TANK_GAUGE_WIDTH
                        - 4;

        /*
         * Use a slightly smaller bottom inset than the top inset so molten
         * layers sit more naturally on the tank floor and do not leave an
         * oversized empty strip below the lowest metal.
         */
        int gaugeHeight =
                FoundryControllerUiLayout.TANK_GAUGE_HEIGHT
                        - 2;

        int bottom =
                gaugeY
                        + gaugeHeight;

        int accumulated =
                0;

        for (
                MoltenMetalDefinition definition :
                ModMoltenMetals.heaviestFirst()
        ) {
            int amount =
                    screen.controllerMenu()
                            .getMetalAmount(
                                    definition
                            );

            if (amount <= 0) {
                continue;
            }

            int lowerPixels =
                    accumulated
                            * gaugeHeight
                            / capacity;

            accumulated +=
                    amount;

            int upperPixels =
                    Math.max(
                            lowerPixels + 1,
                            accumulated
                                    * gaugeHeight
                                    / capacity
                    );

            upperPixels =
                    Math.min(
                            gaugeHeight,
                            upperPixels
                    );

            renderMoltenSegment(
                    graphics,
                    definition.animatedTexture(),
                    gaugeInnerX,
                    gaugeInnerWidth,
                    bottom - upperPixels,
                    bottom - lowerPixels
            );
        }
    }

    private void renderMoltenSegment(
            GuiGraphics graphics,
            ResourceLocation texture,
            int x,
            int width,
            int top,
            int bottom
    ) {
        if (
                width <= 0
                        || bottom <= top
        ) {
            return;
        }

        /*
         * The source textures remain their native 16 px width. Repeating them
         * horizontally inside one scissor rectangle fills the complete Tank
         * opening without stretching or introducing asymmetric side gaps.
         */
        graphics.enableScissor(
                x,
                top,
                x + width,
                bottom
        );

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

        graphics.disableScissor();
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
