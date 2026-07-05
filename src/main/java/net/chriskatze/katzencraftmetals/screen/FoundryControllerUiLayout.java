package net.chriskatze.katzencraftmetals.screen;

/** Pixel coordinates for the user-authored 284 x 236 Controller texture. */
final class FoundryControllerUiLayout {

    static final int WIDTH = 284;
    static final int HEIGHT = 236;

    static final int XP_BAR_X = 30;
    static final int XP_BAR_Y = 25;
    static final int XP_BAR_WIDTH = 248;
    static final int XP_BAR_HEIGHT = 8;

    static final int STATUS_TEXT_X = 52;
    static final int STATUS_TEXT_Y = 220;

    static final int[] INPUT_SLOT_X = {
            43, 43, 62, 62, 81, 81, 100, 100
    };

    static final int[] INPUT_SLOT_Y = {
            60, 79, 60, 79, 60, 79, 60, 79
    };

    static final int FUEL_SLOT_START_X = 43;
    static final int FUEL_SLOT_Y = 103;
    static final int SLOT_SPACING = 19;

    static final int BURN_BAR_X = 43;
    static final int BURN_BAR_Y = 127;
    static final int BURN_BAR_WIDTH = 75;
    static final int BURN_BAR_HEIGHT = 7;

    /* Shared vertical progress display for melting and alloying. */
    static final int PROGRESS_GAUGE_X = 16;
    static final int PROGRESS_GAUGE_Y = 59;
    static final int PROGRESS_GAUGE_WIDTH = 5;
    static final int PROGRESS_GAUGE_HEIGHT = 69;

    static final int TANK_GAUGE_X = 137;
    static final int TANK_GAUGE_Y = 60;
    static final int TANK_GAUGE_WIDTH = 27;
    static final int TANK_GAUGE_HEIGHT = 70;

    static final int TANK_COUNT_X = 126;
    static final int TANK_COUNT_Y = 135;
    static final int TANK_COUNT_WIDTH = 46;
    static final int TANK_COUNT_HEIGHT = 12;

    static final int METAL_LIST_X = 182;
    static final int METAL_LIST_Y = 59;
    static final int METAL_LIST_WIDTH = 86;
    static final int METAL_ROW_HEIGHT = 22;
    static final int VISIBLE_METAL_ROWS = 3;

    static final int METAL_SCROLL_X = 271;
    static final int METAL_SCROLL_Y = 59;
    static final int METAL_SCROLL_WIDTH = 5;
    static final int METAL_SCROLL_HEIGHT = 64;

    static final int ALLOY_LIST_X = 182;
    static final int ALLOY_LIST_Y = 148;
    static final int ALLOY_LIST_WIDTH = 86;
    static final int ALLOY_ROW_HEIGHT = 22;
    static final int VISIBLE_ALLOY_ROWS = 3;

    static final int ALLOY_SCROLL_X = 271;
    static final int ALLOY_SCROLL_Y = 148;
    static final int ALLOY_SCROLL_WIDTH = 5;
    static final int ALLOY_SCROLL_HEIGHT = 65;

    static final int PLAYER_INVENTORY_X = 8;
    static final int PLAYER_INVENTORY_Y = 159;

    static final int QUANTITY_MINUS_X = 181;
    static final int QUANTITY_X = 194;
    static final int QUANTITY_PLUS_X = 215;
    static final int QUANTITY_Y = 217;

    static final int QUANTITY_MINUS_WIDTH = 11;
    static final int QUANTITY_WIDTH = 19;
    static final int QUANTITY_PLUS_WIDTH = 11;

    static final int START_X = 234;
    static final int START_Y = 217;
    static final int START_WIDTH = 43;
    static final int START_HEIGHT = 11;

    private FoundryControllerUiLayout() {
    }

    static boolean contains(
            double mouseX,
            double mouseY,
            int left,
            int top,
            int x,
            int y,
            int width,
            int height
    ) {
        return mouseX >= left + x
                && mouseX < left + x + width
                && mouseY >= top + y
                && mouseY < top + y + height;
    }
}
