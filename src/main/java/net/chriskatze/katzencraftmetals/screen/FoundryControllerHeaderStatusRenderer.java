package net.chriskatze.katzencraftmetals.screen;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.menu.FoundryControllerMenu;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

final class FoundryControllerHeaderStatusRenderer {

    private static final float XP_TEXT_SCALE = 0.75F;

    private static final ResourceLocation BAR_FRAME_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    KatzencraftMetalsMod.MODID,
                    "textures/gui/foundry_controller_bar_frame.png"
            );

    /*
     * The supplied frame texture is exactly 170 x 16 pixels.
     *
     * We first draw the complete bar underneath it and then draw this frame on
     * top. Every transparent pixel inside the frame therefore reveals the bar,
     * while every authored border pixel remains untouched.
     */
    private static final int BAR_X = 2;
    private static final int BAR_WIDTH = 170;
    private static final int BAR_HEIGHT = 16;

    private static final int XP_BAR_Y = 21;

    /*
     * Both empty tracks are fully opaque. The Foundry GUI background can never
     * show through the transparent interior of the authored frame.
     */
    private static final int XP_TRACK = 0xFF172218;
    private static final int XP_TRACK_TOP = 0xFF273C29;
    private static final int XP_TRACK_BOTTOM = 0xFF0B100C;
    private static final int XP_SEGMENT = 0x36FFFFFF;

    /*
     * XP is intentionally always light green, regardless of Foundry tier.
     */
    private static final int XP_TEXT = 0xFFEAF4D7;
    private static final int XP_TOP = 0xFFC9E09B;
    private static final int XP_MIDDLE = 0xFF86B85C;
    private static final int XP_BOTTOM = 0xFF3E7034;
    private static final int XP_GLOW = 0x6689BD69;

    private final FoundryControllerScreen screen;

    FoundryControllerHeaderStatusRenderer(
            FoundryControllerScreen screen
    ) {
        this.screen = screen;
    }

    void render(
            GuiGraphics graphics
    ) {
        renderTier(graphics);
        renderExperience(graphics);

        /*
         * The old bottom status/info bar was removed from the authored texture.
         * Do not render "READY", "MELTING", activity glyphs, or the lower frame
         * anymore; that space is now the player's hotbar row.
         */
    }

    private void renderTier(
            GuiGraphics graphics
    ) {
        Component tier =
                Component.literal(
                        "TIER "
                                + menu().getFoundryTier()
                );

        graphics.drawString(
                screen.uiFont(),
                tier,
                screen.guiLeft()
                        + 278
                        - screen.uiFont().width(tier),
                screen.guiTop() + 7,
                FoundryControllerUiDrawing.TEXT,
                false
        );
    }

    private void renderExperience(
            GuiGraphics graphics
    ) {
        int left =
                screen.guiLeft()
                        + BAR_X;

        int top =
                screen.guiTop()
                        + XP_BAR_Y;

        /*
         * Fill the entire 170 x 16 frame rectangle first. The frame is rendered
         * afterwards, so its visible border is restored on top.
         */
        drawFullTrack(
                graphics,
                left,
                top,
                XP_TRACK,
                XP_TRACK_TOP,
                XP_TRACK_BOTTOM
        );

        int fill =
                menu().getFoundryTier() >= 4
                        ? BAR_WIDTH
                        : menu().getScaledExperience(
                        BAR_WIDTH
                );

        if (fill > 0) {
            drawExperienceFill(
                    graphics,
                    left,
                    top,
                    Math.min(fill, BAR_WIDTH)
            );
        }

        drawExperienceSegments(
                graphics,
                left,
                top
        );

        Component experienceText =
                menu().getFoundryTier() >= 4
                        ? Component.literal("MAX")
                        : Component.literal(
                        menu().getTierExperience()
                                + " / "
                                + menu().getTierExperienceNeeded()
                );

        drawScaledCenteredText(
                graphics,
                experienceText,
                left,
                top,
                BAR_WIDTH,
                BAR_HEIGHT,
                XP_TEXT
        );

        drawFrame(
                graphics,
                left,
                top
        );
    }

    private void drawExperienceFill(
            GuiGraphics graphics,
            int left,
            int top,
            int fillWidth
    ) {
        int right =
                left
                        + fillWidth;

        graphics.fill(
                left,
                top,
                right,
                top + BAR_HEIGHT,
                XP_MIDDLE
        );

        graphics.fill(
                left,
                top,
                right,
                top + 2,
                XP_TOP
        );

        graphics.fill(
                left,
                top + BAR_HEIGHT - 2,
                right,
                top + BAR_HEIGHT,
                XP_BOTTOM
        );

        if (right < left + BAR_WIDTH) {
            graphics.fill(
                    Math.max(left, right - 2),
                    top + 2,
                    right,
                    top + BAR_HEIGHT - 2,
                    XP_GLOW
            );
        }

        int[] sparkleOffsets = {
                9,
                31,
                53,
                75,
                97,
                119,
                141,
                163
        };

        for (int offset : sparkleOffsets) {
            if (offset >= fillWidth - 1) {
                break;
            }

            drawPixel(
                    graphics,
                    left + offset,
                    top + 4,
                    0xA6FFFFFF
            );
        }
    }

    private void drawExperienceSegments(
            GuiGraphics graphics,
            int left,
            int top
    ) {
        for (int segment = 1; segment < 10; segment++) {
            int segmentX =
                    left
                            + segment
                            * BAR_WIDTH
                            / 10;

            graphics.fill(
                    segmentX,
                    top + 2,
                    segmentX + 1,
                    top + BAR_HEIGHT - 2,
                    XP_SEGMENT
            );
        }
    }

    private void drawFullTrack(
            GuiGraphics graphics,
            int left,
            int top,
            int middle,
            int highlight,
            int shadow
    ) {
        graphics.fill(
                left,
                top,
                left + BAR_WIDTH,
                top + BAR_HEIGHT,
                middle
        );

        graphics.fill(
                left,
                top,
                left + BAR_WIDTH,
                top + 2,
                highlight
        );

        graphics.fill(
                left,
                top + BAR_HEIGHT - 2,
                left + BAR_WIDTH,
                top + BAR_HEIGHT,
                shadow
        );
    }

    private void drawFrame(
            GuiGraphics graphics,
            int left,
            int top
    ) {
        graphics.blit(
                BAR_FRAME_TEXTURE,
                left,
                top,
                0.0F,
                0.0F,
                BAR_WIDTH,
                BAR_HEIGHT,
                BAR_WIDTH,
                BAR_HEIGHT
        );
    }

    private void drawScaledCenteredText(
            GuiGraphics graphics,
            Component text,
            int left,
            int top,
            int width,
            int height,
            int color
    ) {
        Font font =
                screen.uiFont();

        float scaledWidth =
                font.width(text)
                        * XP_TEXT_SCALE;

        float scaledHeight =
                font.lineHeight
                        * XP_TEXT_SCALE;

        float textX =
                left
                        + (
                        width
                                - scaledWidth
                ) / 2.0F;

        float textY =
                top
                        + (
                        height
                                - scaledHeight
                ) / 2.0F
                        + 0.5F;

        graphics.pose().pushPose();

        graphics.pose().translate(
                textX,
                textY,
                100.0F
        );

        graphics.pose().scale(
                XP_TEXT_SCALE,
                XP_TEXT_SCALE,
                1.0F
        );

        graphics.drawString(
                font,
                text,
                0,
                0,
                color,
                true
        );

        graphics.pose().popPose();
    }

    private static void drawPixel(
            GuiGraphics graphics,
            int x,
            int y,
            int color
    ) {
        graphics.fill(
                x,
                y,
                x + 1,
                y + 1,
                color
        );
    }

    private FoundryControllerMenu menu() {
        return screen.controllerMenu();
    }
}
