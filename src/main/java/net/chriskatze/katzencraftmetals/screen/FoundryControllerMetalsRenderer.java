package net.chriskatze.katzencraftmetals.screen;

import net.chriskatze.katzencraftmetals.menu.FoundryControllerMenu;
import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.chriskatze.katzencraftmetals.metal.MoltenMetalDefinition;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Renders the stored molten-metal selection list. */
final class FoundryControllerMetalsRenderer {

    private final FoundryControllerScreen screen;
    private int scrollOffset;

    FoundryControllerMetalsRenderer(
            FoundryControllerScreen screen
    ) {
        this.screen = screen;
    }

    void render(
            GuiGraphics graphics
    ) {
        List<MoltenMetalDefinition> entries = buildEntries();
        clampScroll(entries.size());

        for (
                int visibleRow = 0;
                visibleRow < FoundryControllerUiLayout.VISIBLE_METAL_ROWS;
                visibleRow++
        ) {
            int index = scrollOffset + visibleRow;

            if (index >= entries.size()) {
                renderBlankRow(graphics, visibleRow);
                continue;
            }

            renderEntry(
                    graphics,
                    entries.get(index),
                    visibleRow
            );
        }

        FoundryControllerListDrawing.drawScrollbar(
                graphics,
                screen.guiLeft()
                        + FoundryControllerUiLayout.METAL_SCROLL_X,
                screen.guiTop()
                        + FoundryControllerUiLayout.METAL_SCROLL_Y,
                FoundryControllerUiLayout.METAL_SCROLL_HEIGHT,
                entries.size(),
                FoundryControllerUiLayout.VISIBLE_METAL_ROWS,
                scrollOffset
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

        List<MoltenMetalDefinition> entries = buildEntries();

        for (
                int visibleRow = 0;
                visibleRow < FoundryControllerUiLayout.VISIBLE_METAL_ROWS;
                visibleRow++
        ) {
            int index = scrollOffset + visibleRow;

            if (index >= entries.size()) {
                break;
            }

            if (!isMouseOverRow(mouseX, mouseY, visibleRow)) {
                continue;
            }

            int buttonId =
                    FoundryControllerMenu.createSelectMetalButton(
                            entries.get(index)
                    );

            return buttonId >= 0
                    && screen.sendMenuButton(buttonId);
        }

        return false;
    }

    boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double scrollDelta
    ) {
        List<MoltenMetalDefinition> entries = buildEntries();

        int maxOffset =
                Math.max(
                        0,
                        entries.size()
                                - FoundryControllerUiLayout.VISIBLE_METAL_ROWS
                );

        if (
                maxOffset <= 0
                        || !FoundryControllerUiLayout.contains(
                        mouseX,
                        mouseY,
                        screen.guiLeft(),
                        screen.guiTop(),
                        FoundryControllerUiLayout.METAL_LIST_X,
                        FoundryControllerUiLayout.METAL_LIST_Y,
                        FoundryControllerUiLayout.METAL_SCROLL_X
                                + FoundryControllerUiLayout.METAL_SCROLL_WIDTH
                                - FoundryControllerUiLayout.METAL_LIST_X,
                        FoundryControllerUiLayout.METAL_SCROLL_HEIGHT
                )
        ) {
            return false;
        }

        if (scrollDelta > 0.0) {
            scrollOffset =
                    Math.max(
                            0,
                            scrollOffset - 1
                    );
        } else if (scrollDelta < 0.0) {
            scrollOffset =
                    Math.min(
                            maxOffset,
                            scrollOffset + 1
                    );
        }

        return true;
    }

    void renderTooltips(
            GuiGraphics graphics,
            int mouseX,
            int mouseY
    ) {
        List<MoltenMetalDefinition> entries = buildEntries();

        for (
                int visibleRow = 0;
                visibleRow < FoundryControllerUiLayout.VISIBLE_METAL_ROWS;
                visibleRow++
        ) {
            int index = scrollOffset + visibleRow;

            if (index >= entries.size()) {
                break;
            }

            if (!isMouseOverRow(mouseX, mouseY, visibleRow)) {
                continue;
            }

            MoltenMetalDefinition definition = entries.get(index);
            int amount = menu().getMetalAmount(definition);

            graphics.renderTooltip(
                    screen.uiFont(),
                    Component.literal(
                            displayName(definition.id())
                                    + ": "
                                    + FoundryControllerUiDrawing.formatOreAmount(
                                    amount,
                                    definition.unitsPerOre()
                            )
                                    + " ore"
                    ),
                    mouseX,
                    mouseY
            );

            return;
        }
    }

    private void renderEntry(
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
                        * FoundryControllerUiLayout.METAL_ROW_STRIDE;

        boolean selected =
                menu().getSelectedMetalDefinition()
                        .map(
                                selectedDefinition ->
                                        selectedDefinition.id()
                                                .equals(definition.id())
                        )
                        .orElse(false);

        FoundryControllerListDrawing.drawRow(
                graphics,
                left,
                top,
                selected,
                true
        );

        ItemStack icon = getDisplayIcon(definition);

        if (!icon.isEmpty()) {
            graphics.renderItem(
                    icon,
                    left + 5,
                    top + 2
            );
        }
    }


    private void renderBlankRow(
            GuiGraphics graphics,
            int visibleRow
    ) {
        FoundryControllerListDrawing.drawRow(
                graphics,
                screen.guiLeft()
                        + FoundryControllerUiLayout.METAL_LIST_X,
                screen.guiTop()
                        + FoundryControllerUiLayout.METAL_LIST_Y
                        + visibleRow
                        * FoundryControllerUiLayout.METAL_ROW_STRIDE,
                false,
                true
        );
    }

    private boolean isMouseOverRow(
            double mouseX,
            double mouseY,
            int visibleRow
    ) {
        return FoundryControllerUiLayout.contains(
                mouseX,
                mouseY,
                screen.guiLeft(),
                screen.guiTop(),
                FoundryControllerUiLayout.METAL_LIST_X,
                FoundryControllerUiLayout.METAL_LIST_Y
                        + visibleRow
                        * FoundryControllerUiLayout.METAL_ROW_STRIDE,
                FoundryControllerUiLayout.METAL_LIST_WIDTH,
                FoundryControllerUiLayout.METAL_ROW_HEIGHT
        );
    }

    private List<MoltenMetalDefinition> buildEntries() {
        List<MoltenMetalDefinition> entries = new ArrayList<>();

        for (
                MoltenMetalDefinition definition :
                ModMoltenMetals.lightestFirst()
        ) {
            if (menu().getMetalAmount(definition) > 0) {
                entries.add(definition);
            }
        }

        return entries;
    }

    private void clampScroll(
            int count
    ) {
        scrollOffset =
                Math.max(
                        0,
                        Math.min(
                                scrollOffset,
                                Math.max(
                                        0,
                                        count
                                                - FoundryControllerUiLayout.VISIBLE_METAL_ROWS
                                )
                        )
                );
    }

    static ItemStack getDisplayIcon(
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

    static String displayName(
            ResourceLocation id
    ) {
        String path =
                id.getPath()
                        .replace('_', ' ');

        if (path.isEmpty()) {
            return "";
        }

        return Character.toUpperCase(path.charAt(0))
                + path.substring(1);
    }

    private FoundryControllerMenu menu() {
        return screen.controllerMenu();
    }
}
