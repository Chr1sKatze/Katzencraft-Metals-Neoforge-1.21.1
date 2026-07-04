package net.chriskatze.katzencraftmetals.screen;

import net.chriskatze.katzencraftmetals.menu.FoundryControllerMenu;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

final class FoundryControllerHeaderRenderer {

    private final FoundryControllerScreen screen;

    FoundryControllerHeaderRenderer(
            FoundryControllerScreen screen
    ) {
        this.screen =
                screen;
    }

    void render(
            GuiGraphics graphics
    ) {
        renderHeader(
                graphics
        );

        renderStatus(
                graphics
        );

        renderTabs(
                graphics
        );
    }

    private void renderHeader(
            GuiGraphics graphics
    ) {
        int left =
                screen.guiLeft();

        int top =
                screen.guiTop();

        Font font =
                screen.uiFont();

        graphics.drawString(
                font,
                screen.controllerTitle(),
                left + 27,
                top + 9,
                FoundryControllerUiDrawing.DARK_TEXT,
                false
        );

        Component processBadge =
                getProcessBadge();

        graphics.drawString(
                font,
                processBadge,
                left + 247
                        - font.width(
                        processBadge
                ),
                top + 9,
                FoundryControllerUiDrawing.ORANGE,
                false
        );
    }

    private Component getProcessBadge() {
        ItemStack input =
                menu().getInputStack();

        if (input.isEmpty()) {
            return Component.literal(
                    "Idle"
            );
        }

        return menu().getInputMoltenMetalDefinition()
                .<Component>map(
                        definition ->
                                Component.literal(
                                        "Melting "
                                ).append(
                                        Component.translatable(
                                                definition.translationKey()
                                        )
                                )
                )
                .orElseGet(
                        () ->
                                Component.literal(
                                        "Melting"
                                )
                );
    }

    private void renderStatus(
            GuiGraphics graphics
    ) {
        int left =
                screen.guiLeft();

        int top =
                screen.guiTop();

        Font font =
                screen.uiFont();

        FoundryStatus status =
                resolveStatus();

        graphics.drawString(
                font,
                Component.literal(
                        "STATUS"
                ),
                left + 18,
                top + 28,
                FoundryControllerUiDrawing.TEXT,
                false
        );

        graphics.fill(
                left + 66,
                top + 29,
                left + 69,
                top + 32,
                status.color()
        );

        graphics.drawString(
                font,
                status.text(),
                left + 77,
                top + 28,
                status.color(),
                false
        );

        int scaledProgress =
                menu().getScaledProgress(
                        FoundryControllerUiLayout
                                .STATUS_PROGRESS_WIDTH
                );

        graphics.fill(
                left + FoundryControllerUiLayout.STATUS_PROGRESS_X,
                top + FoundryControllerUiLayout.STATUS_PROGRESS_Y,
                left + FoundryControllerUiLayout.STATUS_PROGRESS_X
                        + scaledProgress,
                top + FoundryControllerUiLayout.STATUS_PROGRESS_Y
                        + FoundryControllerUiLayout.STATUS_PROGRESS_HEIGHT,
                FoundryControllerUiDrawing.ORANGE
        );

        String percent =
                menu().getProgressPercent()
                        + "%";

        graphics.drawString(
                font,
                percent,
                left + 239
                        - font.width(
                        percent
                ),
                top + 28,
                FoundryControllerUiDrawing.ORANGE,
                false
        );
    }

    private FoundryStatus resolveStatus() {
        int capacity =
                menu().getTankCapacity();

        if (capacity <= 0) {
            return new FoundryStatus(
                    Component.literal(
                            "No Tanks connected"
                    ),
                    FoundryControllerUiDrawing.RED
            );
        }

        if (
                menu().getTotalMoltenAmount()
                        >= capacity
        ) {
            return new FoundryStatus(
                    Component.literal(
                            "Tank full"
                    ),
                    FoundryControllerUiDrawing.RED
            );
        }

        ItemStack input =
                menu().getInputStack();

        if (input.isEmpty()) {
            return new FoundryStatus(
                    Component.literal(
                            "Ready"
                    ),
                    FoundryControllerUiDrawing.GREEN
            );
        }

        if (!menu().hasFuelAvailable()) {
            return new FoundryStatus(
                    Component.literal(
                            "No fuel"
                    ),
                    FoundryControllerUiDrawing.RED
            );
        }

        if (menu().getProgress() > 0) {
            return new FoundryStatus(
                    Component.literal(
                            "Melting"
                    ),
                    FoundryControllerUiDrawing.ORANGE
            );
        }

        return new FoundryStatus(
                Component.literal(
                        "Ready to melt"
                ),
                FoundryControllerUiDrawing.AMBER
        );
    }

    private void renderTabs(
            GuiGraphics graphics
    ) {
        int left =
                screen.guiLeft();

        int top =
                screen.guiTop();

        Font font =
                screen.uiFont();

        FoundryControllerUiDrawing.drawCentered(
                graphics,
                font,
                Component.literal(
                        "Overview"
                ),
                left + FoundryControllerUiLayout.OVERVIEW_TAB_X,
                top + FoundryControllerUiLayout.TAB_Y,
                FoundryControllerUiLayout.OVERVIEW_TAB_WIDTH,
                FoundryControllerUiLayout.TAB_HEIGHT,
                FoundryControllerUiDrawing.TEXT
        );

        FoundryControllerUiDrawing.drawCentered(
                graphics,
                font,
                Component.literal(
                        "Alloys"
                ),
                left + FoundryControllerUiLayout.ALLOYS_TAB_X,
                top + FoundryControllerUiLayout.TAB_Y,
                FoundryControllerUiLayout.ALLOYS_TAB_WIDTH,
                FoundryControllerUiLayout.TAB_HEIGHT,
                FoundryControllerUiDrawing.MUTED_TEXT
        );

        FoundryControllerUiDrawing.drawCentered(
                graphics,
                font,
                Component.literal(
                        "Faucets"
                ),
                left + FoundryControllerUiLayout.FAUCETS_TAB_X,
                top + FoundryControllerUiLayout.TAB_Y,
                FoundryControllerUiLayout.FAUCETS_TAB_WIDTH,
                FoundryControllerUiLayout.TAB_HEIGHT,
                FoundryControllerUiDrawing.MUTED_TEXT
        );
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
