package net.chriskatze.katzencraftmetals.screen;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.menu.FoundryControllerMenu;
import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.chriskatze.katzencraftmetals.metal.MoltenMetalDefinition;
import net.chriskatze.katzencraftmetals.recipe.FoundryAlloyIngredient;
import net.chriskatze.katzencraftmetals.recipe.FoundryAlloyRecipe;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

final class FoundryControllerMetalsRenderer {

    private static final ResourceLocation UNLOCKED_SLOT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    KatzencraftMetalsMod.MODID,
                    "textures/gui/foundry_controller_unlocked_slot.png"
            );

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
        List<Entry> entries =
                buildEntries();

        normalizeSelection(entries);
        clampScroll(entries.size());

        for (
                int visibleRow = 0;
                visibleRow
                        < FoundryControllerUiLayout.VISIBLE_METAL_ROWS;
                visibleRow++
        ) {
            int index =
                    scrollOffset
                            + visibleRow;

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

        renderRecipe(
                graphics,
                findFocusedEntry(entries)
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

        List<Entry> entries =
                buildEntries();

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
                    scrollOffset
                            + visibleRow;

            if (index >= entries.size()) {
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

            Entry entry =
                    entries.get(index);

            focusedMetal =
                    entry.definition()
                            .id();

            int maximum =
                    entry.recipe() == null
                            ? 0
                            : menu().getMaxCraftableBatches(
                            entry.recipe()
                                    .value()
                    );

            screen.configureAlloyQuantity(
                    maximum > 0
                            && !menu().isAlloyJobActive(),
                    maximum,
                    true
            );

            return true;
        }

        Entry focused =
                findFocusedEntry(entries);

        if (
                focused == null
                        || focused.recipe() == null
        ) {
            return false;
        }

        int maximum =
                menu().getMaxCraftableBatches(
                        focused.recipe()
                                .value()
                );

        boolean enabled =
                maximum > 0
                        && !menu().isAlloyJobActive();

        if (
                FoundryControllerUiLayout.contains(
                        mouseX,
                        mouseY,
                        left,
                        top,
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
                        left,
                        top,
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
                        left,
                        top,
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
        List<Entry> entries =
                buildEntries();

        int maxOffset =
                Math.max(
                        0,
                        entries.size()
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
        List<Entry> entries =
                buildEntries();

        for (
                int visibleRow = 0;
                visibleRow
                        < FoundryControllerUiLayout.VISIBLE_METAL_ROWS;
                visibleRow++
        ) {
            int index =
                    scrollOffset
                            + visibleRow;

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
                Entry entry =
                        entries.get(index);

                graphics.renderTooltip(
                        screen.uiFont(),
                        entry.recipe() == null
                                ? Component.literal(
                                "Stored molten metal"
                        )
                                : Component.literal(
                                "Select to inspect and start this alloy recipe"
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
                        + FoundryControllerUiLayout.METAL_LIST_X;

        int top =
                screen.guiTop()
                        + FoundryControllerUiLayout.METAL_LIST_Y
                        + visibleRow
                        * FoundryControllerUiLayout.METAL_ROW_HEIGHT;

        boolean focused =
                entry.definition()
                        .id()
                        .equals(
                                focusedMetal
                        );

        drawRowFrame(
                graphics,
                left,
                top,
                focused
        );

        ItemStack icon =
                getDisplayIcon(
                        entry.definition()
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

        int secondaryColor =
                focused
                        ? 0xFF4A4A4A
                        : FoundryControllerUiDrawing.MUTED_TEXT;

        graphics.drawString(
                screen.uiFont(),
                Component.literal(
                        displayName(
                                entry.definition()
                                        .id()
                        )
                ),
                left + 24,
                top + 2,
                textColor,
                false
        );

        int storedAmount =
                menu().getMetalAmount(
                        entry.definition()
                );

        if (storedAmount > 0) {
            graphics.drawString(
                    screen.uiFont(),
                    FoundryControllerUiDrawing.formatOreAmount(
                            storedAmount,
                            entry.definition()
                                    .unitsPerOre()
                    )
                            + " ore",
                    left + 24,
                    top + 11,
                    secondaryColor,
                    false
            );
        }
    }

    private void renderRecipe(
            GuiGraphics graphics,
            @Nullable Entry focused
    ) {
        if (
                focused == null
                        || focused.recipe() == null
        ) {
            screen.configureAlloyQuantity(
                    false,
                    1,
                    false
            );

            renderResultOnly(
                    graphics,
                    focused
            );

            renderRequiredTemperature(
                    graphics,
                    0
            );

            return;
        }

        FoundryAlloyRecipe recipe =
                focused.recipe()
                        .value();

        int maximum =
                menu().getMaxCraftableBatches(
                        recipe
                );

        boolean enabled =
                maximum > 0
                        && !menu().isAlloyJobActive();

        screen.configureAlloyQuantity(
                enabled,
                Math.max(1, maximum),
                false
        );

        int quantity =
                enabled
                        ? Math.min(
                        maximum,
                        screen.getAlloyQuantity()
                )
                        : 1;

        int[] ingredientX = {
                FoundryControllerUiLayout.INGREDIENT_ONE_X,
                FoundryControllerUiLayout.INGREDIENT_TWO_X,
                FoundryControllerUiLayout.INGREDIENT_THREE_X
        };

        for (
                int index = 0;
                index < recipe.ingredients().size();
                index++
        ) {
            FoundryAlloyIngredient ingredient =
                    recipe.ingredients()
                            .get(index);

            Optional<MoltenMetalDefinition> definition =
                    ModMoltenMetals.get(
                            ingredient.metal()
                    );

            if (definition.isEmpty()) {
                continue;
            }

            int x =
                    screen.guiLeft()
                            + ingredientX[index];

            int y =
                    screen.guiTop()
                            + FoundryControllerUiLayout.RECIPE_SLOT_Y;

            renderUnlockedRecipeSlot(
                    graphics,
                    x,
                    y
            );

            graphics.renderItem(
                    getDisplayIcon(
                            definition.get()
                    ),
                    x,
                    y
            );

            renderAmount(
                    graphics,
                    x,
                    y,
                    ingredient.amount()
                            * quantity,
                    definition.get()
                            .unitsPerOre()
            );
        }

        Optional<MoltenMetalDefinition> outputDefinition =
                ModMoltenMetals.get(
                        recipe.outputMetal()
                );

        if (outputDefinition.isPresent()) {
            int x =
                    screen.guiLeft()
                            + FoundryControllerUiLayout.RESULT_X;

            int y =
                    screen.guiTop()
                            + FoundryControllerUiLayout.RECIPE_SLOT_Y;

            renderUnlockedRecipeSlot(
                    graphics,
                    x,
                    y
            );

            graphics.renderItem(
                    getDisplayIcon(
                            outputDefinition.get()
                    ),
                    x,
                    y
            );

            renderAmount(
                    graphics,
                    x,
                    y,
                    recipe.outputAmount()
                            * quantity,
                    outputDefinition.get()
                            .unitsPerOre()
            );
        }

        renderRequiredTemperature(
                graphics,
                recipe.requiredTemperature()
        );

        if (!enabled) {
            graphics.fill(
                    screen.guiLeft()
                            + FoundryControllerUiLayout.START_X,
                    screen.guiTop()
                            + FoundryControllerUiLayout.START_Y,
                    screen.guiLeft()
                            + FoundryControllerUiLayout.START_X
                            + FoundryControllerUiLayout.START_WIDTH,
                    screen.guiTop()
                            + FoundryControllerUiLayout.START_Y
                            + FoundryControllerUiLayout.START_HEIGHT,
                    0x66000000
            );
        }
    }

    private void renderResultOnly(
            GuiGraphics graphics,
            @Nullable Entry focused
    ) {
        if (focused == null) {
            return;
        }

        ItemStack result =
                getDisplayIcon(
                        focused.definition()
                );

        if (result.isEmpty()) {
            return;
        }

        int x =
                screen.guiLeft()
                        + FoundryControllerUiLayout.RESULT_X;

        int y =
                screen.guiTop()
                        + FoundryControllerUiLayout.RECIPE_SLOT_Y;

        renderUnlockedRecipeSlot(
                graphics,
                x,
                y
        );

        graphics.renderItem(
                result,
                x,
                y
        );
    }

    private void renderRequiredTemperature(
            GuiGraphics graphics,
            int temperature
    ) {
        FoundryControllerUiDrawing.drawCentered(
                graphics,
                screen.uiFont(),
                Component.literal(
                        temperature <= 0
                                ? "-"
                                : temperature + "°C"
                ),
                screen.guiLeft() + 248,
                screen.guiTop()
                        + FoundryControllerUiLayout.REQUIRED_TEMPERATURE_Y,
                31,
                10,
                temperature <= 0
                        ? FoundryControllerUiDrawing.MUTED_TEXT
                        : FoundryControllerUiDrawing.TEXT
        );
    }

    private void renderUnlockedRecipeSlot(
            GuiGraphics graphics,
            int x,
            int y
    ) {
        graphics.blit(
                UNLOCKED_SLOT_TEXTURE,
                x - 1,
                y - 1,
                0.0f,
                0.0f,
                18,
                18,
                18,
                18
        );
    }

    private void renderAmount(
            GuiGraphics graphics,
            int x,
            int y,
            int units,
            int unitsPerOre
    ) {
        String amount =
                FoundryControllerUiDrawing.formatOreAmount(
                        units,
                        unitsPerOre
                );

        graphics.drawString(
                screen.uiFont(),
                amount,
                x + 16
                        - screen.uiFont()
                        .width(amount),
                y + 9,
                0xFFFFFFFF,
                true
        );
    }

    private List<Entry> buildEntries() {
        List<Entry> entries =
                new ArrayList<>();

        List<RecipeHolder<FoundryAlloyRecipe>> recipes =
                menu().getAlloyRecipes();

        Set<ResourceLocation> included =
                new HashSet<>();

        for (
                MoltenMetalDefinition definition :
                ModMoltenMetals.lightestFirst()
        ) {
            int amount =
                    menu().getMetalAmount(
                            definition
                    );

            if (amount <= 0) {
                continue;
            }

            RecipeReference recipe =
                    findRecipeForOutput(
                            recipes,
                            definition.id()
                    );

            entries.add(
                    new Entry(
                            definition,
                            recipe == null
                                    ? -1
                                    : recipe.index(),
                            recipe == null
                                    ? null
                                    : recipe.holder()
                    )
            );

            included.add(
                    definition.id()
            );
        }

        ResourceLocation activeAlloyOutput =
                menu().getActiveAlloyOutput()
                        .map(
                                MoltenMetalDefinition::id
                        )
                        .orElse(null);

        for (
                int index = 0;
                index < recipes.size();
                index++
        ) {
            RecipeHolder<FoundryAlloyRecipe> holder =
                    recipes.get(index);

            FoundryAlloyRecipe recipe =
                    holder.value();

            boolean activeRecipeOutput =
                    recipe.outputMetal()
                            .equals(
                                    activeAlloyOutput
                            );

            if (
                    included.contains(
                            recipe.outputMetal()
                    )
                            || (
                            menu().getMaxCraftableBatches(
                                    recipe
                            ) <= 0
                                    && !activeRecipeOutput
                    )
            ) {
                continue;
            }

            Optional<MoltenMetalDefinition> outputDefinition =
                    ModMoltenMetals.get(
                            recipe.outputMetal()
                    );

            if (outputDefinition.isEmpty()) {
                continue;
            }

            MoltenMetalDefinition definition =
                    outputDefinition.get();

            entries.add(
                    new Entry(
                            definition,
                            index,
                            holder
                    )
            );

            included.add(
                    definition.id()
            );
        }

        return entries;
    }

    @Nullable
    private static RecipeReference findRecipeForOutput(
            List<RecipeHolder<FoundryAlloyRecipe>> recipes,
            ResourceLocation output
    ) {
        for (int index = 0; index < recipes.size(); index++) {
            RecipeHolder<FoundryAlloyRecipe> holder =
                    recipes.get(index);

            if (
                    holder.value()
                            .outputMetal()
                            .equals(output)
            ) {
                return new RecipeReference(
                        index,
                        holder
                );
            }
        }

        return null;
    }

    private void normalizeSelection(
            List<Entry> entries
    ) {
        if (entries.isEmpty()) {
            focusedMetal = null;
            scrollOffset = 0;
            screen.configureAlloyQuantity(
                    false,
                    1,
                    false
            );
            return;
        }

        Entry current =
                findFocusedEntry(entries);

        if (current != null) {
            return;
        }

        Entry first =
                entries.get(0);

        focusedMetal =
                first.definition()
                        .id();

        int maximum =
                first.recipe() == null
                        ? 0
                        : menu().getMaxCraftableBatches(
                        first.recipe()
                                .value()
                );

        screen.configureAlloyQuantity(
                maximum > 0
                        && !menu().isAlloyJobActive(),
                Math.max(1, maximum),
                true
        );
    }

    @Nullable
    private Entry findFocusedEntry(
            List<Entry> entries
    ) {
        if (focusedMetal == null) {
            return null;
        }

        for (Entry entry : entries) {
            if (
                    entry.definition()
                            .id()
                            .equals(
                                    focusedMetal
                            )
            ) {
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
                                - FoundryControllerUiLayout.VISIBLE_METAL_ROWS
                );

        int thumbHeight =
                maxOffset <= 0
                        ? 14
                        : Math.max(
                        10,
                        height
                                * FoundryControllerUiLayout.VISIBLE_METAL_ROWS
                                / entryCount
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

    private record Entry(
            MoltenMetalDefinition definition,
            int recipeIndex,
            @Nullable RecipeHolder<FoundryAlloyRecipe> recipe
    ) {
    }

    private record RecipeReference(
            int index,
            RecipeHolder<FoundryAlloyRecipe> holder
    ) {
    }
}
