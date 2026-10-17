package net.chriskatze.katzencraftmetals.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Locale;

final class FoundryControllerUiDrawing {

    static final int HEADING_TEXT = 0xFFFFFFFF;
    static final int TEXT = 0xFFE4E4E4;
    static final int MUTED_TEXT = 0xFF9A9A9A;
    static final int DARK_TEXT = 0xFF242424;

    static final int ORANGE = 0xFFFF941F;
    static final int ORANGE_DARK = 0xFFB9550C;
    static final int GREEN = 0xFF62B34E;
    static final int RED = 0xFFD64B3F;
    static final int AMBER = 0xFFE2A43A;

    private FoundryControllerUiDrawing() {
    }

    static void drawCentered(
            GuiGraphics graphics,
            Font font,
            Component text,
            int x,
            int y,
            int width,
            int height,
            int color
    ) {
        graphics.drawString(
                font,
                text,
                x + (width - font.width(text)) / 2,
                y + Math.max(
                        0,
                        (height - font.lineHeight) / 2
                ),
                color,
                false
        );
    }

    /** Nine-pixel clock; transparent corners preserve the existing panel. */
    private static final String[] CLOCK_PIXELS = {
            "001111100", "012222210", "122232221", "122232221", "122233321",
            "122222221", "122222221", "012222210", "001111100"
    };

    static void drawClock(GuiGraphics graphics, int x, int y) {
        for (int row = 0; row < CLOCK_PIXELS.length; row++) {
            for (int column = 0; column < CLOCK_PIXELS[row].length(); column++) {
                char pixel = CLOCK_PIXELS[row].charAt(column);
                if (pixel != '0') {
                    int color = pixel == '1' ? 0xFF969696 : pixel == '2' ? 0xFF353535 : 0xFFE4E4E4;
                    graphics.fill(x + column, y + row, x + column + 1, y + row + 1, color);
                }
            }
        }
    }

    /** Thin native-pixel bevel shared by the existing recipe controls. */
    static void drawControlFrame(GuiGraphics graphics, int x, int y, int width, int height,
                                 boolean enabled, boolean hovered, boolean pressed, boolean recessed) {
        int light = !enabled ? 0xFF454545 : hovered ? 0xFF949494 : 0xFF747474;
        int dark = !enabled ? 0xFF252525 : 0xFF292929;
        if (pressed || recessed) {
            int swap = light;
            light = dark;
            dark = swap;
        }
        graphics.renderOutline(x, y, width, height, 0xFF121212);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 2, light);
        graphics.fill(x + 1, y + 2, x + 2, y + height - 1, light);
        graphics.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, dark);
        graphics.fill(x + width - 2, y + 1, x + width - 1, y + height - 2, dark);
    }

    /** Shared native 18px active-slot edge for melting and alloy output. */
    static void drawActiveSlotOutline(GuiGraphics graphics, int left, int top) {
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

    static String formatOreAmount(
            int moltenUnits,
            int unitsPerOre
    ) {
        if (unitsPerOre <= 0) {
            return "0";
        }

        int hundredths =
                Math.round(
                        moltenUnits
                                * 100.0f
                                / unitsPerOre
                );

        int whole = hundredths / 100;
        int fraction = Math.floorMod(hundredths, 100);

        if (fraction == 0) {
            return Integer.toString(whole);
        }

        if (fraction % 10 == 0) {
            return whole
                    + "."
                    + fraction / 10;
        }

        return String.format(
                Locale.ROOT,
                "%d.%02d",
                whole,
                fraction
        );
    }
}
