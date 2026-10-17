package net.chriskatze.katzencraftmetals.screen;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Shared pixel-perfect list-row and scrollbar drawing for the Controller UI. */
final class FoundryControllerListDrawing {

    static final int ROW_OUTLINE = 0xFF121212;

    private static final ResourceLocation ROW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    KatzencraftMetalsMod.MODID,
                    "textures/gui/foundry_controller_list_row.png"
            );

    private static final ResourceLocation SCROLLBAR_TRACK_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    KatzencraftMetalsMod.MODID,
                    "textures/gui/foundry_controller_scrollbar_track.png"
            );

    private static final ResourceLocation SCROLLBAR_HANDLE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    KatzencraftMetalsMod.MODID,
                    "textures/gui/foundry_controller_scrollbar_handle.png"
            );

    private static final int ROW_WIDTH = 86;
    private static final int ROW_HEIGHT = 19;
    private static final int TRACK_TILE_SIZE = 4;
    private static final int HANDLE_WIDTH = 6;
    private static final int HANDLE_HEIGHT = 17;

    private FoundryControllerListDrawing() {
    }

    /** Exact row accents used by the finalized Faucet Lock screen. */
    static void drawMetalRowAccents(
            GuiGraphics graphics, int left, int top,
            boolean selected, boolean hovered
    ) {
        if (hovered && !selected) {
            graphics.fill(left + 2, top + 1,
                    left + ROW_WIDTH - 2, top + ROW_HEIGHT - 1, 0x18FFFFFF);
        }
        if (selected) {
            for (int y = 1; y < ROW_HEIGHT - 1; y++) {
                for (int x = 1; x <= 2; x++) {
                    graphics.fill(left + x, top + y, left + x + 1, top + y + 1,
                            (x + y) % 2 == 0 ? 0xFF4C361C : 0xFFB88432);
                }
            }
        }
    }

    /** Same dark rail outline and thumb bevel as Faucet Lock. */
    static void drawScrollbarFinish(
            GuiGraphics graphics, int left, int top, int height,
            int entryCount, int visibleRows, int offset
    ) {
        graphics.renderOutline(left, top, HANDLE_WIDTH, height, ROW_OUTLINE);
        int maxOffset = entryCount - visibleRows;
        if (maxOffset <= 0) {
            return;
        }
        int thumbTop = top + (height - HANDLE_HEIGHT) * offset / maxOffset;
        graphics.renderOutline(left, thumbTop, HANDLE_WIDTH, HANDLE_HEIGHT, 0xFF292929);
        graphics.fill(left + 1, thumbTop + 1, left + HANDLE_WIDTH - 1, thumbTop + 2,
                0xFF747474);
        graphics.fill(left + 2, thumbTop + HANDLE_HEIGHT / 2,
                left + HANDLE_WIDTH - 2, thumbTop + HANDLE_HEIGHT / 2 + 1,
                0xFF383838);
    }

    /** Faucet Lock's thin double rim, bounded by the current GUI viewport. */
    static void drawOuterFrame(
            GuiGraphics graphics, int left, int top, int width, int height
    ) {
        for (int inset = 2; inset >= 1; inset--) {
            int x = Math.max(0, left - inset);
            int y = Math.max(0, top - inset);
            int right = Math.min(graphics.guiWidth(), left + width + inset);
            int bottom = Math.min(graphics.guiHeight(), top + height + inset);
            if (right > x && bottom > y) {
                graphics.renderOutline(x, y, right - x, bottom - y,
                        inset == 2 ? 0xFF161616 : 0xFF626262);
            }
        }
    }

    static void drawRow(
            GuiGraphics graphics,
            int left,
            int top,
            boolean selected,
            boolean enabled
    ) {
        graphics.blit(
                ROW_TEXTURE,
                left,
                top,
                0.0f,
                0.0f,
                ROW_WIDTH,
                ROW_HEIGHT,
                ROW_WIDTH,
                ROW_HEIGHT
        );

        if (selected) {
            /*
             * The authored selected texture was too bright. Keep selection as a
             * subtle light lift on the normal card instead.
             */
            graphics.fill(
                    left + 2,
                    top + 1,
                    left + ROW_WIDTH - 2,
                    top + ROW_HEIGHT - 1,
                    0x22FFFFFF
            );

            graphics.fill(
                    left + 2,
                    top + 1,
                    left + ROW_WIDTH - 2,
                    top + 2,
                    0x33FFFFFF
            );
        }

        if (!enabled) {
            graphics.fill(
                    left + 3,
                    top + 2,
                    left + ROW_WIDTH - 1,
                    top + ROW_HEIGHT - 1,
                    0x66000000
            );
        }
    }

    /** Only occupied rows get a decorative rail, as in the fitted Faucet list. */
    static int visibleScrollbarHeight(int entryCount, int visibleRows, int rowHeight, int rowStride) {
        int rows = Math.min(Math.max(0, entryCount), visibleRows);
        return rows == 0 ? 0 : (rows - 1) * rowStride + rowHeight;
    }

    static void drawAdaptiveScrollbar(
            GuiGraphics graphics, int left, int top, int entryCount, int visibleRows,
            int rowHeight, int rowStride, int offset
    ) {
        int height = visibleScrollbarHeight(entryCount, visibleRows, rowHeight, rowStride);
        if (height == 0) return;
        if (entryCount > visibleRows) {
            drawScrollbar(graphics, left, top, height, entryCount, visibleRows, offset);
        } else {
            drawScrollbarTrack(graphics, left, top, height);
        }
        drawScrollbarFinish(graphics, left, top, height, entryCount, visibleRows, offset);
    }

    static void drawScrollbar(
            GuiGraphics graphics,
            int left,
            int top,
            int height,
            int entryCount,
            int visibleRows,
            int offset
    ) {
        drawScrollbarTrack(graphics, left, top, height);

        int maxOffset = Math.max(0, entryCount - visibleRows);
        int travel = Math.max(0, height - HANDLE_HEIGHT);
        int handleY = top + (maxOffset <= 0 ? 0 : travel * offset / maxOffset);

        graphics.blit(
                SCROLLBAR_HANDLE_TEXTURE,
                left, handleY,
                0.0f, 0.0f,
                HANDLE_WIDTH, HANDLE_HEIGHT,
                HANDLE_WIDTH, HANDLE_HEIGHT
        );
    }

    static void drawScrollbarTrack(
            GuiGraphics graphics,
            int left,
            int top,
            int height
    ) {
        graphics.fill(
                left,
                top,
                left + HANDLE_WIDTH,
                top + height,
                ROW_OUTLINE
        );

        int trackLeft = left + 1;
        int trackTop = top + 1;
        int trackBottom = top + height - 1;

        graphics.enableScissor(
                trackLeft,
                trackTop,
                trackLeft + TRACK_TILE_SIZE,
                trackBottom
        );

        for (
                int tileY = trackTop;
                tileY < trackBottom;
                tileY += TRACK_TILE_SIZE
        ) {
            graphics.blit(
                    SCROLLBAR_TRACK_TEXTURE,
                    trackLeft,
                    tileY,
                    0.0f,
                    0.0f,
                    TRACK_TILE_SIZE,
                    TRACK_TILE_SIZE,
                    TRACK_TILE_SIZE,
                    TRACK_TILE_SIZE
            );
        }

        graphics.disableScissor();

    }
}
