package net.chriskatze.katzencraftmetals.screen;

/**
 * Shared geometry for every Foundry Controller tab.
 *
 * The Overview now uses only two normal inventory rows plus the hotbar. The
 * saved row is given to the machine panels so labels and dynamic values have
 * enough breathing room at large Minecraft GUI scales.
 */
final class FoundryControllerUiLayout {

    static final int WIDTH = 260;
    static final int HEIGHT = 236;

    static final int TAB_Y = 42;
    static final int TAB_HEIGHT = 15;

    static final int OVERVIEW_TAB_X = 7;
    static final int OVERVIEW_TAB_WIDTH = 78;

    static final int ALLOYS_TAB_X = 86;
    static final int ALLOYS_TAB_WIDTH = 78;

    static final int FAUCETS_TAB_X = 165;
    static final int FAUCETS_TAB_WIDTH = 88;

    static final int PANEL_Y = 60;
    static final int PANEL_HEIGHT = 105;

    static final int PROCESS_X = 7;
    static final int PROCESS_WIDTH = 78;

    static final int TANK_X = 88;
    static final int TANK_WIDTH = 58;

    static final int METALS_X = 149;
    static final int METALS_WIDTH = 104;

    static final int INPUT_SLOT_X = 22;
    static final int INPUT_SLOT_Y = 84;

    static final int FUEL_SLOT_START_X = 13;
    static final int FUEL_SLOT_Y = 124;
    static final int SLOT_SPACING = 19;

    static final int PLAYER_INVENTORY_X = 49;
    static final int PLAYER_INVENTORY_Y = 177;
    static final int PLAYER_HOTBAR_Y = 213;

    static final int PROGRESS_X = 43;
    static final int PROGRESS_Y = 90;
    static final int PROGRESS_WIDTH = 20;
    static final int PROGRESS_HEIGHT = 7;

    static final int HEAT_X = 69;
    static final int HEAT_Y = 82;
    static final int HEAT_WIDTH = 9;
    static final int HEAT_HEIGHT = 60;

    static final int FUEL_BAR_X = 13;
    static final int FUEL_BAR_Y = 156;
    static final int FUEL_BAR_WIDTH = 58;
    static final int FUEL_BAR_HEIGHT = 4;

    static final int TANK_GAUGE_X = 98;
    static final int TANK_GAUGE_Y = 76;
    static final int TANK_GAUGE_WIDTH = 37;
    static final int TANK_GAUGE_HEIGHT = 71;

    static final int METAL_LIST_X = 154;
    static final int METAL_LIST_Y = 74;
    static final int METAL_LIST_WIDTH = 93;
    static final int METAL_ROW_HEIGHT = 22;
    static final int VISIBLE_METAL_ROWS = 4;

    static final int METAL_SCROLL_X = 248;
    static final int METAL_SCROLL_UP_Y = 74;
    static final int METAL_SCROLL_DOWN_Y = 142;
    static final int METAL_SCROLL_WIDTH = 4;
    static final int METAL_SCROLL_BUTTON_HEIGHT = 12;

    static final int METAL_DETAILS_X = 154;
    static final int METAL_DETAILS_Y = 156;
    static final int METAL_DETAILS_WIDTH = 98;
    static final int METAL_DETAILS_HEIGHT = 7;

    static final int STATUS_PROGRESS_X = 166;
    static final int STATUS_PROGRESS_Y = 29;
    static final int STATUS_PROGRESS_WIDTH = 52;
    static final int STATUS_PROGRESS_HEIGHT = 5;

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
