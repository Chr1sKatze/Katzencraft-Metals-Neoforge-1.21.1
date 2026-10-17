package net.chriskatze.katzencraftmetals.screen;

import net.chriskatze.katzencraftmetals.menu.FoundryControllerMenu;

/** Pixel coordinates for the user-authored 282 x 232 Controller texture. */
final class FoundryControllerUiLayout {

    static final int WIDTH = 282;
    static final int HEIGHT = 232;

    // Atlas dimensions above stay unchanged, including for the Faucet Lock.
    static final int GUI_HEIGHT = FoundryControllerMenu.PLAYER_INVENTORY_Y + 77;
    static final int VERTICAL_COMPACTION = HEIGHT - GUI_HEIGHT;
    static final int HEADER_HEIGHT = 15;
    static final int PANEL_TOP = 2 + HEADER_HEIGHT + 2;
    static final int PANEL_BOTTOM = 145 - VERTICAL_COMPACTION;
    static final int PANEL_HEIGHT = PANEL_BOTTOM - PANEL_TOP;
    static final int THREE_ROW_LIST_HEIGHT = 61;
    static final int METAL_ALLOY_GAP = 5;
    static final int RIGHT_CONTENT_OFFSET_Y = 4;
    static final int ALLOY_SECTION_OFFSET_Y = 1;

    // Visible seven-pixel texture glyphs, one-pixel separators, and the
    // approved ALLOY RECIPES spacing. Gaps count empty GUI-pixel rows.
    static final int HEADING_HEIGHT = 7;
    static final int HEADING_UNDERLINE_GAP = 2;
    static final int UNDERLINE_HEIGHT = 1;
    static final int UNDERLINE_CONTENT_GAP = 3;
    static final int PROCESS_HEADING_Y = 26;
    static final int TANK_HEADING_Y = 26;
    static final int ALLOY_HEADING_Y = 98 - VERTICAL_COMPACTION + RIGHT_CONTENT_OFFSET_Y + ALLOY_SECTION_OFFSET_Y;
    static final int METAL_HEADING_Y = ALLOY_HEADING_Y - ALLOY_SECTION_OFFSET_Y - METAL_ALLOY_GAP
            - THREE_ROW_LIST_HEIGHT - HEADING_HEIGHT - HEADING_UNDERLINE_GAP
            - UNDERLINE_HEIGHT - UNDERLINE_CONTENT_GAP;
    static final int PROCESS_UNDERLINE_Y = underlineY(PROCESS_HEADING_Y);
    static final int TANK_UNDERLINE_Y = underlineY(TANK_HEADING_Y);
    static final int METAL_UNDERLINE_Y = underlineY(METAL_HEADING_Y);
    static final int ALLOY_UNDERLINE_Y = underlineY(ALLOY_HEADING_Y);
    static final int PROCESS_CONTENT_Y = contentY(PROCESS_HEADING_Y);

    /*
     * Actual 16 x 16 item-slot positions. The authored 18 x 18 frames begin
     * one pixel above and left of these coordinates.
     */
    static final int[] INPUT_SLOT_X = {
            38, 38, 57, 57, 76, 76, 95, 95
    };

    static final int[] INPUT_SLOT_Y = {
            FoundryControllerMenu.INPUT_FIRST_ROW_Y, FoundryControllerMenu.INPUT_FIRST_ROW_Y + 19,
            FoundryControllerMenu.INPUT_FIRST_ROW_Y, FoundryControllerMenu.INPUT_FIRST_ROW_Y + 19,
            FoundryControllerMenu.INPUT_FIRST_ROW_Y, FoundryControllerMenu.INPUT_FIRST_ROW_Y + 19,
            FoundryControllerMenu.INPUT_FIRST_ROW_Y, FoundryControllerMenu.INPUT_FIRST_ROW_Y + 19
    };

    static final int PROCESS_UNDERLINE_X = INPUT_SLOT_X[0] - 1;
    static final int PROCESS_UNDERLINE_WIDTH = INPUT_SLOT_X[INPUT_SLOT_X.length - 1]
            + 17 - PROCESS_UNDERLINE_X;
    static final int FUEL_SLOT_START_X = 38;
    static final int FUEL_SLOT_Y = FoundryControllerMenu.FUEL_SLOT_Y;
    static final int PROCESS_CLUSTER_OFFSET_X = -3;
    static final int PROCESS_CLUSTER_OFFSET_Y = FoundryControllerMenu.INPUT_FIRST_ROW_Y - 56;
    static final int SLOT_SPACING = 19;

    /*
     * These dimensions match foundry_controller_fuel_mask.png.
     * The visible fuel fill shape comes from the mask alpha.
     */
    static final int BURN_BAR_X = 38;
    static final int BURN_BAR_Y = FUEL_SLOT_Y + 21;
    static final int BURN_LABEL_Y = BURN_BAR_Y + 12;
    static final int BURN_BAR_WIDTH = 73;
    static final int BURN_BAR_HEIGHT = 8;

