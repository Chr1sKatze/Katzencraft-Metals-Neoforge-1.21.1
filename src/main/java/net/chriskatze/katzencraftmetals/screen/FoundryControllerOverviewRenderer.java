package net.chriskatze.katzencraftmetals.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Coordinator for the Overview tab.
 *
 * Each large panel has its own renderer so future changes do not turn this
 * screen back into a single oversized class.
 */
final class FoundryControllerOverviewRenderer {

    private final FoundryControllerScreen screen;

    private final FoundryControllerHeaderRenderer headerRenderer;
    private final FoundryControllerProcessPanelRenderer processRenderer;
    private final FoundryControllerTankPanelRenderer tankRenderer;
    private final FoundryControllerMetalsPanelRenderer metalsRenderer;

    FoundryControllerOverviewRenderer(
            FoundryControllerScreen screen
    ) {
        this.screen =
                screen;

        this.headerRenderer =
                new FoundryControllerHeaderRenderer(
                        screen
                );

        this.processRenderer =
                new FoundryControllerProcessPanelRenderer(
                        screen
                );

        this.tankRenderer =
                new FoundryControllerTankPanelRenderer(
                        screen
                );

        this.metalsRenderer =
                new FoundryControllerMetalsPanelRenderer(
                        screen
                );
    }

    void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY
    ) {
        headerRenderer.render(
                graphics
        );

        processRenderer.render(
                graphics
        );

        tankRenderer.render(
                graphics
        );

        metalsRenderer.render(
                graphics
        );


    }

    boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (
                button == 0
                        && isReservedTabClick(
                        mouseX,
                        mouseY
                )
        ) {
            return true;
        }

        return metalsRenderer.mouseClicked(
                mouseX,
                mouseY,
                button
        );
    }

    void renderTooltips(
            GuiGraphics graphics,
            int mouseX,
            int mouseY
    ) {
        int left =
                screen.guiLeft();

        int top =
                screen.guiTop();

        if (
                FoundryControllerUiLayout.contains(
                        mouseX,
                        mouseY,
                        left,
                        top,
                        FoundryControllerUiLayout.ALLOYS_TAB_X,
                        FoundryControllerUiLayout.TAB_Y,
                        FoundryControllerUiLayout.ALLOYS_TAB_WIDTH,
                        FoundryControllerUiLayout.TAB_HEIGHT
                )
        ) {
            graphics.renderTooltip(
                    screen.uiFont(),
                    Component.literal(
                            "Alloy recipes and batch controls arrive in the Alloys step."
                    ),
                    mouseX,
                    mouseY
            );

            return;
        }

        if (
                FoundryControllerUiLayout.contains(
                        mouseX,
                        mouseY,
                        left,
                        top,
                        FoundryControllerUiLayout.FAUCETS_TAB_X,
                        FoundryControllerUiLayout.TAB_Y,
                        FoundryControllerUiLayout.FAUCETS_TAB_WIDTH,
                        FoundryControllerUiLayout.TAB_HEIGHT
                )
        ) {
            graphics.renderTooltip(
                    screen.uiFont(),
                    Component.literal(
                            "Per-Faucet output routing will live on this tab."
                    ),
                    mouseX,
                    mouseY
            );

            return;
        }

        metalsRenderer.renderTooltips(
                graphics,
                mouseX,
                mouseY
        );
    }

    private boolean isReservedTabClick(
            double mouseX,
            double mouseY
    ) {
        int left =
                screen.guiLeft();

        int top =
                screen.guiTop();

        return FoundryControllerUiLayout.contains(
                mouseX,
                mouseY,
                left,
                top,
                FoundryControllerUiLayout.ALLOYS_TAB_X,
                FoundryControllerUiLayout.TAB_Y,
                FoundryControllerUiLayout.ALLOYS_TAB_WIDTH,
                FoundryControllerUiLayout.TAB_HEIGHT
        )
                || FoundryControllerUiLayout.contains(
                mouseX,
                mouseY,
                left,
                top,
                FoundryControllerUiLayout.FAUCETS_TAB_X,
                FoundryControllerUiLayout.TAB_Y,
                FoundryControllerUiLayout.FAUCETS_TAB_WIDTH,
                FoundryControllerUiLayout.TAB_HEIGHT
        );
    }
}
