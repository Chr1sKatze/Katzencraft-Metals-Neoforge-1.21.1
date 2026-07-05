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
            texture("foundry_controller_unlocked_slot.png");

    private static final ResourceLocation LOCKED_SLOT_TEXTURE =
            texture("foundry_controller_slot_locked.png");

    private static final ResourceLocation MINUS_BUTTON_TEXTURE =
            texture("foundry_controller_minus_button.png");

    private static final ResourceLocation MINUS_BUTTON_HOVER_TEXTURE =
            texture("foundry_controller_minus_button_hover.png");

    private static final ResourceLocation MINUS_BUTTON_PRESSED_TEXTURE =
            texture("foundry_controller_minus_button_pressed.png");

    private static final ResourceLocation PLUS_BUTTON_TEXTURE =
            texture("foundry_controller_plus_button.png");

    private static final ResourceLocation PLUS_BUTTON_HOVER_TEXTURE =
            texture("foundry_controller_plus_button_hover.png");

    private static final ResourceLocation PLUS_BUTTON_PRESSED_TEXTURE =
            texture("foundry_controller_plus_button_pressed.png");

    private static final ResourceLocation EDITBAR_TEXTURE =
            texture("foundry_controller_alloy_editbar.png");

    private static final ResourceLocation START_BUTTON_TEXTURE =
            texture("foundry_controller_alloy_button.png");

    private static final ResourceLocation START_BUTTON_HOVER_TEXTURE =
            texture("foundry_controller_alloy_button_hover.png");

    private static final ResourceLocation START_BUTTON_PRESSED_TEXTURE =
            texture("foundry_controller_alloy_button_pressed.png");

    private static final long PRESSED_DISPLAY_TIME_MS = 120L;

    private final FoundryControllerScreen screen;
    private int scrollOffset;

    @Nullable
    private ResourceLocation focusedRecipeId;

    private ControlButton pressedControl = ControlButton.NONE;
    private long pressedUntil;

    FoundryControllerAlloysRenderer(
            FoundryControllerScreen screen
    ) {
        this.screen = screen;
    }

    void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY
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

        renderRecipePreview(
                graphics,
                focused
        );

        configureControls(
                focused,
                false
        );

        renderControls(
                graphics,
                focused,
                mouseX,
                mouseY
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

        boolean alloying =
                menu().isAlloyJobActive();

        /*
         * During an active job, the main button becomes STOP and remains
         * usable even though the quantity controls are locked.
         */
        if (
                alloying
                        && isMouseOverStart(
                        mouseX,
                        mouseY
                )
        ) {
            press(ControlButton.START);
            screen.playButtonClickSound();
            screen.sendMenuButton(
                    FoundryControllerMenu
                            .createStopAlloyButton()
            );
            return true;
        }

        List<Entry> entries = buildEntries();

        if (!alloying) {
            for (
                    int visibleRow = 0;
                    visibleRow
                            < FoundryControllerUiLayout.VISIBLE_ALLOY_ROWS;
                    visibleRow++
            ) {
                int index =
                        scrollOffset + visibleRow;

                if (index >= entries.size()) {
                    break;
                }

                if (
                        !isMouseOverRow(
                                mouseX,
                                mouseY,
                                visibleRow
                        )
                ) {
                    continue;
                }

                Entry entry =
                        entries.get(index);

                focusedRecipeId =
                        entry.holder().id();

                configureControls(
                        entry,
                        true
                );

                return true;
            }
        }

        Entry focused =
                findFocusedEntry(entries);

        if (focused == null) {
            return false;
        }

        int maximum =
                menu().getMaxCraftableBatches(
                        focused.holder().value()
                );

        boolean enabled =
                maximum > 0
                        && !alloying;

        if (isMouseOverMinus(mouseX, mouseY)) {
            if (enabled) {
                press(ControlButton.MINUS);
                screen.playButtonClickSound();
                screen.changeAlloyQuantity(-1);
            }

            return true;
        }

        if (isMouseOverPlus(mouseX, mouseY)) {
            if (enabled) {
                press(ControlButton.PLUS);
                screen.playButtonClickSound();
                screen.changeAlloyQuantity(1);
            }

            return true;
        }

        if (isMouseOverStart(mouseX, mouseY)) {
            if (!enabled) {
                return true;
            }

            press(ControlButton.START);
            screen.playButtonClickSound();

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

            graphics.renderTooltip(
                    screen.uiFont(),
                    Component.literal(
                            FoundryControllerMetalsRenderer.displayName(
                                    entry.holder()
                                            .value()
                                            .outputMetal()
                            )
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

        FoundryAlloyRecipe recipe =
                focused.holder().value();

        int batchCount =
                getPreviewBatchCount(focused);

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
                                    safeMultiply(
                                            ingredient.amount(),
                                            batchCount
                                    ),
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
            MoltenMetalDefinition output =
                    focused.outputDefinition();

            graphics.renderTooltip(
                    screen.uiFont(),
                    Component.literal(
                            FoundryControllerMetalsRenderer.displayName(
                                    recipe.outputMetal()
                            )
                                    + ": "
                                    + FoundryControllerUiDrawing.formatOreAmount(
                                    safeMultiply(
                                            recipe.outputAmount(),
                                            batchCount
                                    ),
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

        String alloyName =
                FoundryControllerMetalsRenderer.fitText(
                        screen.uiFont(),
                        FoundryControllerMetalsRenderer.displayName(
                                entry.holder()
                                        .value()
                                        .outputMetal()
                        ),
                        FoundryControllerMetalsRenderer.LIST_TEXT_WIDTH
                );

        int alloyNameY =
                top
                        + Math.max(
                        0,
                        (
                                FoundryControllerUiLayout.ALLOY_ROW_HEIGHT
                                        - FoundryControllerMetalsRenderer
                                        .scaledListLineHeight(
                                                screen.uiFont()
                                        )
                        ) / 2
                )
                        + 1;

        FoundryControllerMetalsRenderer.drawScaledListText(
                graphics,
                Component.literal(alloyName),
                left
                        + FoundryControllerMetalsRenderer
                        .LIST_TEXT_X_OFFSET,
                alloyNameY,
                enabled
                        ? FoundryControllerUiDrawing.TEXT
                        : FoundryControllerUiDrawing.MUTED_TEXT
        );
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

        int batchCount =
                getPreviewBatchCount(entry);

        for (
                int index = 0;
                index
                        < FoundryControllerUiLayout
                        .ALLOY_INGREDIENT_SLOT_X.length;
                index++
        ) {
            boolean used =
                    recipe != null
                            && index
                            < recipe.ingredients().size();

            int slotX =
                    FoundryControllerUiLayout
                            .ALLOY_INGREDIENT_SLOT_X[index];

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

            Optional<MoltenMetalDefinition> definition =
                    ModMoltenMetals.get(
                            ingredient.metal()
                    );

            if (definition.isEmpty()) {
                continue;
            }

            ItemStack stack =
                    FoundryControllerMetalsRenderer
                            .getDisplayIcon(
                                    definition.get()
                            );

            if (stack.isEmpty()) {
                continue;
            }

            int itemX =
                    screen.guiLeft() + slotX;

            int itemY =
                    screen.guiTop()
                            + FoundryControllerUiLayout
                            .ALLOY_RECIPE_SLOT_Y;

            graphics.renderItem(
                    stack,
                    itemX,
                    itemY
            );

            graphics.renderItemDecorations(
                    screen.uiFont(),
                    stack,
                    itemX,
                    itemY,
                    FoundryControllerUiDrawing
                            .formatOreAmount(
                                    safeMultiply(
                                            ingredient.amount(),
                                            batchCount
                                    ),
                                    definition.get()
                                            .unitsPerOre()
                            )
            );
        }

        boolean hasResult =
                entry != null;

        drawPreviewSlot(
                graphics,
                FoundryControllerUiLayout.ALLOY_RESULT_SLOT_X,
                FoundryControllerUiLayout.ALLOY_RECIPE_SLOT_Y,
                hasResult
        );

        if (!hasResult) {
            return;
        }

        ItemStack output =
                FoundryControllerMetalsRenderer
                        .getDisplayIcon(
                                entry.outputDefinition()
                        );

        if (output.isEmpty()) {
            return;
        }

        int resultX =
                screen.guiLeft()
                        + FoundryControllerUiLayout
                        .ALLOY_RESULT_SLOT_X;

        int resultY =
                screen.guiTop()
                        + FoundryControllerUiLayout
                        .ALLOY_RECIPE_SLOT_Y;

        graphics.renderItem(
                output,
                resultX,
                resultY
        );

        graphics.renderItemDecorations(
                screen.uiFont(),
                output,
                resultX,
                resultY,
                FoundryControllerUiDrawing
                        .formatOreAmount(
                                safeMultiply(
                                        entry.holder()
                                                .value()
                                                .outputAmount(),
                                        batchCount
                                ),
                                entry.outputDefinition()
                                        .unitsPerOre()
                        )
        );
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

    private void renderControls(
            GuiGraphics graphics,
            @Nullable Entry entry,
            int mouseX,
            int mouseY
    ) {
        clearExpiredPress();

        boolean alloying =
                menu().isAlloyJobActive();

        boolean startEnabled =
                entry != null
                        && menu().getMaxCraftableBatches(
                        entry.holder().value()
                ) > 0
                        && !alloying;

        boolean mainButtonEnabled =
                alloying || startEnabled;

        graphics.blit(
                EDITBAR_TEXTURE,
                screen.guiLeft()
                        + FoundryControllerUiLayout.QUANTITY_EDITBAR_X,
                screen.guiTop()
                        + FoundryControllerUiLayout.QUANTITY_EDITBAR_Y,
                0.0f,
                0.0f,
                FoundryControllerUiLayout.QUANTITY_EDITBAR_WIDTH,
                FoundryControllerUiLayout.QUANTITY_EDITBAR_HEIGHT,
                FoundryControllerUiLayout.QUANTITY_EDITBAR_WIDTH,
                FoundryControllerUiLayout.QUANTITY_EDITBAR_HEIGHT
        );

        drawControlButton(
                graphics,
                MINUS_BUTTON_TEXTURE,
                MINUS_BUTTON_HOVER_TEXTURE,
                MINUS_BUTTON_PRESSED_TEXTURE,
                FoundryControllerUiLayout.QUANTITY_MINUS_X,
                FoundryControllerUiLayout.QUANTITY_BUTTON_Y,
                FoundryControllerUiLayout.QUANTITY_MINUS_WIDTH,
                FoundryControllerUiLayout.QUANTITY_BUTTON_HEIGHT,
                startEnabled,
                isMouseOverMinus(mouseX, mouseY),
                isPressed(ControlButton.MINUS)
        );

        drawControlButton(
                graphics,
                PLUS_BUTTON_TEXTURE,
                PLUS_BUTTON_HOVER_TEXTURE,
                PLUS_BUTTON_PRESSED_TEXTURE,
                FoundryControllerUiLayout.QUANTITY_PLUS_X,
                FoundryControllerUiLayout.QUANTITY_BUTTON_Y,
                FoundryControllerUiLayout.QUANTITY_PLUS_WIDTH,
                FoundryControllerUiLayout.QUANTITY_BUTTON_HEIGHT,
                startEnabled,
                isMouseOverPlus(mouseX, mouseY),
                isPressed(ControlButton.PLUS)
        );

        boolean startPressed =
                isPressed(ControlButton.START);

        drawControlButton(
                graphics,
                START_BUTTON_TEXTURE,
                START_BUTTON_HOVER_TEXTURE,
                START_BUTTON_PRESSED_TEXTURE,
                FoundryControllerUiLayout.START_X,
                FoundryControllerUiLayout.START_Y,
                FoundryControllerUiLayout.START_WIDTH,
                FoundryControllerUiLayout.START_HEIGHT,
                mainButtonEnabled,
                isMouseOverStart(mouseX, mouseY),
                startPressed
        );

        FoundryControllerUiDrawing.drawCentered(
                graphics,
                screen.uiFont(),
                Component.literal(
                        alloying
                                ? "STOP"
                                : "START"
                ),
                screen.guiLeft()
                        + FoundryControllerUiLayout.START_X,
                screen.guiTop()
                        + FoundryControllerUiLayout.START_TEXT_Y
                        + (
                        startPressed
                                ? 1
                                : 0
                ),
                FoundryControllerUiLayout.START_WIDTH,
                FoundryControllerUiLayout.START_HEIGHT,
                alloying
                        ? FoundryControllerUiDrawing.RED
                        : startEnabled
                        ? FoundryControllerUiDrawing.GREEN
                        : FoundryControllerUiDrawing.MUTED_TEXT
        );
    }

    private void drawControlButton(
            GuiGraphics graphics,
            ResourceLocation normalTexture,
            ResourceLocation hoverTexture,
            ResourceLocation pressedTexture,
            int relativeX,
            int relativeY,
            int width,
            int height,
            boolean enabled,
            boolean hovered,
            boolean pressed
    ) {
        ResourceLocation texture =
                pressed
                        ? pressedTexture
                        : hovered && enabled
                        ? hoverTexture
                        : normalTexture;

        int left = screen.guiLeft() + relativeX;
        int top = screen.guiTop() + relativeY;

        graphics.blit(
                texture,
                left,
                top,
                0.0f,
                0.0f,
                width,
                height,
                width,
                height
        );

        if (!enabled) {
            graphics.fill(
                    left + 1,
                    top + 1,
                    left + width - 1,
                    top + height - 1,
                    0x66000000
            );
        }
    }

    private void configureControls(
            @Nullable Entry entry,
            boolean reset
    ) {
        if (menu().isAlloyJobActive()) {
            screen.configureAlloyQuantity(
                    false,
                    Math.max(
                            1,
                            menu().getActiveAlloyBatchCount()
                    ),
                    false
            );
            return;
        }

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
                maximum > 0,
                Math.max(1, maximum),
                reset
        );
    }

    private int getPreviewBatchCount(
            @Nullable Entry entry
    ) {
        if (menu().isAlloyJobActive()) {
            return Math.max(
                    1,
                    menu().getActiveAlloyBatchCount()
            );
        }

        return entry == null
                ? 1
                : Math.max(
                1,
                screen.getAlloyQuantity()
        );
    }

    private static int safeMultiply(
            int first,
            int second
    ) {
        long result =
                (long) first
                        * second;

        return result > Integer.MAX_VALUE
                ? Integer.MAX_VALUE
                : (int) result;
    }

    private List<Entry> buildEntries() {
        List<Entry> entries = new ArrayList<>();

        List<RecipeHolder<FoundryAlloyRecipe>> recipes =
                menu().getAlloyRecipes();

        for (int index = 0; index < recipes.size(); index++) {
            RecipeHolder<FoundryAlloyRecipe> holder =
                    recipes.get(index);

            FoundryAlloyRecipe recipe =
                    holder.value();

            if (
                    recipe.requiredTier() > menu().getFoundryTier()
                            || !menu().isAlloyRecipeUnlocked(recipe)
            ) {
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
                    focusedRecipeId =
                            entry.holder().id();

                    configureControls(
                            entry,
                            true
                    );
                    return;
                }
            }
        }

        focusedRecipeId =
                entries.get(0)
                        .holder()
                        .id();

        configureControls(
                entries.get(0),
                true
        );
    }

    @Nullable
    private Entry findFocusedEntry(
            List<Entry> entries
    ) {
        if (focusedRecipeId == null) {
            return null;
        }

        for (Entry entry : entries) {
            if (
                    entry.holder()
                            .id()
                            .equals(focusedRecipeId)
            ) {
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

    private boolean isMouseOverMinus(
            double mouseX,
            double mouseY
    ) {
        return FoundryControllerUiLayout.contains(
                mouseX,
                mouseY,
                screen.guiLeft(),
                screen.guiTop(),
                FoundryControllerUiLayout.QUANTITY_MINUS_X,
                FoundryControllerUiLayout.QUANTITY_BUTTON_Y,
                FoundryControllerUiLayout.QUANTITY_MINUS_WIDTH,
                FoundryControllerUiLayout.QUANTITY_BUTTON_HEIGHT
        );
    }

    private boolean isMouseOverPlus(
            double mouseX,
            double mouseY
    ) {
        return FoundryControllerUiLayout.contains(
                mouseX,
                mouseY,
                screen.guiLeft(),
                screen.guiTop(),
                FoundryControllerUiLayout.QUANTITY_PLUS_X,
                FoundryControllerUiLayout.QUANTITY_BUTTON_Y,
                FoundryControllerUiLayout.QUANTITY_PLUS_WIDTH,
                FoundryControllerUiLayout.QUANTITY_BUTTON_HEIGHT
        );
    }

    private boolean isMouseOverStart(
            double mouseX,
            double mouseY
    ) {
        return FoundryControllerUiLayout.contains(
                mouseX,
                mouseY,
                screen.guiLeft(),
                screen.guiTop(),
                FoundryControllerUiLayout.START_X,
                FoundryControllerUiLayout.START_Y,
                FoundryControllerUiLayout.START_WIDTH,
                FoundryControllerUiLayout.START_HEIGHT
        );
    }

    private void press(
            ControlButton control
    ) {
        pressedControl = control;
        pressedUntil =
                System.currentTimeMillis()
                        + PRESSED_DISPLAY_TIME_MS;
    }

    private void clearExpiredPress() {
        if (
                pressedControl != ControlButton.NONE
                        && System.currentTimeMillis() >= pressedUntil
        ) {
            pressedControl = ControlButton.NONE;
        }
    }

    private boolean isPressed(
            ControlButton control
    ) {
        return pressedControl == control
                && System.currentTimeMillis() < pressedUntil;
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
        StringBuilder result =
                new StringBuilder();

        for (
                int index = 0;
                index < recipe.ingredients().size();
                index++
        ) {
            FoundryAlloyIngredient ingredient =
                    recipe.ingredients().get(index);

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

    private static ResourceLocation texture(
            String fileName
    ) {
        return ResourceLocation.fromNamespaceAndPath(
                KatzencraftMetalsMod.MODID,
                "textures/gui/" + fileName
        );
    }

    private FoundryControllerMenu menu() {
        return screen.controllerMenu();
    }

    private enum ControlButton {
        NONE,
        MINUS,
        PLUS,
        START
    }

    private record Entry(
            int recipeIndex,
            RecipeHolder<FoundryAlloyRecipe> holder,
            MoltenMetalDefinition outputDefinition
    ) {
    }
}
