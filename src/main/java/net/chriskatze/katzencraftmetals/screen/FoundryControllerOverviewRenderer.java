package net.chriskatze.katzencraftmetals.screen;

import net.minecraft.client.gui.GuiGraphics;

/** Coordinates the independently sized sections of the Controller UI. */
final class FoundryControllerOverviewRenderer {

    private final FoundryControllerHeaderStatusRenderer headerRenderer;
    private final FoundryControllerProcessRenderer processRenderer;
    private final FoundryControllerTankRenderer tankRenderer;
    private final FoundryControllerMetalsRenderer metalsRenderer;
    private final FoundryControllerAlloysRenderer alloysRenderer;

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

        this.alloysRenderer =
                new FoundryControllerAlloysRenderer(screen);
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
        alloysRenderer.render(
                graphics,
                mouseX,
                mouseY
        );
    }

    boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (
                metalsRenderer.mouseClicked(
                        mouseX,
                        mouseY,
                        button
                )
        ) {
            return true;
        }

        return alloysRenderer.mouseClicked(
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
        if (
                metalsRenderer.mouseScrolled(
                        mouseX,
                        mouseY,
                        scrollDelta
                )
        ) {
            return true;
        }

        return alloysRenderer.mouseScrolled(
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

        alloysRenderer.renderTooltips(
                graphics,
                mouseX,
                mouseY
        );
    }
}
