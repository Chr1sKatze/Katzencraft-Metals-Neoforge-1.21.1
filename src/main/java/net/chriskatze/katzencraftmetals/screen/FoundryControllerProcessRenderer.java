package net.chriskatze.katzencraftmetals.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

final class FoundryControllerProcessRenderer {

    private static final ResourceLocation UNLOCKED_SLOT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    KatzencraftMetalsMod.MODID,
                    "textures/gui/foundry_controller_unlocked_slot.png"
            );

    private static final ResourceLocation LOCKED_SLOT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    KatzencraftMetalsMod.MODID,
                    "textures/gui/foundry_controller_slot_locked.png"
            );

    private static final ResourceLocation PROCESS_MASK_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    KatzencraftMetalsMod.MODID,
                    "textures/gui/foundry_controller_process_mask.png"
            );

    private static final ResourceLocation FUEL_MASK_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    KatzencraftMetalsMod.MODID,
                    "textures/gui/foundry_controller_fuel_mask.png"
            );

    private static final int PROCESS_GRADIENT_BOTTOM = 0xFF9E3009;
    private static final int PROCESS_GRADIENT_MIDDLE = 0xFFFF941F;
    private static final int PROCESS_GRADIENT_TOP = 0xFFFFD66B;

    private static final int FUEL_GRADIENT_LEFT = 0xFFB9550C;
    private static final int FUEL_GRADIENT_RIGHT = 0xFFFFC34A;

    private final FoundryControllerScreen screen;

    FoundryControllerProcessRenderer(
            FoundryControllerScreen screen
    ) {
        this.screen = screen;
    }

    void render(
            GuiGraphics graphics
    ) {
        renderSlotFrames(graphics);
        renderActiveInputOutline(graphics);
        renderBurnTime(graphics);
        renderProcessProgress(graphics);
    }

    private void renderSlotFrames(
            GuiGraphics graphics
    ) {
        int unlockedInputs =
                screen.controllerMenu()
                        .getUnlockedInputSlotCount();

        for (
                int slot = 0;
                slot < FoundryControllerUiLayout.INPUT_SLOT_X.length;
                slot++
        ) {
            drawSlotFrame(
                    graphics,
                    FoundryControllerUiLayout.INPUT_SLOT_X[slot],
                    FoundryControllerUiLayout.INPUT_SLOT_Y[slot],
                    slot < unlockedInputs
            );
        }

        int unlockedFuelSlots =
                screen.controllerMenu()
                        .getUnlockedFuelSlotCount();

        for (int slot = 0; slot < 4; slot++) {
            drawSlotFrame(
                    graphics,
                    FoundryControllerUiLayout.FUEL_SLOT_START_X
                            + slot * FoundryControllerUiLayout.SLOT_SPACING,
                    FoundryControllerUiLayout.FUEL_SLOT_Y,
                    slot < unlockedFuelSlots
            );
        }
    }

    private void drawSlotFrame(
            GuiGraphics graphics,
            int slotX,
            int slotY,
            boolean unlocked
    ) {
        if (unlocked) {
            graphics.blit(
                    UNLOCKED_SLOT_TEXTURE,
                    screen.guiLeft() + slotX - 1,
                    screen.guiTop() + slotY - 1,
                    0.0f,
                    0.0f,
                    18,
                    18,
                    18,
                    18
            );
            return;
        }

        graphics.blit(
                LOCKED_SLOT_TEXTURE,
                screen.guiLeft() + slotX,
                screen.guiTop() + slotY,
                0.0f,
                0.0f,
                16,
                16,
                16,
                16
        );
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

        drawHorizontalGradientMask(
                graphics,
                FUEL_MASK_TEXTURE,
                FoundryControllerUiLayout.BURN_BAR_X,
                FoundryControllerUiLayout.BURN_BAR_Y,
                Math.min(
                        fill,
                        FoundryControllerUiLayout.BURN_BAR_WIDTH
                ),
                FoundryControllerUiLayout.BURN_BAR_HEIGHT,
                FoundryControllerUiLayout.BURN_BAR_WIDTH,
                FUEL_GRADIENT_LEFT,
                FUEL_GRADIENT_RIGHT
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

        fill =
                Math.min(
                        fill,
                        FoundryControllerUiLayout.PROGRESS_GAUGE_HEIGHT
                );

        int sourceYOffset =
                FoundryControllerUiLayout.PROGRESS_GAUGE_HEIGHT
                        - fill;

        drawVerticalGradientMask(
                graphics,
                PROCESS_MASK_TEXTURE,
                FoundryControllerUiLayout.PROGRESS_GAUGE_X,
                FoundryControllerUiLayout.PROGRESS_GAUGE_Y,
                sourceYOffset,
                FoundryControllerUiLayout.PROGRESS_GAUGE_WIDTH,
                fill,
                FoundryControllerUiLayout.PROGRESS_GAUGE_HEIGHT,
                PROCESS_GRADIENT_BOTTOM,
                PROCESS_GRADIENT_MIDDLE,
                PROCESS_GRADIENT_TOP
        );
    }

    private void drawHorizontalGradientMask(
            GuiGraphics graphics,
            ResourceLocation texture,
            int relativeX,
            int relativeY,
            int visibleWidth,
            int height,
            int totalWidth,
            int leftColor,
            int rightColor
    ) {
        if (visibleWidth <= 0 || height <= 0 || totalWidth <= 0) {
            return;
        }

        int screenX =
                screen.guiLeft()
                        + relativeX;

        int screenY =
                screen.guiTop()
                        + relativeY;

        for (int slice = 0; slice < visibleWidth; slice++) {
            float progress =
                    totalWidth <= 1
                            ? 1.0F
                            : slice
                            / (float) (totalWidth - 1);

            setShaderColor(
                    lerpColor(
                            leftColor,
                            rightColor,
                            progress
                    )
            );

            graphics.blit(
                    texture,
                    screenX + slice,
                    screenY,
                    relativeX + slice,
                    relativeY,
                    1,
                    height,
                    FoundryControllerUiLayout.WIDTH,
                    FoundryControllerUiLayout.HEIGHT
            );
        }

        resetShaderColor();
    }

    private void drawVerticalGradientMask(
            GuiGraphics graphics,
            ResourceLocation texture,
            int relativeX,
            int relativeY,
            int sourceYOffset,
            int width,
            int visibleHeight,
            int totalHeight,
            int bottomColor,
            int middleColor,
            int topColor
    ) {
        if (width <= 0 || visibleHeight <= 0 || totalHeight <= 0) {
            return;
        }

        int screenX =
                screen.guiLeft()
                        + relativeX;

        int screenTop =
                screen.guiTop()
                        + relativeY
                        + sourceYOffset;

        for (int slice = 0; slice < visibleHeight; slice++) {
            int absoluteY =
                    sourceYOffset
                            + slice;

            float fromBottom =
                    totalHeight <= 1
                            ? 1.0F
                            : (totalHeight - 1 - absoluteY)
                            / (float) (totalHeight - 1);

            int color =
                    gradient3(
                            bottomColor,
                            middleColor,
                            topColor,
                            fromBottom
                    );

            setShaderColor(color);

            graphics.blit(
                    texture,
                    screenX,
                    screenTop + slice,
                    relativeX,
                    relativeY + absoluteY,
                    width,
                    1,
                    FoundryControllerUiLayout.WIDTH,
                    FoundryControllerUiLayout.HEIGHT
            );
        }

        resetShaderColor();
    }

    private static int gradient3(
            int bottomColor,
            int middleColor,
            int topColor,
            float progress
    ) {
        if (progress <= 0.5F) {
            return lerpColor(
                    bottomColor,
                    middleColor,
                    progress * 2.0F
            );
        }

        return lerpColor(
                middleColor,
                topColor,
                (progress - 0.5F) * 2.0F
        );
    }

    private static int lerpColor(
            int firstColor,
            int secondColor,
            float progress
    ) {
        progress =
                Math.max(
                        0.0F,
                        Math.min(
                                1.0F,
                                progress
                        )
                );

        int alpha =
                lerpChannel(
                        firstColor >>> 24,
                        secondColor >>> 24,
                        progress
                );

        int red =
                lerpChannel(
                        firstColor >>> 16,
                        secondColor >>> 16,
                        progress
                );

        int green =
                lerpChannel(
                        firstColor >>> 8,
                        secondColor >>> 8,
                        progress
                );

        int blue =
                lerpChannel(
                        firstColor,
                        secondColor,
                        progress
                );

        return alpha << 24
                | red << 16
                | green << 8
                | blue;
    }

    private static int lerpChannel(
            int first,
            int second,
            float progress
    ) {
        return Math.round(
                (first & 0xFF)
                        + (
                        (second & 0xFF)
                                - (first & 0xFF)
                )
                        * progress
        );
    }

    private static void setShaderColor(
            int color
    ) {
        RenderSystem.setShaderColor(
                ((color >>> 16) & 0xFF) / 255.0F,
                ((color >>> 8) & 0xFF) / 255.0F,
                (color & 0xFF) / 255.0F,
                ((color >>> 24) & 0xFF) / 255.0F
        );
    }

    private static void resetShaderColor() {
        RenderSystem.setShaderColor(
                1.0F,
                1.0F,
                1.0F,
                1.0F
        );
    }
}
