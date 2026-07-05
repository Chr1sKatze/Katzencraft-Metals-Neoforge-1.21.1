package net.chriskatze.katzencraftmetals.screen;

import net.chriskatze.katzencraftmetals.menu.FoundryControllerMenu;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

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
    }

    private FoundryStatus resolveStatus() {
        return switch (menu().getProcessingStatus()) {
            case 1 -> new FoundryStatus(
                    Component.literal("NO TANKS CONNECTED"),
                    FoundryControllerUiDrawing.RED
            );
            case 2 -> new FoundryStatus(
                    Component.literal("TANK FULL"),
                    FoundryControllerUiDrawing.RED
            );
            case 3 -> new FoundryStatus(
                    Component.literal("MISSING FUEL"),
                    FoundryControllerUiDrawing.RED
            );
            case 4 -> new FoundryStatus(
                    Component.literal("MELTING ").append(
                            menu().getInputMoltenMetalDefinition()
                                    .<Component>map(
                                            definition ->
                                                    Component.literal(
                                                            displayName(
                                                                    definition.id()
                                                            )
                                                    )
                                    )
                                    .orElse(
                                            Component.literal("ORE")
                                    )
                    ),
                    FoundryControllerUiDrawing.ORANGE
            );
            case 5 -> new FoundryStatus(
                    Component.literal("ALLOYING ").append(
                            menu().getActiveAlloyOutput()
                                    .<Component>map(
                                            definition ->
                                                    Component.literal(
                                                            displayName(
                                                                    definition.id()
                                                            )
                                                    )
                                    )
                                    .orElse(
                                            Component.literal("ALLOY")
                                    )
                    ),
                    FoundryControllerUiDrawing.ORANGE
            );
            default -> new FoundryStatus(
                    Component.literal("READY"),
                    FoundryControllerUiDrawing.GREEN
            );
        };
    }

    private static String displayName(
            ResourceLocation id
    ) {
        String path =
                id.getPath()
                        .replace(
                                '_',
                                ' '
                        );

        if (path.isEmpty()) {
            return "";
        }

        return Character.toUpperCase(
                path.charAt(0)
        )
                + path.substring(1);
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
