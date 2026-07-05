package net.chriskatze.katzencraftmetals.screen;

import net.chriskatze.katzencraftmetals.menu.FoundryControllerMenu;
import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.chriskatze.katzencraftmetals.metal.MoltenMetalDefinition;
import net.chriskatze.katzencraftmetals.recipe.FoundryAlloyIngredient;
import net.chriskatze.katzencraftmetals.recipe.FoundryAlloyRecipe;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Renders alloy recipes separately from the stored-metal output list. */
final class FoundryControllerAlloysRenderer {

    private final FoundryControllerScreen screen;

    private int scrollOffset;

    @Nullable
    private ResourceLocation focusedRecipeId;

    FoundryControllerAlloysRenderer(
            FoundryControllerScreen screen
    ) {
        this.screen = screen;
    }

    void render(
            GuiGraphics graphics
    ) {
        List<Entry> entries = buildEntries();

        normalizeSelection(entries);
        clampScroll(entries.size());

        for (
                int visibleRow = 0;
                visibleRow < FoundryControllerUiLayout.VISIBLE_ALLOY_ROWS;
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

        configureControls(
                findFocusedEntry(entries),
                false
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

        List<Entry> entries = buildEntries();

        for (
                int visibleRow = 0;
                visibleRow < FoundryControllerUiLayout.VISIBLE_ALLOY_ROWS;
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
                            FoundryControllerUiLayout.ALLOY_LIST_X,
                            FoundryControllerUiLayout.ALLOY_LIST_Y
                                    + visibleRow
                                    * FoundryControllerUiLayout.ALLOY_ROW_HEIGHT,
                            FoundryControllerUiLayout.ALLOY_LIST_WIDTH,
                            FoundryControllerUiLayout.ALLOY_ROW_HEIGHT
                    )
            ) {
                continue;
            }

            Entry entry = entries.get(index);
            focusedRecipeId = entry.holder().id();
            configureControls(entry, true);
            return true;
        }

        Entry focused = findFocusedEntry(entries);

        if (focused == null) {
            return false;
        }

        int maximum =
                menu().getMaxCraftableBatches(
                        focused.holder().value()
                );

        boolean enabled =
                maximum > 0
                        && !menu().isAlloyJobActive();

        if (
                FoundryControllerUiLayout.contains(
                        mouseX,
                        mouseY,
                        screen.guiLeft(),
                        screen.guiTop(),
                        FoundryControllerUiLayout.QUANTITY_MINUS_X,
                        FoundryControllerUiLayout.QUANTITY_Y,
                        FoundryControllerUiLayout.QUANTITY_MINUS_WIDTH,
                        11
                )
        ) {
            if (enabled) {
                screen.changeAlloyQuantity(-1);
            }

            return true;
        }

        if (
                FoundryControllerUiLayout.contains(
                        mouseX,
                        mouseY,
                        screen.guiLeft(),
                        screen.guiTop(),
                        FoundryControllerUiLayout.QUANTITY_PLUS_X,
                        FoundryControllerUiLayout.QUANTITY_Y,
                        FoundryControllerUiLayout.QUANTITY_PLUS_WIDTH,
                        11
                )
        ) {
            if (enabled) {
                screen.changeAlloyQuantity(1);
            }

            return true;
        }

        if (
                FoundryControllerUiLayout.contains(
                        mouseX,
                        mouseY,
                        screen.guiLeft(),
                        screen.guiTop(),
                        FoundryControllerUiLayout.START_X,
                        FoundryControllerUiLayout.START_Y,
                        FoundryControllerUiLayout.START_WIDTH,
                        FoundryControllerUiLayout.START_HEIGHT
                )
        ) {
            if (!enabled) {
                return true;
            }

            int quantity =
                    Math.min(
                            maximum,
                            screen.getAlloyQuantity()
                    );

            screen.sendMenuButton(
                    FoundryControllerMenu.createStartAlloyButton(
                            focused.recipeIndex(),
                            quantity
                    )
            );

            return true;
        }

        return false;
    }

    boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double scrollDelta
    ) {
        List<Entry> entries = buildEntries();

        int maxOffset =
                Math.max(
                        0,
                        entries.size()
                                - FoundryControllerUiLayout.VISIBLE_ALLOY_ROWS
                );

        if (
                maxOffset <= 0
                        || !FoundryControllerUiLayout.contains(
                        mouseX,
                        mouseY,
                        screen.guiLeft(),
                        screen.guiTop(),
                        FoundryControllerUiLayout.ALLOY_LIST_X,
                        FoundryControllerUiLayout.ALLOY_LIST_Y,
                        FoundryControllerUiLayout.ALLOY_LIST_WIDTH
                                + FoundryControllerUiLayout.ALLOY_SCROLL_WIDTH
                                + 4,
                        FoundryControllerUiLayout.ALLOY_SCROLL_HEIGHT
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
        List<Entry> entries = buildEntries();

        for (
                int visibleRow = 0;
                visibleRow < FoundryControllerUiLayout.VISIBLE_ALLOY_ROWS;
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
                            FoundryControllerUiLayout.ALLOY_LIST_X,
                            FoundryControllerUiLayout.ALLOY_LIST_Y
                                    + visibleRow
                                    * FoundryControllerUiLayout.ALLOY_ROW_HEIGHT,
                            FoundryControllerUiLayout.ALLOY_LIST_WIDTH,
                            FoundryControllerUiLayout.ALLOY_ROW_HEIGHT
                    )
            ) {
                Entry entry = entries.get(index);
                int maximum =
                        menu().getMaxCraftableBatches(
                                entry.holder().value()
                        );

                String suffix =
                        maximum > 0
                                ? " (up to " + maximum + ")"
                                : " (missing materials or space)";

                graphics.renderTooltip(
                        screen.uiFont(),
                        Component.literal(
                                recipeSummary(
                                        entry.holder().value()
                                )
                                        + suffix
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
            Entry entry,
            int visibleRow
    ) {
        int left =
                screen.guiLeft()
                        + FoundryControllerUiLayout.ALLOY_LIST_X;

        int top =
                screen.guiTop()
                        + FoundryControllerUiLayout.ALLOY_LIST_Y
                        + visibleRow
                        * FoundryControllerUiLayout.ALLOY_ROW_HEIGHT;

        boolean selected =
                focusedRecipeId != null
                        && focusedRecipeId.equals(
                        entry.holder().id()
                );

        int maximum =
                menu().getMaxCraftableBatches(
                        entry.holder().value()
                );

        boolean enabled = maximum > 0;

        FoundryControllerMetalsRenderer.drawRowFrame(
                graphics,
                left,
                top,
                FoundryControllerUiLayout.ALLOY_LIST_WIDTH,
                FoundryControllerUiLayout.ALLOY_ROW_HEIGHT,
                selected,
                enabled
        );

        ItemStack icon =
                FoundryControllerMetalsRenderer.getDisplayIcon(
                        entry.outputDefinition()
                );

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
                        : enabled
                        ? FoundryControllerUiDrawing.TEXT
                        : FoundryControllerUiDrawing.MUTED_TEXT;

        int secondaryColor =
                selected
                        ? 0xFF4A4A4A
                        : enabled
                        ? FoundryControllerUiDrawing.MUTED_TEXT
                        : 0xFF666666;

        graphics.drawString(
                screen.uiFont(),
                FoundryControllerMetalsRenderer.displayName(
                        entry.outputDefinition().id()
                ),
                left + 24,
                top + 2,
                textColor,
                false
        );

        graphics.drawString(
                screen.uiFont(),
                enabled
                        ? "max " + maximum
                        : "unavailable",
                left + 24,
                top + 11,
                secondaryColor,
                false
        );
    }

    private void configureControls(
            @Nullable Entry entry,
            boolean reset
    ) {
        if (entry == null) {
            screen.configureAlloyQuantity(
                    false,
                    1,
                    reset
            );
            return;
        }

        int maximum =
                menu().getMaxCraftableBatches(
                        entry.holder().value()
                );

        screen.configureAlloyQuantity(
                maximum > 0
                        && !menu().isAlloyJobActive(),
                Math.max(1, maximum),
                reset
        );
    }

    private List<Entry> buildEntries() {
        List<Entry> entries = new ArrayList<>();
        List<RecipeHolder<FoundryAlloyRecipe>> recipes = menu().getAlloyRecipes();

        for (int index = 0; index < recipes.size(); index++) {
            RecipeHolder<FoundryAlloyRecipe> holder = recipes.get(index);
            FoundryAlloyRecipe recipe = holder.value();

            if (recipe.requiredTier() > menu().getFoundryTier()) {
                continue;
            }

            Optional<MoltenMetalDefinition> outputDefinition =
                    ModMoltenMetals.get(
                            recipe.outputMetal()
                    );

            if (outputDefinition.isEmpty()) {
                continue;
            }

            entries.add(
                    new Entry(
                            index,
                            holder,
                            outputDefinition.get()
                    )
            );
        }

        return entries;
    }

    private void normalizeSelection(
            List<Entry> entries
    ) {
        if (entries.isEmpty()) {
            focusedRecipeId = null;
            scrollOffset = 0;
            screen.configureAlloyQuantity(
                    false,
                    1,
                    false
            );
            return;
        }

        if (findFocusedEntry(entries) != null) {
            return;
        }

        ResourceLocation activeOutput =
                menu().getActiveAlloyOutput()
                        .map(MoltenMetalDefinition::id)
                        .orElse(null);

        if (activeOutput != null) {
            for (Entry entry : entries) {
                if (
                        entry.holder()
                                .value()
                                .outputMetal()
                                .equals(activeOutput)
                ) {
                    focusedRecipeId = entry.holder().id();
                    configureControls(entry, true);
                    return;
                }
            }
        }

        focusedRecipeId = entries.get(0).holder().id();
        configureControls(entries.get(0), true);
    }

    @Nullable
    private Entry findFocusedEntry(
            List<Entry> entries
    ) {
        if (focusedRecipeId == null) {
            return null;
        }

        for (Entry entry : entries) {
            if (entry.holder().id().equals(focusedRecipeId)) {
                return entry;
            }
        }

        return null;
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
                                                - FoundryControllerUiLayout.VISIBLE_ALLOY_ROWS
                                )
                        )
                );
    }

    private void renderScrollbar(
            GuiGraphics graphics,
            int entryCount
    ) {
        FoundryControllerMetalsRenderer.drawScrollbar(
                graphics,
                screen.guiLeft()
                        + FoundryControllerUiLayout.ALLOY_SCROLL_X,
                screen.guiTop()
                        + FoundryControllerUiLayout.ALLOY_SCROLL_Y,
                FoundryControllerUiLayout.ALLOY_SCROLL_WIDTH,
                FoundryControllerUiLayout.ALLOY_SCROLL_HEIGHT,
                entryCount,
                FoundryControllerUiLayout.VISIBLE_ALLOY_ROWS,
                scrollOffset
        );
    }

    private static String recipeSummary(
            FoundryAlloyRecipe recipe
    ) {
        StringBuilder result = new StringBuilder();

        for (int index = 0; index < recipe.ingredients().size(); index++) {
            FoundryAlloyIngredient ingredient = recipe.ingredients().get(index);

            if (index > 0) {
                result.append(" + ");
            }

            result.append(
                    FoundryControllerMetalsRenderer.displayName(
                            ingredient.metal()
                    )
            );
        }

        result.append(" -> ");
        result.append(
                FoundryControllerMetalsRenderer.displayName(
                        recipe.outputMetal()
                )
        );

        return result.toString();
    }

    private FoundryControllerMenu menu() {
        return screen.controllerMenu();
    }

    private record Entry(
            int recipeIndex,
            RecipeHolder<FoundryAlloyRecipe> holder,
            MoltenMetalDefinition outputDefinition
    ) {
    }
}
