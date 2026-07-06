package net.chriskatze.katzencraftmetals.screen;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.menu.FoundryControllerMenu;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

final class FoundryControllerHeaderStatusRenderer {

    private static final float XP_TEXT_SCALE = 0.75F;
    private static final float STATUS_TEXT_SCALE = 0.75F;

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
    private static final int STATUS_BAR_Y = 214;

    private static final int STATUS_TEXT_X_OFFSET = 19;
    private static final int STATUS_TEXT_RIGHT_PADDING = 6;
    private static final int STATUS_ACTIVITY_WIDTH = 15;

    /*
     * Both empty tracks are fully opaque. The Foundry GUI background can never
     * show through the transparent interior of the authored frame.
     */
    private static final int XP_TRACK = 0xFF172218;
    private static final int XP_TRACK_TOP = 0xFF273C29;
    private static final int XP_TRACK_BOTTOM = 0xFF0B100C;
    private static final int XP_SEGMENT = 0x36FFFFFF;

    private static final int STATUS_TRACK = 0xFF151321;
    private static final int STATUS_TRACK_TOP = 0xFF29243B;
    private static final int STATUS_TRACK_BOTTOM = 0xFF090811;

    /*
     * XP is intentionally always light green, regardless of Foundry tier.
     */
    private static final TierPalette XP_PALETTE =
            new TierPalette(
                    0xFFE7F7C8,
                    0xFF9DCE72,
                    0xFF4D7E3E,
                    0xFFF4FFE8,
                    0x66B8E58D
            );

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
        renderStatus(graphics);
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
                XP_PALETTE.text()
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
                XP_PALETTE.middle()
        );

        graphics.fill(
                left,
                top,
                right,
                top + 2,
                XP_PALETTE.top()
        );

        graphics.fill(
                left,
                top + BAR_HEIGHT - 2,
                right,
                top + BAR_HEIGHT,
                XP_PALETTE.bottom()
        );

        if (right < left + BAR_WIDTH) {
            graphics.fill(
                    Math.max(left, right - 2),
                    top + 2,
                    right,
                    top + BAR_HEIGHT - 2,
                    XP_PALETTE.glow()
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

    private void renderStatus(
            GuiGraphics graphics
    ) {
        FoundryStatus status =
                resolveStatus();

        TierPalette palette =
                statusPalette();

        int left =
                screen.guiLeft()
                        + BAR_X;

        int top =
                screen.guiTop()
                        + STATUS_BAR_Y;

        drawFullTrack(
                graphics,
                left,
                top,
                STATUS_TRACK,
                STATUS_TRACK_TOP,
                STATUS_TRACK_BOTTOM
        );

        /*
         * Restore the small glowing status dot used by the earlier design.
         */
        drawStatusDot(
                graphics,
                left + 6,
                top + 5,
                palette
        );

        int activityWidth =
                status.working()
                        ? STATUS_ACTIVITY_WIDTH
                        : 0;

        int availableTextWidth =
                BAR_WIDTH
                        - STATUS_TEXT_X_OFFSET
                        - STATUS_TEXT_RIGHT_PADDING
                        - activityWidth;

        String fitted =
                fitScaledText(
                        screen.uiFont(),
                        status.text().getString(),
                        availableTextWidth,
                        STATUS_TEXT_SCALE
                );

        drawScaledLeftText(
                graphics,
                Component.literal(fitted),
                left + STATUS_TEXT_X_OFFSET,
                top,
                BAR_HEIGHT,
                palette.text()
        );

        if (status.working()) {
            drawActivityGlyphs(
                    graphics,
                    left + BAR_WIDTH - STATUS_ACTIVITY_WIDTH,
                    top,
                    palette
            );
        }

        drawFrame(
                graphics,
                left,
                top
        );
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

    private void drawStatusDot(
            GuiGraphics graphics,
            int left,
            int top,
            TierPalette palette
    ) {
        /*
         * Five-pixel rounded orb:
         *
         *   xxx
         *  xxxxx
         * xxxxxxx
         *  xxxxx
         *   xxx
         */
        graphics.fill(
                left + 2,
                top,
                left + 5,
                top + 1,
                palette.top()
        );

        graphics.fill(
                left + 1,
                top + 1,
                left + 6,
                top + 2,
                palette.middle()
        );

        graphics.fill(
                left,
                top + 2,
                left + 7,
                top + 3,
                palette.middle()
        );

        graphics.fill(
                left + 1,
                top + 3,
                left + 6,
                top + 4,
                palette.middle()
        );

        graphics.fill(
                left + 2,
                top + 4,
                left + 5,
                top + 5,
                palette.bottom()
        );

        drawPixel(
                graphics,
                left + 2,
                top + 1,
                0xCCFFFFFF
        );

        drawPixel(
                graphics,
                left + 5,
                top + 2,
                palette.glow()
        );
    }

    private void drawActivityGlyphs(
            GuiGraphics graphics,
            int left,
            int top,
            TierPalette palette
    ) {
        int phase =
                (int) (
                        System.currentTimeMillis()
                                / 220L
                                % 3L
                );

        for (int index = 0; index < 3; index++) {
            int glyphLeft =
                    left
                            + index * 4;

            int color =
                    index == phase
                            ? palette.top()
                            : palette.bottom();

            graphics.fill(
                    glyphLeft,
                    top + 7,
                    glyphLeft + 2,
                    top + 8,
                    color
            );

            graphics.fill(
                    glyphLeft + 1,
                    top + 6,
                    glyphLeft + 2,
                    top + 9,
                    color
            );
        }
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

    private void drawScaledLeftText(
            GuiGraphics graphics,
            Component text,
            int left,
            int top,
            int height,
            int color
    ) {
        Font font =
                screen.uiFont();

        float scaledHeight =
                font.lineHeight
                        * STATUS_TEXT_SCALE;

        float textY =
                top
                        + (
                        height
                                - scaledHeight
                ) / 2.0F
                        + 0.5F;

        graphics.pose().pushPose();

        graphics.pose().translate(
                left,
                textY,
                100.0F
        );

        graphics.pose().scale(
                STATUS_TEXT_SCALE,
                STATUS_TEXT_SCALE,
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

    private TierPalette statusPalette() {
        return switch (menu().getFoundryTier()) {
            case 1 -> new TierPalette(
                    0xFFFFFFFF,
                    0xFFE6E6E6,
                    0xFF9F9F9F,
                    0xFFFFFFFF,
                    0x66FFFFFF
            );
            case 2 -> new TierPalette(
                    0xFFD3F5FF,
                    0xFF8CCEFF,
                    0xFF3E7AC1,
                    0xFFF4FCFF,
                    0x6657CFFF
            );
            case 3 -> new TierPalette(
                    0xFFFFF1B0,
                    0xFFF4C75A,
                    0xFFB77A1F,
                    0xFFFFF7DB,
                    0x66FFD36B
            );
            default -> new TierPalette(
                    0xFFE0EDFF,
                    0xFFB48CFF,
                    0xFF4A3CB7,
                    0xFFF7F2FF,
                    0x667ED7FF
            );
        };
    }

    private static String fitScaledText(
            Font font,
            String text,
            int renderedWidth,
            float scale
    ) {
        if (
                text == null
                        || text.isEmpty()
                        || renderedWidth <= 0
                        || scale <= 0.0F
        ) {
            return "";
        }

        int maximumWidth =
                Math.max(
                        1,
                        (int) Math.floor(
                                renderedWidth
                                        / scale
                        )
                );

        if (font.width(text) <= maximumWidth) {
            return text;
        }

        String suffix = "...";
        int suffixWidth =
                font.width(suffix);

        if (suffixWidth >= maximumWidth) {
            return font.plainSubstrByWidth(
                    text,
                    maximumWidth
            );
        }

        return font.plainSubstrByWidth(
                text,
                maximumWidth - suffixWidth
        ) + suffix;
    }

    private FoundryStatus resolveStatus() {
        return switch (menu().getProcessingStatus()) {
            case 1 -> new FoundryStatus(
                    Component.literal("NO TANKS CONNECTED"),
                    false
            );
            case 2 -> new FoundryStatus(
                    Component.literal("TANK FULL"),
                    false
            );
            case 3 -> new FoundryStatus(
                    Component.literal("MISSING FUEL"),
                    false
            );
            case 4 -> new FoundryStatus(
                    Component.literal("MELTING ").append(
                            menu().getInputMoltenMetalDefinition()
                                    .<Component>map(
                                            definition ->
                                                    Component.literal(
                                                            displayName(
                                                                    definition.id()
                                                            )
                                                    )
                                    )
                                    .orElse(
                                            Component.literal("ORE")
                                    )
                    ),
                    true
            );
            case 5 -> new FoundryStatus(
                    Component.literal("ALLOYING ").append(
                            menu().getActiveAlloyOutput()
                                    .<Component>map(
                                            definition ->
                                                    Component.literal(
                                                            displayName(
                                                                    definition.id()
                                                            )
                                                    )
                                    )
                                    .orElse(
                                            Component.literal("ALLOY")
                                    )
                    ),
                    true
            );
            default -> new FoundryStatus(
                    Component.literal("READY"),
                    false
            );
        };
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

    private static String displayName(
            ResourceLocation id
    ) {
        String path =
                id.getPath()
                        .replace(
                                '_',
                                ' '
                        );

        if (path.isEmpty()) {
            return "";
        }

        return Character.toUpperCase(
                path.charAt(0)
        )
                + path.substring(1);
    }

    private FoundryControllerMenu menu() {
        return screen.controllerMenu();
    }

    private record FoundryStatus(
            Component text,
            boolean working
    ) {
    }

    private record TierPalette(
            int top,
            int middle,
            int bottom,
            int text,
            int glow
    ) {
    }
}
