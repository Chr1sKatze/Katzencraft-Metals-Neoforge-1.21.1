package net.chriskatze.katzencraftmetals.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Locale;

final class FoundryControllerUiDrawing {

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
