package net.chriskatze.katzencraftmetals.screen;

/** Pixel coordinates for the user-authored 284 x 236 Controller texture. */
final class FoundryControllerUiLayout {

    static final int WIDTH = 284;
    static final int HEIGHT = 236;

    static final int XP_BAR_X = 30;
    static final int XP_BAR_Y = 25;
    static final int XP_BAR_WIDTH = 248;
    static final int XP_BAR_HEIGHT = 8;

    static final int STATUS_TEXT_X = 55;
    static final int STATUS_TEXT_Y = 44;

    static final int STATUS_PROGRESS_X = 184;
    static final int STATUS_PROGRESS_Y = 44;
    static final int STATUS_PROGRESS_WIDTH = 93;
    static final int STATUS_PROGRESS_HEIGHT = 8;

    static final int[] INPUT_SLOT_X = {
            43,
            43,
            62,
            62,
            81,
            81,
            100,
            100
    };

    static final int[] INPUT_SLOT_Y = {
            78,
            97,
            78,
            97,
            78,
            97,
            78,
            97
    };

    static final int FUEL_SLOT_START_X = 43;
    static final int FUEL_SLOT_Y = 121;
    static final int SLOT_SPACING = 19;

    static final int BURN_BAR_X = 43;
    static final int BURN_BAR_Y = 143;
    static final int BURN_BAR_WIDTH = 75;
    static final int BURN_BAR_HEIGHT = 7;

    static final int TEMPERATURE_GAUGE_X = 17;
    static final int TEMPERATURE_GAUGE_Y = 79;
    static final int TEMPERATURE_GAUGE_WIDTH = 16;
    static final int TEMPERATURE_GAUGE_HEIGHT = 70;

    static final int TANK_GAUGE_X = 137;
    static final int TANK_GAUGE_Y = 79;
    static final int TANK_GAUGE_WIDTH = 27;
    static final int TANK_GAUGE_HEIGHT = 70;

    static final int METAL_LIST_X = 182;
    static final int METAL_LIST_Y = 78;
    static final int METAL_LIST_WIDTH = 86;
    static final int METAL_ROW_HEIGHT = 22;
    static final int VISIBLE_METAL_ROWS = 4;

    static final int METAL_SCROLL_X = 271;
    static final int METAL_SCROLL_Y = 78;
    static final int METAL_SCROLL_WIDTH = 5;
    static final int METAL_SCROLL_HEIGHT = 86;

    static final int PLAYER_INVENTORY_X = 8;
    static final int PLAYER_INVENTORY_Y = 177;

    static final int INGREDIENT_ONE_X = 182;
    static final int INGREDIENT_TWO_X = 201;
    static final int INGREDIENT_THREE_X = 220;
    static final int RESULT_X = 260;
    static final int RECIPE_SLOT_Y = 167;

    static final int REQUIRED_TEMPERATURE_Y = 201;

    static final int QUANTITY_MINUS_X = 181;
    static final int QUANTITY_X = 194;
    static final int QUANTITY_PLUS_X = 215;
    static final int QUANTITY_Y = 217;

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
