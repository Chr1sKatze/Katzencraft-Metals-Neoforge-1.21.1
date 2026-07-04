package net.chriskatze.katzencraftmetals.screen;

import net.minecraft.client.gui.GuiGraphics;

/** Coordinates the independently sized sections of the final Controller UI. */
final class FoundryControllerOverviewRenderer {

    private final FoundryControllerHeaderStatusRenderer headerRenderer;
    private final FoundryControllerProcessRenderer processRenderer;
    private final FoundryControllerTankRenderer tankRenderer;
    private final FoundryControllerMetalsRenderer metalsRenderer;

    FoundryControllerOverviewRenderer(
            FoundryControllerScreen screen
    ) {
        this.headerRenderer =
                new FoundryControllerHeaderStatusRenderer(screen);

        this.processRenderer =
                new FoundryControllerProcessRenderer(screen);

        this.tankRenderer =
                new FoundryControllerTankRenderer(screen);

        this.metalsRenderer =
                new FoundryControllerMetalsRenderer(screen);
    }

    void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY
    ) {
        headerRenderer.render(graphics);
        processRenderer.render(graphics);
        tankRenderer.render(graphics);
        metalsRenderer.render(graphics);
    }

    boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        return metalsRenderer.mouseClicked(
                mouseX,
                mouseY,
                button
        );
    }

    boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double scrollDelta
    ) {
        return metalsRenderer.mouseScrolled(
                mouseX,
                mouseY,
                scrollDelta
        );
    }

    void renderTooltips(
            GuiGraphics graphics,
            int mouseX,
            int mouseY
    ) {
        metalsRenderer.renderTooltips(
                graphics,
                mouseX,
                mouseY
        );
    }
}
