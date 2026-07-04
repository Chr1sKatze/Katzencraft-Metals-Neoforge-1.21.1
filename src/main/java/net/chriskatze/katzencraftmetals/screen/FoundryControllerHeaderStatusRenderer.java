package net.chriskatze.katzencraftmetals.screen;

import net.chriskatze.katzencraftmetals.menu.FoundryControllerMenu;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

final class FoundryControllerHeaderStatusRenderer {

    private final FoundryControllerScreen screen;

    FoundryControllerHeaderStatusRenderer(
            FoundryControllerScreen screen
    ) {
        this.screen = screen;
    }

    void render(
            GuiGraphics graphics
    ) {
        renderTier(graphics);
        renderExperience(graphics);
        renderStatus(graphics);
    }

    private void renderTier(
            GuiGraphics graphics
    ) {
        Component tier =
                Component.literal(
                        "TIER "
                                + menu().getFoundryTier()
                );

        graphics.drawString(
                screen.uiFont(),
                tier,
                screen.guiLeft()
                        + 278
                        - screen.uiFont().width(tier),
                screen.guiTop() + 7,
                FoundryControllerUiDrawing.TEXT,
                false
        );
    }

    private void renderExperience(
            GuiGraphics graphics
    ) {
        int left =
                screen.guiLeft()
                        + FoundryControllerUiLayout.XP_BAR_X;

        int top =
                screen.guiTop()
                        + FoundryControllerUiLayout.XP_BAR_Y;

        int fill =
                menu().getScaledExperience(
                        FoundryControllerUiLayout.XP_BAR_WIDTH
                );

        if (fill > 0) {
            graphics.fill(
                    left,
                    top,
                    left + fill,
                    top + FoundryControllerUiLayout.XP_BAR_HEIGHT,
                    0xFF4CA83D
            );

            if (fill > 3) {
                graphics.fill(
                        left + 1,
                        top + 1,
                        left + fill - 1,
                        top + 2,
                        0xFF75CF5E
                );
            }
        }

        Component experienceText =
                menu().getFoundryTier() >= 4
                        ? Component.literal("MAX")
                        : Component.literal(
                        menu().getTierExperience()
                                + " / "
                                + menu().getTierExperienceNeeded()
                );

        Font font = screen.uiFont();

        graphics.drawString(
                font,
                experienceText,
                left
                        + FoundryControllerUiLayout.XP_BAR_WIDTH
                        - font.width(experienceText)
                        - 3,
                top,
                FoundryControllerUiDrawing.TEXT,
                false
        );
    }

    private void renderStatus(
            GuiGraphics graphics
    ) {
        FoundryStatus status = resolveStatus();

        graphics.drawString(
                screen.uiFont(),
                status.text(),
                screen.guiLeft()
                        + FoundryControllerUiLayout.STATUS_TEXT_X,
                screen.guiTop()
                        + FoundryControllerUiLayout.STATUS_TEXT_Y,
                status.color(),
                false
        );

        int progress =
                menu().getScaledProgress(
                        FoundryControllerUiLayout.STATUS_PROGRESS_WIDTH
                );

        if (progress > 0) {
            int left =
                    screen.guiLeft()
                            + FoundryControllerUiLayout.STATUS_PROGRESS_X;

            int top =
                    screen.guiTop()
                            + FoundryControllerUiLayout.STATUS_PROGRESS_Y;

            graphics.fill(
                    left,
                    top,
                    left + progress,
                    top + FoundryControllerUiLayout.STATUS_PROGRESS_HEIGHT,
                    FoundryControllerUiDrawing.ORANGE
            );
        }
    }

    private FoundryStatus resolveStatus() {
        if (menu().getTankCount() <= 0) {
            return new FoundryStatus(
                    Component.literal("NO TANKS CONNECTED"),
                    FoundryControllerUiDrawing.RED
            );
        }

        if (
                menu().getTankCapacity() > 0
                        && menu().getTotalMoltenAmount()
                        >= menu().getTankCapacity()
        ) {
            return new FoundryStatus(
                    Component.literal("TANK FULL"),
                    FoundryControllerUiDrawing.RED
            );
        }

        ItemStack input = menu().getInputStack();

        if (input.isEmpty()) {
            return new FoundryStatus(
                    Component.literal("READY"),
                    FoundryControllerUiDrawing.GREEN
            );
        }

        if (!menu().hasFuelAvailable()) {
            return new FoundryStatus(
                    Component.literal("MISSING FUEL"),
                    FoundryControllerUiDrawing.RED
            );
        }

        Component metal =
                menu().getInputMoltenMetalDefinition()
                        .<Component>map(
                                definition ->
                                        Component.translatable(
                                                definition.translationKey()
                                        )
                        )
                        .orElse(
                                Component.literal("ORE")
                        );

        return new FoundryStatus(
                Component.literal("MELTING ")
                        .append(metal),
                FoundryControllerUiDrawing.ORANGE
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
