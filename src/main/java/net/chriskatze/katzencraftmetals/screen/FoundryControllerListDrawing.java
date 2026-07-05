package net.chriskatze.katzencraftmetals.screen;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Shared pixel-perfect list-row and scrollbar drawing for the Controller UI. */
final class FoundryControllerListDrawing {

    private static final ResourceLocation ROW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    KatzencraftMetalsMod.MODID,
                    "textures/gui/foundry_controller_list_row.png"
            );

    private static final ResourceLocation SELECTED_ROW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    KatzencraftMetalsMod.MODID,
                    "textures/gui/foundry_controller_list_row_selected.png"
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

    static void drawRow(
            GuiGraphics graphics,
            int left,
            int top,
            boolean selected,
            boolean enabled
    ) {
        graphics.blit(
                selected
                        ? SELECTED_ROW_TEXTURE
                        : ROW_TEXTURE,
                left,
                top,
                0.0f,
                0.0f,
                ROW_WIDTH,
                ROW_HEIGHT,
                ROW_WIDTH,
                ROW_HEIGHT
        );

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

    static void drawScrollbar(
            GuiGraphics graphics,
            int left,
            int top,
            int height,
            int entryCount,
            int visibleRows,
            int offset
    ) {
        graphics.fill(
                left,
                top,
                left + HANDLE_WIDTH,
                top + height,
                0xFF121212
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

        int maxOffset =
                Math.max(
                        0,
                        entryCount - visibleRows
                );

        int travel =
                Math.max(
                        0,
                        height - HANDLE_HEIGHT
                );

        int handleY =
                top
                        + (
                        maxOffset <= 0
                                ? 0
                                : travel * offset / maxOffset
                );

        graphics.blit(
                SCROLLBAR_HANDLE_TEXTURE,
                left,
                handleY,
                0.0f,
                0.0f,
                HANDLE_WIDTH,
                HANDLE_HEIGHT,
                HANDLE_WIDTH,
                HANDLE_HEIGHT
        );
    }
}
