package net.chriskatze.katzencraftmetals.screen;

import net.chriskatze.katzencraftmetals.menu.FoundryControllerMenu;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

final class FoundryControllerHeaderStatusRenderer {

    private static final float XP_TEXT_SCALE = 0.75F;

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
        FoundryStatus status = resolveStatus();

        graphics.drawString(
                screen.uiFont(),
                status.text(),
                screen.guiLeft()
                        + FoundryControllerUiLayout.STATUS_TEXT_X,
                screen.guiTop()
                        + FoundryControllerUiLayout.STATUS_TEXT_Y,
                status.color(),
                false
        );
    }

    private FoundryStatus resolveStatus() {
        return switch (menu().getProcessingStatus()) {
            case 1 -> new FoundryStatus(
                    Component.literal("NO TANKS CONNECTED"),
                    FoundryControllerUiDrawing.RED
            );
            case 2 -> new FoundryStatus(
                    Component.literal("TANK FULL"),
                    FoundryControllerUiDrawing.RED
            );
            case 3 -> new FoundryStatus(
                    Component.literal("MISSING FUEL"),
                    FoundryControllerUiDrawing.RED
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
                    FoundryControllerUiDrawing.ORANGE
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
                    FoundryControllerUiDrawing.ORANGE
            );
            default -> new FoundryStatus(
                    Component.literal("READY"),
                    FoundryControllerUiDrawing.GREEN
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
            int color
    ) {
    }
}