    /*
     * The original process mask is extended by repeating its straight stem.
     * The progress mask includes the round bulb and the thin vertical pillar.
     */
    static final int PROGRESS_UNDERLINE_X = 10;
    static final int PROGRESS_UNDERLINE_WIDTH = 21;
    static final int PROGRESS_GAUGE_X = PROGRESS_UNDERLINE_X
            + (PROGRESS_UNDERLINE_WIDTH - 13) / 2;
    static final int CLOCK_SIZE = 9;
    static final int CLOCK_X = PROGRESS_GAUGE_X + (13 - CLOCK_SIZE) / 2;
    static final int CLOCK_Y = PROCESS_HEADING_Y + (HEADING_HEIGHT - CLOCK_SIZE) / 2;
    static final int PROGRESS_GAUGE_Y = FoundryControllerMenu.INPUT_FIRST_ROW_Y - 1;
    static final int PROGRESS_GAUGE_WIDTH = 13;
    static final int PROGRESS_GAUGE_HEIGHT = BURN_BAR_Y + BURN_BAR_HEIGHT - PROGRESS_GAUGE_Y;

    /*
     * The original 72-pixel tank mask is mapped onto the GUI vessel.
     * The molten metal renderer keeps using the real molten textures and
     * renders them inside this mask's bounding box.
     */
    static final int TANK_GAUGE_X = 133;
    static final int TANK_FRAME_Y = contentY(TANK_HEADING_Y);
    static final int TANK_GAUGE_Y = TANK_FRAME_Y + 1;
    static final int TANK_GAUGE_WIDTH = 28;
    static final int TANK_UNDERLINE_X = TANK_GAUGE_X - 1;
    static final int TANK_UNDERLINE_WIDTH = TANK_GAUGE_WIDTH + 2;
    static final int TANK_GAUGE_HEIGHT = BURN_LABEL_Y - 3 - 1 - TANK_GAUGE_Y;

    static final int TANK_COUNT_X = 124;
    static final int TANK_COUNT_Y = BURN_LABEL_Y;
    static final int TANK_COUNT_WIDTH = 46;
    static final int TANK_COUNT_HEIGHT = 9;

    static final int METAL_LIST_X = 179;
    static final int METAL_LIST_Y = contentY(METAL_HEADING_Y);
    static final int METAL_LIST_WIDTH = 86;
    static final int METAL_ROW_HEIGHT = 19;
    static final int METAL_ROW_STRIDE = 21;
    static final int VISIBLE_METAL_ROWS = 3;

    static final int METAL_SCROLL_X = 267;
    static final int METAL_SCROLL_Y = METAL_LIST_Y;
    static final int METAL_SCROLL_WIDTH = 6;
    static final int METAL_SCROLL_HEIGHT = 61;

    static final int ALLOY_LIST_X = 179;
    static final int ALLOY_LIST_Y = contentY(ALLOY_HEADING_Y);
    static final int ALLOY_LIST_WIDTH = 86;
    static final int ALLOY_ROW_HEIGHT = 19;
    static final int ALLOY_ROW_STRIDE = 21;
    static final int VISIBLE_ALLOY_ROWS = 3;

    static final int ALLOY_SCROLL_X = 267;
    static final int ALLOY_SCROLL_Y = ALLOY_LIST_Y;
    static final int ALLOY_SCROLL_WIDTH = 6;
    static final int ALLOY_SCROLL_HEIGHT = 61;

    static final int[] ALLOY_INGREDIENT_SLOT_X = {
            180, 199, 218
    };

    static final int ALLOY_RECIPE_SLOT_Y = 184 - VERTICAL_COMPACTION;
    static final int ALLOY_RESULT_SLOT_X = 256;

    static final int PLAYER_INVENTORY_X = 7;
    static final int PLAYER_INVENTORY_Y = FoundryControllerMenu.PLAYER_INVENTORY_Y;

    static final int QUANTITY_MINUS_X = 179;
    static final int QUANTITY_PLUS_X = 213;

    static final int QUANTITY_BUTTON_Y = 207 - VERTICAL_COMPACTION;
    static final int QUANTITY_MINUS_WIDTH = 11;
    static final int QUANTITY_PLUS_WIDTH = 11;
    static final int QUANTITY_BUTTON_HEIGHT = 15;

    static final int QUANTITY_EDITBAR_X = 192;
    static final int QUANTITY_EDITBAR_Y = 207 - VERTICAL_COMPACTION;
    static final int QUANTITY_EDITBAR_WIDTH = 19;
    static final int QUANTITY_EDITBAR_HEIGHT = 15;

    /*
     * The EditBox has no border of its own. Its x position is adjusted at
     * runtime so the entered number is horizontally centered in the edit bar.
     */
    static final int QUANTITY_TEXT_Y = 211 - VERTICAL_COMPACTION;
    static final int QUANTITY_TEXT_HEIGHT = 10;

    static final int START_X = 226;
    static final int START_Y = 207 - VERTICAL_COMPACTION;
    static final int START_TEXT_Y = 208 - VERTICAL_COMPACTION;
    static final int START_WIDTH = 49;
    static final int START_HEIGHT = 15;

    private FoundryControllerUiLayout() {
    }

    private static int underlineY(int headingY) {
        return headingY + HEADING_HEIGHT + HEADING_UNDERLINE_GAP;
    }

    private static int contentY(int headingY) {
        return underlineY(headingY) + UNDERLINE_HEIGHT + UNDERLINE_CONTENT_GAP;
    }

    static int processMaskSourceY(int y) {
        // Extend only the straight stem; preserve the original cap and bulb.
        int stemDelta = PROGRESS_GAUGE_HEIGHT - 72;
        return y < 6 ? y : y < 6 + stemDelta ? 6 : y - stemDelta;
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
