package net.chriskatze.katzencraftmetals.screen;

import net.chriskatze.katzencraftmetals.menu.FoundryControllerMenu;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

final class FoundryControllerHeaderStatusRenderer {

    private static final float XP_TEXT_SCALE = 0.75F;
    private static final float STATUS_TEXT_SCALE = 0.75F;

    private static final int XP_FRAME = 0xFF171A15;
    private static final int XP_TRACK = 0xFF292E25;
    private static final int XP_TRACK_HIGHLIGHT = 0xFF3A4034;

    private static final int XP_FILL_TOP = 0xFF8BE36E;
    private static final int XP_FILL_MIDDLE = 0xFF53B947;
    private static final int XP_FILL_BOTTOM = 0xFF2F7932;

    private static final int XP_MAX_TOP = 0xFFFFE27A;
    private static final int XP_MAX_MIDDLE = 0xFFE0A936;
    private static final int XP_MAX_BOTTOM = 0xFF9B6B19;

    private static final int XP_SEGMENT = 0x66000000;
    private static final int XP_FILL_EDGE = 0xAAFFFFFF;

    /*
     * The authored GUI already leaves a status area at the bottom-left.
     * This plate begins where the dynamic status text previously began, so the
     * left-hand authored label remains untouched.
     */
    private static final int STATUS_PANEL_X = 48;
    private static final int STATUS_PANEL_Y = 216;
    private static final int STATUS_PANEL_WIDTH = 122;
    private static final int STATUS_PANEL_HEIGHT = 12;

    private static final int STATUS_FRAME = 0xFF171815;
    private static final int STATUS_BACKGROUND = 0xFF252821;
    private static final int STATUS_BACKGROUND_TOP = 0xFF393C32;
    private static final int STATUS_BACKGROUND_BOTTOM = 0xFF11130F;
    private static final int STATUS_DIVIDER = 0xFF11130F;
    private static final int STATUS_RIVET = 0xFF606357;

    private static final int STATUS_LAMP_FRAME = 0xFF0D0E0C;
    private static final int STATUS_LAMP_HIGHLIGHT = 0xCCFFFFFF;

    private static final int STATUS_TEXT_X_OFFSET = 18;
    private static final int STATUS_TEXT_RIGHT_PADDING = 5;
    private static final int STATUS_ACTIVITY_WIDTH = 13;

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
                        + FoundryControllerUiLayout.XP_BAR_X;

        int top =
                screen.guiTop()
                        + FoundryControllerUiLayout.XP_BAR_Y;

        int width =
                FoundryControllerUiLayout.XP_BAR_WIDTH;

        int height =
                FoundryControllerUiLayout.XP_BAR_HEIGHT;

        int innerLeft = left + 1;
        int innerTop = top + 1;
        int innerWidth = Math.max(0, width - 2);
        int innerHeight = Math.max(0, height - 2);

        boolean maximumTier =
                menu().getFoundryTier() >= 4;

        int fill =
                maximumTier
                        ? innerWidth
                        : menu().getScaledExperience(
                        innerWidth
                );

        /*
         * Dark one-pixel frame and recessed track.
         */
        graphics.fill(
                left,
                top,
                left + width,
                top + height,
                XP_FRAME
        );

        if (innerWidth > 0 && innerHeight > 0) {
            graphics.fill(
                    innerLeft,
                    innerTop,
                    innerLeft + innerWidth,
                    innerTop + innerHeight,
                    XP_TRACK
            );

            /*
             * A faint highlight along the empty track keeps it from looking
             * like a completely flat black rectangle.
             */
            graphics.fill(
                    innerLeft,
                    innerTop,
                    innerLeft + innerWidth,
                    innerTop + 1,
                    XP_TRACK_HIGHLIGHT
            );
        }

        if (fill > 0 && innerHeight > 0) {
            int fillRight =
                    innerLeft
                            + Math.min(
                            fill,
                            innerWidth
                    );

            int topColor =
                    maximumTier
                            ? XP_MAX_TOP
                            : XP_FILL_TOP;

            int middleColor =
                    maximumTier
                            ? XP_MAX_MIDDLE
                            : XP_FILL_MIDDLE;

            int bottomColor =
                    maximumTier
                            ? XP_MAX_BOTTOM
                            : XP_FILL_BOTTOM;

            /*
             * Three simple pixel-art shades make the fill look beveled without
             * requiring another texture.
             */
            graphics.fill(
                    innerLeft,
                    innerTop,
                    fillRight,
                    innerTop + innerHeight,
                    middleColor
            );

            graphics.fill(
                    innerLeft,
                    innerTop,
                    fillRight,
                    innerTop + 1,
                    topColor
            );

            graphics.fill(
                    innerLeft,
                    innerTop + innerHeight - 1,
                    fillRight,
                    innerTop + innerHeight,
                    bottomColor
            );

            /*
             * Bright one-pixel cap at the current progress position.
             */
            if (fillRight < innerLeft + innerWidth) {
                graphics.fill(
                        fillRight - 1,
                        innerTop + 1,
                        fillRight,
                        innerTop + innerHeight - 1,
                        XP_FILL_EDGE
                );
            }
        }

