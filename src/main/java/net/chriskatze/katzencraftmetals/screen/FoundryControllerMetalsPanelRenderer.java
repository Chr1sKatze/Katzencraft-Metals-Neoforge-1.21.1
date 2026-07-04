package net.chriskatze.katzencraftmetals.screen;

import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.chriskatze.katzencraftmetals.metal.MoltenMetalDefinition;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

final class FoundryControllerMetalsPanelRenderer {

    private final FoundryControllerScreen screen;

    private int metalScrollOffset;

    @Nullable
    private ResourceLocation focusedMetal;

    FoundryControllerMetalsPanelRenderer(
            FoundryControllerScreen screen
    ) {
        this.screen =
                screen;
    }

    void render(
            GuiGraphics graphics
    ) {
        normalizeFocusedMetal();

        int left =
                screen.guiLeft();

        int top =
                screen.guiTop();

        graphics.drawString(
                screen.uiFont(),
                Component.literal(
                        "METALS"
                ),
                left + 155,
                top + 64,
                FoundryControllerUiDrawing.TEXT,
                false
        );

        List<MoltenMetalDefinition> stored =
                getStoredMetals();

        if (stored.isEmpty()) {
            FoundryControllerUiDrawing.drawCentered(
                    graphics,
                    screen.uiFont(),
                    Component.literal(
                            "No molten metal stored"
                    ),
                    left + FoundryControllerUiLayout.METAL_LIST_X,
                    top + FoundryControllerUiLayout.METAL_LIST_Y + 28,
                    FoundryControllerUiLayout.METAL_LIST_WIDTH - 5,
                    18,
                    FoundryControllerUiDrawing.MUTED_TEXT
            );

            renderScrollbar(
                    graphics,
                    0
            );

            return;
        }

        clampScroll(
                stored.size()
        );

        for (
                int visibleRow = 0;
                visibleRow
                        < FoundryControllerUiLayout.VISIBLE_METAL_ROWS;
                visibleRow++
        ) {
            int index =
                    metalScrollOffset
                            + visibleRow;

            if (index >= stored.size()) {
                break;
            }

            renderMetalRow(
                    graphics,
                    stored.get(index),
                    visibleRow
            );
        }

        renderScrollbar(
                graphics,
                stored.size()
        );
    }

    boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (button != 0) {
            return false;
        }

        List<MoltenMetalDefinition> stored =
                getStoredMetals();

        if (handleScrollClick(
                mouseX,
                mouseY,
                stored.size()
        )) {
            return true;
        }

        int left =
                screen.guiLeft();

        int top =
                screen.guiTop();

        for (
                int visibleRow = 0;
                visibleRow
                        < FoundryControllerUiLayout.VISIBLE_METAL_ROWS;
                visibleRow++
        ) {
            int index =
                    metalScrollOffset
                            + visibleRow;

            if (index >= stored.size()) {
                break;
            }

            if (
                    !FoundryControllerUiLayout.contains(
                            mouseX,
                            mouseY,
                            left,
                            top,
                            FoundryControllerUiLayout.METAL_LIST_X,
                            FoundryControllerUiLayout.METAL_LIST_Y
                                    + visibleRow
                                    * FoundryControllerUiLayout.METAL_ROW_HEIGHT,
                            FoundryControllerUiLayout.METAL_LIST_WIDTH,
                            FoundryControllerUiLayout.METAL_ROW_HEIGHT
                    )
            ) {
                continue;
            }

            MoltenMetalDefinition definition =
                    stored.get(index);

            focusedMetal =
                    definition.id();

            int syncId =
                    ModMoltenMetals.getSyncId(
                            definition.id()
                    );

            if (syncId >= 0) {
                screen.sendMenuButton(
                        syncId
                );
            }

            return true;
        }

