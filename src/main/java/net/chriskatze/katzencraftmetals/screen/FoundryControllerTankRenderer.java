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
                screen.guiLeft() + 127,
                screen.guiTop() + 152,
                44,
                12,
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
