package net.chriskatze.katzencraftmetals.screen;

import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.chriskatze.katzencraftmetals.metal.MoltenMetalDefinition;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

final class FoundryControllerMetalsRenderer {

    private final FoundryControllerScreen screen;

    private int scrollOffset;

    @Nullable
    private ResourceLocation focusedMetal;

    FoundryControllerMetalsRenderer(
            FoundryControllerScreen screen
    ) {
        this.screen = screen;
    }

    void render(
            GuiGraphics graphics
    ) {
        normalizeFocusedMetal();

        List<MoltenMetalDefinition> stored =
                getStoredMetals();

        clampScroll(stored.size());

        for (
                int visibleRow = 0;
                visibleRow
                        < FoundryControllerUiLayout.VISIBLE_METAL_ROWS;
                visibleRow++
        ) {
            int index = scrollOffset + visibleRow;

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

        renderRecipeFoundation(
                graphics,
                stored
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

        int left = screen.guiLeft();
        int top = screen.guiTop();

        for (
                int visibleRow = 0;
                visibleRow
                        < FoundryControllerUiLayout.VISIBLE_METAL_ROWS;
                visibleRow++
        ) {
            int index = scrollOffset + visibleRow;

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

            focusedMetal = definition.id();

            int syncId =
                    ModMoltenMetals.getSyncId(
                            definition.id()
                    );

            if (syncId >= 0) {
                screen.sendMenuButton(syncId);
            }

            return true;
        }

        return false;
    }

    boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double scrollDelta
    ) {
        List<MoltenMetalDefinition> stored =
                getStoredMetals();

        int maxOffset =
                Math.max(
                        0,
                        stored.size()
                                - FoundryControllerUiLayout.VISIBLE_METAL_ROWS
                );

        if (maxOffset <= 0) {
            return false;
        }

        if (
                !FoundryControllerUiLayout.contains(
                        mouseX,
                        mouseY,
                        screen.guiLeft(),
                        screen.guiTop(),
                        FoundryControllerUiLayout.METAL_LIST_X,
                        FoundryControllerUiLayout.METAL_LIST_Y,
                        FoundryControllerUiLayout.METAL_LIST_WIDTH
                                + FoundryControllerUiLayout.METAL_SCROLL_WIDTH
                                + 4,
                        FoundryControllerUiLayout.METAL_SCROLL_HEIGHT
                )
        ) {
            return false;
        }

        if (scrollDelta > 0.0) {
            scrollOffset = Math.max(0, scrollOffset - 1);
        } else if (scrollDelta < 0.0) {
            scrollOffset = Math.min(maxOffset, scrollOffset + 1);
        }

        return true;
    }

    void renderTooltips(
            GuiGraphics graphics,
            int mouseX,
            int mouseY
    ) {
        List<MoltenMetalDefinition> stored =
                getStoredMetals();

        for (
                int visibleRow = 0;
                visibleRow
                        < FoundryControllerUiLayout.VISIBLE_METAL_ROWS;
                visibleRow++
        ) {
            int index = scrollOffset + visibleRow;

            if (index >= stored.size()) {
                break;
            }

            if (
                    FoundryControllerUiLayout.contains(
                            mouseX,
                            mouseY,
                            screen.guiLeft(),
                            screen.guiTop(),
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
                                "Select this metal. Per-Faucet output settings will later move to Shift + Right Click on each Faucet."
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
                        .equals(focusedMetal);

        drawRowFrame(
                graphics,
                left,
                top,
                focused
        );

        ItemStack icon = getDisplayIcon(definition);

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

        int secondaryColor =
                focused
                        ? 0xFF4A4A4A
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
                        .getMetalAmount(definition);

        graphics.drawString(
                screen.uiFont(),
                FoundryControllerUiDrawing.formatOreAmount(
                        amount,
                        definition.unitsPerOre()
                )
                        + " ore",
                left + 24,
                top + 11,
                secondaryColor,
                false
        );
    }

    private void renderScrollbar(
            GuiGraphics graphics,
            int metalCount
    ) {
        int left =
                screen.guiLeft()
                        + FoundryControllerUiLayout.METAL_SCROLL_X;

        int top =
                screen.guiTop()
                        + FoundryControllerUiLayout.METAL_SCROLL_Y;

        int width =
                FoundryControllerUiLayout.METAL_SCROLL_WIDTH;

        int height =
                FoundryControllerUiLayout.METAL_SCROLL_HEIGHT;

        graphics.fill(
                left,
                top,
                left + width,
                top + height,
                0xFF151515
        );

        graphics.fill(
                left + 1,
                top + 1,
                left + width - 1,
                top + height - 1,
                0xFF2B2B2B
        );

        int maxOffset =
                Math.max(
                        0,
                        metalCount
                                - FoundryControllerUiLayout.VISIBLE_METAL_ROWS
                );

        int thumbHeight =
                maxOffset <= 0
                        ? 14
                        : Math.max(
                        10,
                        height
                                * FoundryControllerUiLayout.VISIBLE_METAL_ROWS
                                / metalCount
                );

        int travel =
                Math.max(
                        0,
                        height - thumbHeight - 2
                );

        int thumbY =
                top + 1
                        + (
                        maxOffset <= 0
                                ? travel / 2
                                : travel
                                * scrollOffset
                                / maxOffset
                );

        graphics.fill(
                left + 1,
                thumbY,
                left + width - 1,
                thumbY + thumbHeight,
                maxOffset <= 0
                        ? 0xFF626262
                        : 0xFFA5A5A5
        );
    }

    private void renderRecipeFoundation(
            GuiGraphics graphics,
            List<MoltenMetalDefinition> stored
    ) {
        MoltenMetalDefinition focused =
                findFocusedDefinition(stored);

        if (focused != null) {
            ItemStack result = getDisplayIcon(focused);

            if (!result.isEmpty()) {
                graphics.renderItem(
                        result,
                        screen.guiLeft()
                                + FoundryControllerUiLayout.RESULT_X,
                        screen.guiTop()
                                + FoundryControllerUiLayout.RECIPE_SLOT_Y
                );
            }
        }

        FoundryControllerUiDrawing.drawCentered(
                graphics,
                screen.uiFont(),
                Component.literal("-"),
                screen.guiLeft() + 255,
                screen.guiTop()
                        + FoundryControllerUiLayout.REQUIRED_TEMPERATURE_Y,
                24,
                10,
                FoundryControllerUiDrawing.MUTED_TEXT
        );

        FoundryControllerUiDrawing.drawCentered(
                graphics,
                screen.uiFont(),
                Component.literal("1"),
                screen.guiLeft()
                        + FoundryControllerUiLayout.QUANTITY_X,
                screen.guiTop()
                        + FoundryControllerUiLayout.QUANTITY_Y,
                19,
                10,
                FoundryControllerUiDrawing.TEXT
        );
    }

    private static void drawRowFrame(
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
                        : 0xFF232323
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
        ResourceLocation candidateId =
                ResourceLocation.fromNamespaceAndPath(
                        definition.id().getNamespace(),
                        definition.id().getPath()
                                + "_ingot"
                );

        Item item =
                BuiltInRegistries.ITEM
                        .getOptional(candidateId)
                        .orElse(Items.AIR);

        if (item != Items.AIR) {
            return new ItemStack(item);
        }

        return definition.createCastResult();
    }

    private void normalizeFocusedMetal() {
        List<MoltenMetalDefinition> stored =
                getStoredMetals();

        if (stored.isEmpty()) {
            focusedMetal = null;
            scrollOffset = 0;
            return;
        }

        boolean currentExists =
                focusedMetal != null
                        && stored.stream()
                        .anyMatch(
                                definition ->
                                        definition.id()
                                                .equals(focusedMetal)
                        );

        if (currentExists) {
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
                        .orElse(stored.get(0))
                        .id();
    }

    private void clampScroll(
            int metalCount
    ) {
        scrollOffset =
                Math.max(
                        0,
                        Math.min(
                                scrollOffset,
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
                            .getMetalAmount(definition) > 0
            ) {
                result.add(definition);
            }
        }

        return result;
    }

    @Nullable
    private MoltenMetalDefinition findFocusedDefinition(
            List<MoltenMetalDefinition> stored
    ) {
        if (focusedMetal == null) {
            return null;
        }

        for (MoltenMetalDefinition definition : stored) {
            if (definition.id().equals(focusedMetal)) {
                return definition;
            }
        }

        return null;
    }
}