        return false;
    }

    void renderTooltips(
            GuiGraphics graphics,
            int mouseX,
            int mouseY
    ) {
        List<MoltenMetalDefinition> stored =
                getStoredMetals();

        int left =
                screen.guiLeft();

        int top =
                screen.guiTop();

        for (
                int visibleRow = 0;
                visibleRow
                        < FoundryControllerUiLayout.VISIBLE_METAL_ROWS;
                visibleRow++
        ) {
            int index =
                    metalScrollOffset
                            + visibleRow;

            if (index >= stored.size()) {
                break;
            }

            if (
                    FoundryControllerUiLayout.contains(
                            mouseX,
                            mouseY,
                            left,
                            top,
                            FoundryControllerUiLayout.METAL_LIST_X,
                            FoundryControllerUiLayout.METAL_LIST_Y
                                    + visibleRow
                                    * FoundryControllerUiLayout.METAL_ROW_HEIGHT,
                            FoundryControllerUiLayout.METAL_LIST_WIDTH,
                            FoundryControllerUiLayout.METAL_ROW_HEIGHT
                    )
            ) {
                graphics.renderTooltip(
                        screen.uiFont(),
                        Component.literal(
                                "Select this metal. Until the Faucets tab exists, this remains the shared Faucet output."
                        ),
                        mouseX,
                        mouseY
                );

                return;
            }
        }
    }

    private void renderMetalRow(
            GuiGraphics graphics,
            MoltenMetalDefinition definition,
            int visibleRow
    ) {
        int left =
                screen.guiLeft()
                        + FoundryControllerUiLayout.METAL_LIST_X;

        int top =
                screen.guiTop()
                        + FoundryControllerUiLayout.METAL_LIST_Y
                        + visibleRow
                        * FoundryControllerUiLayout.METAL_ROW_HEIGHT;

        boolean focused =
                definition.id()
                        .equals(
                                focusedMetal
                        );

        renderMetalRowFrame(
                graphics,
                left,
                top,
                focused
        );

        ItemStack icon =
                getDisplayIcon(
                        definition
                );

        if (!icon.isEmpty()) {
            graphics.renderItem(
                    icon,
                    left + 5,
                    top + 2
            );
        }

        int textColor =
                focused
                        ? FoundryControllerUiDrawing.DARK_TEXT
                        : FoundryControllerUiDrawing.TEXT;

        int subTextColor =
                focused
                        ? 0xFF474747
                        : FoundryControllerUiDrawing.MUTED_TEXT;

        graphics.drawString(
                screen.uiFont(),
                Component.translatable(
                        definition.translationKey()
                ),
                left + 24,
                top + 2,
                textColor,
                false
        );

        int amount =
                screen.controllerMenu()
                        .getMetalAmount(
                                definition
                        );

        graphics.drawString(
                screen.uiFont(),
                FoundryControllerUiDrawing.formatOreAmount(
                        amount,
                        definition.unitsPerOre()
                )
                        + " ore",
                left + 24,
                top + 12,
                subTextColor,
                false
        );
    }

    private void renderScrollbar(
            GuiGraphics graphics,
            int metalCount
    ) {
        int maxOffset =
                Math.max(
                        0,
                        metalCount
                                - FoundryControllerUiLayout.VISIBLE_METAL_ROWS
                );

        int left =
                screen.guiLeft()
                        + FoundryControllerUiLayout.METAL_SCROLL_X;

        int top =
                screen.guiTop()
                        + FoundryControllerUiLayout.METAL_SCROLL_UP_Y;

        int totalHeight =
                FoundryControllerUiLayout.VISIBLE_METAL_ROWS
                        * FoundryControllerUiLayout.METAL_ROW_HEIGHT;

        int trackBottom =
                top + totalHeight;

        int trackHeight =
                totalHeight;

        graphics.fill(
                left,
                top,
                left + FoundryControllerUiLayout.METAL_SCROLL_WIDTH,
                trackBottom,
                0xFF151515
        );

        graphics.fill(
                left + 1,
                top + 1,
                left + FoundryControllerUiLayout.METAL_SCROLL_WIDTH - 1,
                trackBottom - 1,
                0xFF2B2B2B
        );

        int thumbHeight =
                maxOffset <= 0
                        ? 12
                        : Math.max(
                        10,
                        trackHeight
                                * FoundryControllerUiLayout.VISIBLE_METAL_ROWS
                                / metalCount
                );

        int thumbTravel =
                Math.max(
                        0,
                        trackHeight - thumbHeight - 2
                );

        int thumbY =
                top + 1
                        + (
                        maxOffset <= 0
                                ? (trackHeight - thumbHeight - 2) / 2
                                : thumbTravel
                                * metalScrollOffset
                                / maxOffset
                );

        graphics.fill(
                left + 1,
                thumbY,
                left + FoundryControllerUiLayout.METAL_SCROLL_WIDTH - 1,
                thumbY + thumbHeight,
                maxOffset <= 0
                        ? 0xFF666666
                        : 0xFFA6A6A6
        );
    }

    private boolean handleScrollClick(
            double mouseX,
            double mouseY,
            int metalCount
    ) {
        if (
                metalCount
                        <= FoundryControllerUiLayout.VISIBLE_METAL_ROWS
        ) {
            return false;
        }

        int left =
                screen.guiLeft();

        int top =
                screen.guiTop();

        int totalHeight =
                FoundryControllerUiLayout.VISIBLE_METAL_ROWS
                        * FoundryControllerUiLayout.METAL_ROW_HEIGHT;

        if (
                !FoundryControllerUiLayout.contains(
                        mouseX,
                        mouseY,
                        left,
                        top,
                        FoundryControllerUiLayout.METAL_SCROLL_X,
                        FoundryControllerUiLayout.METAL_SCROLL_UP_Y,
                        FoundryControllerUiLayout.METAL_SCROLL_WIDTH,
                        totalHeight
                )
        ) {
            return false;
        }

        int maxOffset =
                metalCount
                        - FoundryControllerUiLayout.VISIBLE_METAL_ROWS;

        double relativeY =
                mouseY
                        - (
                        top
                                + FoundryControllerUiLayout.METAL_SCROLL_UP_Y
                );

        double ratio =
                totalHeight <= 0
                        ? 0.0
                        : relativeY / totalHeight;

        int targetOffset =
                Math.max(
                        0,
                        Math.min(
                                maxOffset,
                                (int) Math.round(
                                        ratio * maxOffset
                                )
                        )
                );

        if (targetOffset == metalScrollOffset) {
            if (relativeY < totalHeight / 2.0) {
                targetOffset = Math.max(0, metalScrollOffset - 1);
            } else {
                targetOffset = Math.min(maxOffset, metalScrollOffset + 1);
            }
        }

        metalScrollOffset = targetOffset;
        return true;
    }


    private static void renderMetalRowFrame(
            GuiGraphics graphics,
            int left,
            int top,
            boolean focused
    ) {
        int right =
                left
                        + FoundryControllerUiLayout.METAL_LIST_WIDTH;

        int bottom =
                top
                        + FoundryControllerUiLayout.METAL_ROW_HEIGHT
                        - 2;

        graphics.fill(
                left,
                top,
                right,
                bottom,
                0xFF000000
        );

        graphics.fill(
                left + 1,
                top + 1,
                right - 1,
                bottom - 1,
                focused
                        ? 0xFFB5B5B5
                        : 0xFF272727
        );

        graphics.fill(
                left + 1,
                top + 1,
                right - 1,
                top + 2,
                focused
                        ? 0xFFE2E2E2
                        : 0xFF777777
        );

        graphics.fill(
                left + 1,
                bottom - 1,
                right - 1,
                bottom,
                0xFF111111
        );

        if (focused) {
            graphics.fill(
                    left + 1,
                    top + 1,
                    left + 3,
                    bottom - 1,
                    FoundryControllerUiDrawing.ORANGE
            );
        }
    }

    private static ItemStack getDisplayIcon(
            MoltenMetalDefinition definition
    ) {
        var candidateId =
                ResourceLocation.fromNamespaceAndPath(
                        definition.id()
                                .getNamespace(),
                        definition.id()
                                .getPath()
                                + "_ingot"
                );

        Item item =
                BuiltInRegistries.ITEM.getOptional(
                                candidateId
                        )
                        .orElse(
                                Items.AIR
                        );

        if (item != Items.AIR) {
            return new ItemStack(
                    item
            );
        }

        return definition.createCastResult();
    }

    private void normalizeFocusedMetal() {
        List<MoltenMetalDefinition> stored =
                getStoredMetals();

        if (stored.isEmpty()) {
            focusedMetal =
                    null;

            metalScrollOffset =
                    0;

            return;
        }

        boolean currentStillExists =
                focusedMetal != null
                        && stored.stream()
                        .anyMatch(
                                definition ->
                                        definition.id()
                                                .equals(
                                                        focusedMetal
                                                )
                        );

        if (currentStillExists) {
            return;
        }

        focusedMetal =
                screen.controllerMenu()
                        .getSelectedMetalDefinition()
                        .filter(
                                selected ->
                                        stored.stream()
                                                .anyMatch(
                                                        definition ->
                                                                definition.id()
                                                                        .equals(
                                                                                selected.id()
                                                                        )
                                                )
                        )
                        .orElse(
                                stored.get(0)
                        )
                        .id();
    }

    private void clampScroll(
            int metalCount
    ) {
        metalScrollOffset =
                Math.max(
                        0,
                        Math.min(
                                metalScrollOffset,
                                Math.max(
                                        0,
                                        metalCount
                                                - FoundryControllerUiLayout.VISIBLE_METAL_ROWS
                                )
                        )
                );
    }

    private List<MoltenMetalDefinition> getStoredMetals() {
        List<MoltenMetalDefinition> result =
                new ArrayList<>();

        for (
                MoltenMetalDefinition definition :
                ModMoltenMetals.lightestFirst()
        ) {
            if (
                    screen.controllerMenu()
                            .getMetalAmount(
                                    definition
                            ) > 0
            ) {
                result.add(
                        definition
                );
            }
        }

        return result;
    }

}
