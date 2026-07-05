package net.chriskatze.katzencraftmetals.screen;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
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

/** Renders alloy recipes, their preview slots, quantity controls and start button. */
final class FoundryControllerAlloysRenderer {

    private static final ResourceLocation UNLOCKED_SLOT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    KatzencraftMetalsMod.MODID,
                    "textures/gui/foundry_controller_unlocked_slot.png"
            );

    private static final ResourceLocation LOCKED_SLOT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    KatzencraftMetalsMod.MODID,
                    "textures/gui/foundry_controller_slot_locked.png"
            );

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
                        + FoundryControllerUiLayout.ALLOY_SCROLL_X,
                screen.guiTop()
                        + FoundryControllerUiLayout.ALLOY_SCROLL_Y,
                FoundryControllerUiLayout.ALLOY_SCROLL_HEIGHT,
                entries.size(),
                FoundryControllerUiLayout.VISIBLE_ALLOY_ROWS,
                scrollOffset
        );

        Entry focused = findFocusedEntry(entries);
        renderRecipePreview(graphics, focused);
        configureControls(focused, false);
        renderStartLabel(graphics, focused);
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

            if (!isMouseOverRow(mouseX, mouseY, visibleRow)) {
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
                        FoundryControllerUiLayout.QUANTITY_BUTTON_Y,
                        FoundryControllerUiLayout.QUANTITY_MINUS_WIDTH,
                        FoundryControllerUiLayout.QUANTITY_BUTTON_HEIGHT
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
                        FoundryControllerUiLayout.QUANTITY_BUTTON_Y,
                        FoundryControllerUiLayout.QUANTITY_PLUS_WIDTH,
                        FoundryControllerUiLayout.QUANTITY_BUTTON_HEIGHT
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
                        FoundryControllerUiLayout.ALLOY_SCROLL_X
                                + FoundryControllerUiLayout.ALLOY_SCROLL_WIDTH
                                - FoundryControllerUiLayout.ALLOY_LIST_X,
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

            if (!isMouseOverRow(mouseX, mouseY, visibleRow)) {
                continue;
            }

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
                            recipeSummary(entry.holder().value())
                                    + suffix
                    ),
                    mouseX,
                    mouseY
            );

            return;
        }

        Entry focused = findFocusedEntry(entries);

        if (focused == null) {
            return;
        }

        FoundryAlloyRecipe recipe = focused.holder().value();

        for (
                int index = 0;
                index < recipe.ingredients().size()
                        && index
                        < FoundryControllerUiLayout.ALLOY_INGREDIENT_SLOT_X.length;
                index++
        ) {
            if (!isMouseOverIngredientSlot(mouseX, mouseY, index)) {
                continue;
            }

            FoundryAlloyIngredient ingredient =
                    recipe.ingredients().get(index);

            Optional<MoltenMetalDefinition> definition =
                    ModMoltenMetals.get(ingredient.metal());

            if (definition.isEmpty()) {
                return;
            }

            graphics.renderTooltip(
                    screen.uiFont(),
                    Component.literal(
                            FoundryControllerMetalsRenderer.displayName(
                                    ingredient.metal()
                            )
                                    + ": "
                                    + FoundryControllerUiDrawing.formatOreAmount(
                                    ingredient.amount(),
                                    definition.get().unitsPerOre()
                            )
                                    + " ore"
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
                        screen.guiLeft(),
                        screen.guiTop(),
                        FoundryControllerUiLayout.ALLOY_RESULT_SLOT_X,
                        FoundryControllerUiLayout.ALLOY_RECIPE_SLOT_Y,
                        16,
                        16
                )
        ) {
            MoltenMetalDefinition output = focused.outputDefinition();

            graphics.renderTooltip(
                    screen.uiFont(),
                    Component.literal(
                            FoundryControllerMetalsRenderer.displayName(
                                    recipe.outputMetal()
                            )
                                    + ": "
                                    + FoundryControllerUiDrawing.formatOreAmount(
                                    recipe.outputAmount(),
                                    output.unitsPerOre()
                            )
                                    + " ore"
                    ),
                    mouseX,
                    mouseY
            );
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
                        * FoundryControllerUiLayout.ALLOY_ROW_STRIDE;

        boolean selected =
                focusedRecipeId != null
                        && focusedRecipeId.equals(
                        entry.holder().id()
                );

        boolean enabled =
                menu().getMaxCraftableBatches(
                        entry.holder().value()
                ) > 0;

        FoundryControllerListDrawing.drawRow(
                graphics,
                left,
                top,
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
    }


    private void renderBlankRow(
            GuiGraphics graphics,
            int visibleRow
    ) {
        FoundryControllerListDrawing.drawRow(
                graphics,
                screen.guiLeft()
                        + FoundryControllerUiLayout.ALLOY_LIST_X,
                screen.guiTop()
                        + FoundryControllerUiLayout.ALLOY_LIST_Y
                        + visibleRow
                        * FoundryControllerUiLayout.ALLOY_ROW_STRIDE,
                false,
                true
        );
    }

    private void renderRecipePreview(
            GuiGraphics graphics,
            @Nullable Entry entry
    ) {
        FoundryAlloyRecipe recipe =
                entry == null
                        ? null
                        : entry.holder().value();

        for (
                int index = 0;
                index
                        < FoundryControllerUiLayout.ALLOY_INGREDIENT_SLOT_X.length;
                index++
        ) {
            boolean used =
                    recipe != null
                            && index < recipe.ingredients().size();

            int slotX =
                    FoundryControllerUiLayout.ALLOY_INGREDIENT_SLOT_X[index];

            drawPreviewSlot(
                    graphics,
                    slotX,
                    FoundryControllerUiLayout.ALLOY_RECIPE_SLOT_Y,
                    used
            );

            if (!used) {
                continue;
            }

            FoundryAlloyIngredient ingredient =
                    recipe.ingredients().get(index);

            ModMoltenMetals.get(ingredient.metal())
                    .map(
                            FoundryControllerMetalsRenderer::getDisplayIcon
                    )
                    .filter(stack -> !stack.isEmpty())
                    .ifPresent(
                            stack -> graphics.renderItem(
                                    stack,
                                    screen.guiLeft() + slotX,
                                    screen.guiTop()
                                            + FoundryControllerUiLayout.ALLOY_RECIPE_SLOT_Y
                            )
                    );
        }

        boolean hasResult = entry != null;

        drawPreviewSlot(
                graphics,
                FoundryControllerUiLayout.ALLOY_RESULT_SLOT_X,
                FoundryControllerUiLayout.ALLOY_RECIPE_SLOT_Y,
                hasResult
        );

        if (hasResult) {
            ItemStack output =
                    FoundryControllerMetalsRenderer.getDisplayIcon(
                            entry.outputDefinition()
                    );

            if (!output.isEmpty()) {
                graphics.renderItem(
                        output,
                        screen.guiLeft()
                                + FoundryControllerUiLayout.ALLOY_RESULT_SLOT_X,
                        screen.guiTop()
                                + FoundryControllerUiLayout.ALLOY_RECIPE_SLOT_Y
                );
            }
        }
    }

    private void drawPreviewSlot(
            GuiGraphics graphics,
            int slotX,
            int slotY,
            boolean unlocked
    ) {
        if (unlocked) {
            graphics.blit(
                    UNLOCKED_SLOT_TEXTURE,
                    screen.guiLeft() + slotX - 1,
                    screen.guiTop() + slotY - 1,
                    0.0f,
                    0.0f,
                    18,
                    18,
                    18,
                    18
            );
            return;
        }

        graphics.blit(
                LOCKED_SLOT_TEXTURE,
                screen.guiLeft() + slotX,
                screen.guiTop() + slotY,
                0.0f,
                0.0f,
                16,
                16,
                16,
                16
        );
    }

    private void renderStartLabel(
            GuiGraphics graphics,
            @Nullable Entry entry
    ) {
        boolean enabled =
                entry != null
                        && menu().getMaxCraftableBatches(
                        entry.holder().value()
                ) > 0
                        && !menu().isAlloyJobActive();

        FoundryControllerUiDrawing.drawCentered(
                graphics,
                screen.uiFont(),
                Component.literal("START"),
                screen.guiLeft()
                        + FoundryControllerUiLayout.START_X,
                screen.guiTop()
                        + FoundryControllerUiLayout.START_Y,
                FoundryControllerUiLayout.START_WIDTH,
                FoundryControllerUiLayout.START_HEIGHT,
                enabled
                        ? FoundryControllerUiDrawing.GREEN
                        : FoundryControllerUiDrawing.MUTED_TEXT
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
        List<RecipeHolder<FoundryAlloyRecipe>> recipes =
                menu().getAlloyRecipes();

        for (int index = 0; index < recipes.size(); index++) {
            RecipeHolder<FoundryAlloyRecipe> holder = recipes.get(index);
            FoundryAlloyRecipe recipe = holder.value();

            if (recipe.requiredTier() > menu().getFoundryTier()) {
                continue;
            }

            Optional<MoltenMetalDefinition> outputDefinition =
                    ModMoltenMetals.get(recipe.outputMetal());

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
                FoundryControllerUiLayout.ALLOY_LIST_X,
                FoundryControllerUiLayout.ALLOY_LIST_Y
                        + visibleRow
                        * FoundryControllerUiLayout.ALLOY_ROW_STRIDE,
                FoundryControllerUiLayout.ALLOY_LIST_WIDTH,
                FoundryControllerUiLayout.ALLOY_ROW_HEIGHT
        );
    }

    private boolean isMouseOverIngredientSlot(
            double mouseX,
            double mouseY,
            int index
    ) {
        return index >= 0
                && index
                < FoundryControllerUiLayout.ALLOY_INGREDIENT_SLOT_X.length
                && FoundryControllerUiLayout.contains(
                mouseX,
                mouseY,
                screen.guiLeft(),
                screen.guiTop(),
                FoundryControllerUiLayout.ALLOY_INGREDIENT_SLOT_X[index],
                FoundryControllerUiLayout.ALLOY_RECIPE_SLOT_Y,
                16,
                16
        );
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
