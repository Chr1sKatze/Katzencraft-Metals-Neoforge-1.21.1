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

    private static final int XP_SEGMENT_COUNT = 10;
    private static final int XP_SEGMENT_LEFT_INSET = 2;
    private static final int XP_SEGMENT_RIGHT_INSET = 2;
    private static final int XP_SEGMENT_TOP_INSET = 3;
    private static final int XP_SEGMENT_BOTTOM_INSET = 3;

    private static final int TIER_TEXT_RIGHT = 278;
    private static final int TIER_TEXT_Y = 7;

    /*
     * Softer, less yellow XP palette.
     *
     * The foreground is now a clean flat fill: no sparkle dots and no brighter
     * trailing edge pixels at the current progress boundary.
     */
    private static final int XP_TRACK = 0xFF131E17;
    private static final int XP_TRACK_TOP = 0xFF223428;
    private static final int XP_TRACK_BOTTOM = 0xFF090F0B;
    private static final int XP_SEGMENT = 0x2FEAF4D7;

    private static final int XP_TEXT = 0xFFE4EEDC;
    private static final int XP_TOP = 0xFFA6C99A;
    private static final int XP_MIDDLE = 0xFF6F9A68;
    private static final int XP_BOTTOM = 0xFF385F3E;

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
        int tierLevel =
                Math.max(
                        1,
                        menu().getFoundryTier()
                );

        Component tier =
                Component.literal(
                        "TIER "
                                + tierLevel
                );

        TierTextPalette palette =
                tierTextPalette(tierLevel);

        Font font =
                screen.uiFont();

        int textWidth =
                font.width(tier);

        int textX =
                screen.guiLeft()
                        + TIER_TEXT_RIGHT
                        - textWidth;

        int textY =
                screen.guiTop()
                        + TIER_TEXT_Y;

        /*
         * Restrained rank styling:
         * - keeps the original top-right text placement
         * - adds a soft dark shadow for depth
         * - uses tier-specific text color
         * - adds a tiny accent under the word "TIER" only
         */
        graphics.drawString(
                font,
                tier,
                textX + 1,
                textY + 1,
                0xAA000000,
                false
        );

        graphics.drawString(
                font,
                tier,
                textX,
                textY,
                palette.text(),
                false
        );

        int underlineY =
                textY
                        + font.lineHeight
                        + 1;

        int tierWordWidth =
                font.width("TIER");

        graphics.fill(
                textX,
                underlineY,
                textX + tierWordWidth,
                underlineY + 1,
                palette.line()
        );
    }

    private static TierTextPalette tierTextPalette(
            int tier
    ) {
        return switch (tier) {
            case 1 -> new TierTextPalette(
                    0xFFE6E4DC,
                    0x668D8D86
            );
            case 2 -> new TierTextPalette(
                    0xFFE4A05F,
                    0xAA8A4E2C
            );
            case 3 -> new TierTextPalette(
                    0xFFC7E8F0,
                    0xAA6FA4B5
            );
            case 4 -> new TierTextPalette(
                    0xFFE0B6FF,
                    0xAA8C4DC2
            );
            default -> new TierTextPalette(
                    0xFFE0B6FF,
                    0xAA8C4DC2
            );
        };
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
    }

    private void drawExperienceSegments(
            GuiGraphics graphics,
            int left,
            int top
    ) {
        int segmentAreaLeft =
                left
                        + XP_SEGMENT_LEFT_INSET;

        int segmentAreaWidth =
                BAR_WIDTH
                        - XP_SEGMENT_LEFT_INSET
                        - XP_SEGMENT_RIGHT_INSET;

        /*
         * Use rounded floating-point placement instead of integer truncation.
         *
         * Pixel art can never split every possible interior width perfectly, but
         * rounding distributes the remainder evenly instead of pushing all error
         * toward the right side of the bar.
         */
        for (int segment = 1; segment < XP_SEGMENT_COUNT; segment++) {
            int segmentX =
                    segmentAreaLeft
                            + Math.round(
                            segment
                                    * segmentAreaWidth
                                    / (float) XP_SEGMENT_COUNT
                    );

            graphics.fill(
                    segmentX,
                    top + XP_SEGMENT_TOP_INSET,
                    segmentX + 1,
                    top + BAR_HEIGHT - XP_SEGMENT_BOTTOM_INSET,
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

    private FoundryControllerMenu menu() {
        return screen.controllerMenu();
    }

    private record TierTextPalette(
            int text,
            int line
    ) {
    }
}
