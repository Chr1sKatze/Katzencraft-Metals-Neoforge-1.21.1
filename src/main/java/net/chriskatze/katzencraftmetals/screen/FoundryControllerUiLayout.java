package net.chriskatze.katzencraftmetals.screen;

/** Pixel coordinates for the user-authored 282 x 232 Controller texture. */
final class FoundryControllerUiLayout {

    static final int WIDTH = 282;
    static final int HEIGHT = 232;

    static final int XP_BAR_X = 30;
    static final int XP_BAR_Y = 25;
    static final int XP_BAR_WIDTH = 138;
    static final int XP_BAR_HEIGHT = 8;

    static final int STATUS_TEXT_X = 52;
    static final int STATUS_TEXT_Y = 218;

    /*
     * Actual 16 x 16 item-slot positions. The authored 18 x 18 frames begin
     * one pixel above and left of these coordinates.
     */
    static final int[] INPUT_SLOT_X = {
            41, 41, 60, 60, 79, 79, 98, 98
    };

    static final int[] INPUT_SLOT_Y = {
            56, 75, 56, 75, 56, 75, 56, 75
    };

    static final int FUEL_SLOT_START_X = 41;
    static final int FUEL_SLOT_Y = 99;
    static final int SLOT_SPACING = 19;

    static final int BURN_BAR_X = 41;
    static final int BURN_BAR_Y = 120;
    static final int BURN_BAR_WIDTH = 73;
    static final int BURN_BAR_HEIGHT = 8;

    static final int PROGRESS_GAUGE_X = 18;
    static final int PROGRESS_GAUGE_Y = 56;
    static final int PROGRESS_GAUGE_WIDTH = 5;
    static final int PROGRESS_GAUGE_HEIGHT = 64;

    static final int TANK_GAUGE_X = 135;
    static final int TANK_GAUGE_Y = 56;
    static final int TANK_GAUGE_WIDTH = 27;
    static final int TANK_GAUGE_HEIGHT = 70;

    static final int TANK_COUNT_X = 124;
    static final int TANK_COUNT_Y = 131;
    static final int TANK_COUNT_WIDTH = 46;
    static final int TANK_COUNT_HEIGHT = 12;

    static final int METAL_LIST_X = 179;
    static final int METAL_LIST_Y = 37;
    static final int METAL_LIST_WIDTH = 86;
    static final int METAL_ROW_HEIGHT = 19;
    static final int METAL_ROW_STRIDE = 21;
    static final int VISIBLE_METAL_ROWS = 4;

    static final int METAL_SCROLL_X = 267;
    static final int METAL_SCROLL_Y = 37;
    static final int METAL_SCROLL_WIDTH = 6;
    static final int METAL_SCROLL_HEIGHT = 82;

    static final int ALLOY_LIST_X = 179;
    static final int ALLOY_LIST_Y = 137;
    static final int ALLOY_LIST_WIDTH = 86;
    static final int ALLOY_ROW_HEIGHT = 19;
    static final int ALLOY_ROW_STRIDE = 21;
    static final int VISIBLE_ALLOY_ROWS = 2;

    static final int ALLOY_SCROLL_X = 267;
    static final int ALLOY_SCROLL_Y = 137;
    static final int ALLOY_SCROLL_WIDTH = 6;
    static final int ALLOY_SCROLL_HEIGHT = 40;

    static final int[] ALLOY_INGREDIENT_SLOT_X = {
            180, 199, 218
    };

    static final int ALLOY_RECIPE_SLOT_Y = 184;
    static final int ALLOY_RESULT_SLOT_X = 256;

    static final int PLAYER_INVENTORY_X = 7;
    static final int PLAYER_INVENTORY_Y = 155;

    static final int QUANTITY_MINUS_X = 179;
    static final int QUANTITY_X = 193;
    static final int QUANTITY_PLUS_X = 213;
    static final int QUANTITY_Y = 209;

    static final int QUANTITY_MINUS_WIDTH = 11;
    static final int QUANTITY_WIDTH = 17;
    static final int QUANTITY_PLUS_WIDTH = 11;
    static final int QUANTITY_HEIGHT = 10;
    static final int QUANTITY_BUTTON_Y = 207;
    static final int QUANTITY_BUTTON_HEIGHT = 15;

    static final int START_X = 226;
    static final int START_Y = 207;
    static final int START_WIDTH = 49;
    static final int START_HEIGHT = 15;

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
