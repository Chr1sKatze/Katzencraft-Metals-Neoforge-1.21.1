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

/** Renders stored molten metals only. Alloy recipes are handled separately. */
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
                break;
            }

            renderEntry(
                    graphics,
                    entries.get(index),
                    visibleRow
            );
        }

        renderScrollbar(
                graphics,
                entries.size()
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

            if (
                    !FoundryControllerUiLayout.contains(
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
                        FoundryControllerUiLayout.METAL_LIST_WIDTH
                                + FoundryControllerUiLayout.METAL_SCROLL_WIDTH
                                + 4,
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
                MoltenMetalDefinition definition = entries.get(index);
                int amount = menu().getMetalAmount(definition);

                graphics.renderTooltip(
                        screen.uiFont(),
                        Component.literal(
                                "Select "
                                        + displayName(definition.id())
                                        + " for output ("
                                        + FoundryControllerUiDrawing.formatOreAmount(
                                        amount,
                                        definition.unitsPerOre()
                                )
                                        + " ore)"
                        ),
                        mouseX,
                        mouseY
                );

                return;
            }
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
                        * FoundryControllerUiLayout.METAL_ROW_HEIGHT;

        boolean selected =
                menu().getSelectedMetalDefinition()
                        .map(
                                selectedDefinition ->
                                        selectedDefinition.id()
                                                .equals(definition.id())
                        )
                        .orElse(false);

        drawRowFrame(
                graphics,
                left,
                top,
                selected
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
                selected
                        ? FoundryControllerUiDrawing.DARK_TEXT
                        : FoundryControllerUiDrawing.TEXT;

        int secondaryColor =
                selected
                        ? 0xFF4A4A4A
                        : FoundryControllerUiDrawing.MUTED_TEXT;

        graphics.drawString(
                screen.uiFont(),
                displayName(definition.id()),
                left + 24,
                top + 2,
                textColor,
                false
        );

        graphics.drawString(
                screen.uiFont(),
                FoundryControllerUiDrawing.formatOreAmount(
                        menu().getMetalAmount(definition),
                        definition.unitsPerOre()
                )
                        + " ore",
                left + 24,
                top + 11,
                secondaryColor,
                false
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

    private void renderScrollbar(
            GuiGraphics graphics,
            int entryCount
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

        drawScrollbar(
                graphics,
                left,
                top,
                width,
                height,
                entryCount,
                FoundryControllerUiLayout.VISIBLE_METAL_ROWS,
                scrollOffset
        );
    }

    static void drawScrollbar(
            GuiGraphics graphics,
            int left,
            int top,
            int width,
            int height,
            int entryCount,
            int visibleRows,
            int offset
    ) {
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
                        entryCount
                                - visibleRows
                );

        int thumbHeight =
                maxOffset <= 0
                        ? 14
                        : Math.max(
                        10,
                        height
                                * visibleRows
                                / Math.max(1, entryCount)
                );

        int travel =
                Math.max(
                        0,
                        height
                                - thumbHeight
                                - 2
                );

        int thumbY =
                top + 1
                        + (
                        maxOffset <= 0
                                ? travel / 2
                                : travel
                                * offset
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

    static void drawRowFrame(
            GuiGraphics graphics,
            int left,
            int top,
            int width,
            int height,
            boolean selected,
            boolean enabled
    ) {
        int right = left + width;
        int bottom = top + height - 2;

        graphics.fill(
                left,
                top,
                right,
                bottom,
                0xFF000000
        );

        int background =
                selected
                        ? 0xFFB5B5B5
                        : enabled
                        ? 0xFF232323
                        : 0xFF191919;

        graphics.fill(
                left + 1,
                top + 1,
                right - 1,
                bottom - 1,
                background
        );

        graphics.fill(
                left + 1,
                top + 1,
                right - 1,
                top + 2,
                selected
                        ? 0xFFE2E2E2
                        : enabled
                        ? 0xFF777777
                        : 0xFF454545
        );

        if (selected) {
            graphics.fill(
                    left + 1,
                    top + 1,
                    left + 3,
                    bottom - 1,
                    FoundryControllerUiDrawing.ORANGE
            );
        }
    }

    private static void drawRowFrame(
            GuiGraphics graphics,
            int left,
            int top,
            boolean selected
    ) {
        drawRowFrame(
                graphics,
                left,
                top,
                FoundryControllerUiLayout.METAL_LIST_WIDTH,
                FoundryControllerUiLayout.METAL_ROW_HEIGHT,
                selected,
                true
        );
    }

    static ItemStack getDisplayIcon(
            MoltenMetalDefinition definition
    ) {
        ResourceLocation candidateId =
                ResourceLocation.fromNamespaceAndPath(
                        definition.id()
                                .getNamespace(),
                        definition.id()
                                .getPath()
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
}