        /*
         * Ten subtle divisions make progress easier to read while keeping the
         * original compact eight-pixel-high bar.
         */
        for (int segment = 1; segment < 10; segment++) {
            int segmentX =
                    innerLeft
                            + segment
                            * innerWidth
                            / 10;

            graphics.fill(
                    segmentX,
                    innerTop,
                    segmentX + 1,
                    innerTop + innerHeight,
                    XP_SEGMENT
            );
        }

        Component experienceText =
                maximumTier
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
                width,
                height,
                maximumTier
                        ? 0xFFFFF0A6
                        : 0xFFFFFFFF
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

    private void renderStatus(
            GuiGraphics graphics
    ) {
        FoundryStatus status =
                resolveStatus();

        int left =
                screen.guiLeft()
                        + STATUS_PANEL_X;

        int top =
                screen.guiTop()
                        + STATUS_PANEL_Y;

        int right =
                left
                        + STATUS_PANEL_WIDTH;

        int bottom =
                top
                        + STATUS_PANEL_HEIGHT;

        /*
         * A compact, recessed metal plate matching the visual treatment of the
         * XP bar.
         */
        graphics.fill(
                left,
                top,
                right,
                bottom,
                STATUS_FRAME
        );

        graphics.fill(
                left + 1,
                top + 1,
                right - 1,
                bottom - 1,
                STATUS_BACKGROUND
        );

        graphics.fill(
                left + 1,
                top + 1,
                right - 1,
                top + 2,
                STATUS_BACKGROUND_TOP
        );

        graphics.fill(
                left + 1,
                bottom - 2,
                right - 1,
                bottom - 1,
                STATUS_BACKGROUND_BOTTOM
        );

        /*
         * Tiny corner rivets help the plate look like part of the machine
         * instead of a plain text box.
         */
        drawPixel(
                graphics,
                left + 2,
                top + 2,
                STATUS_RIVET
        );

        drawPixel(
                graphics,
                right - 3,
                top + 2,
                STATUS_RIVET
        );

        drawStatusLamp(
                graphics,
                left + 5,
                top + 3,
                status
        );

        graphics.fill(
                left + 15,
                top + 2,
                left + 16,
                bottom - 2,
                STATUS_DIVIDER
        );

        int activityWidth =
                status.working()
                        ? STATUS_ACTIVITY_WIDTH
                        : 0;

        int availableTextWidth =
                STATUS_PANEL_WIDTH
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
                STATUS_PANEL_HEIGHT,
                status.textColor()
        );

        if (status.working()) {
            drawActivityDots(
                    graphics,
                    right - STATUS_ACTIVITY_WIDTH,
                    top,
                    status
            );
        }
    }

    private void drawStatusLamp(
            GuiGraphics graphics,
            int left,
            int top,
            FoundryStatus status
    ) {
        graphics.fill(
                left,
                top,
                left + 7,
                top + 7,
                STATUS_LAMP_FRAME
        );

        graphics.fill(
                left + 1,
                top + 1,
                left + 6,
                top + 6,
                status.lampMiddle()
        );

        graphics.fill(
                left + 1,
                top + 1,
                left + 6,
                top + 2,
                status.lampTop()
        );

        graphics.fill(
                left + 1,
                top + 5,
                left + 6,
                top + 6,
                status.lampBottom()
        );

        drawPixel(
                graphics,
                left + 2,
                top + 2,
                STATUS_LAMP_HIGHLIGHT
        );
    }

    private void drawActivityDots(
            GuiGraphics graphics,
            int left,
            int top,
            FoundryStatus status
    ) {
        int phase =
                (int) (
                        System.currentTimeMillis()
                                / 250L
                                % 3L
                );

        for (int index = 0; index < 3; index++) {
            int dotLeft =
                    left
                            + index * 4;

            int color =
                    index == phase
                            ? status.lampTop()
                            : status.lampBottom();

            graphics.fill(
                    dotLeft,
                    top + 5,
                    dotLeft + 2,
                    top + 7,
                    color
            );
        }
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

    private FoundryStatus resolveStatus() {
        return switch (menu().getProcessingStatus()) {
            case 1 -> FoundryStatus.error(
                    Component.literal(
                            "NO TANKS CONNECTED"
                    )
            );
            case 2 -> FoundryStatus.error(
                    Component.literal(
                            "TANK FULL"
                    )
            );
            case 3 -> FoundryStatus.error(
                    Component.literal(
                            "MISSING FUEL"
                    )
            );
            case 4 -> FoundryStatus.working(
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
                    )
            );
            case 5 -> FoundryStatus.working(
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
                    )
            );
            default -> FoundryStatus.ready(
                    Component.literal("READY")
            );
        };
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
            int textColor,
            int lampTop,
            int lampMiddle,
            int lampBottom,
            boolean working
    ) {

        private static FoundryStatus ready(
                Component text
        ) {
            return new FoundryStatus(
                    text,
                    0xFFD8F5CB,
                    0xFFA2ED88,
                    0xFF52B84D,
                    0xFF28632A,
                    false
            );
        }

        private static FoundryStatus working(
                Component text
        ) {
            return new FoundryStatus(
                    text,
                    0xFFFFD6A0,
                    0xFFFFD27A,
                    0xFFE69334,
                    0xFF8A4D18,
                    true
            );
        }

        private static FoundryStatus error(
                Component text
        ) {
            return new FoundryStatus(
                    text,
                    0xFFFFB5AA,
                    0xFFFF9A8C,
                    0xFFD94C43,
                    0xFF772720,
                    false
            );
        }
    }
}
