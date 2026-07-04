package net.chriskatze.katzencraftmetals.screen;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.menu.FoundryControllerMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class FoundryControllerScreen
        extends AbstractContainerScreen<FoundryControllerMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    KatzencraftMetalsMod.MODID,
                    "textures/gui/foundry_controller.png"
            );

    private final FoundryControllerOverviewRenderer overviewRenderer =
            new FoundryControllerOverviewRenderer(this);

    private EditBox alloyQuantityBox;
    private int alloyQuantityLimit = 1;
    private boolean alloyQuantityEnabled;

    public FoundryControllerScreen(
            FoundryControllerMenu menu,
            Inventory playerInventory,
            Component title
    ) {
        super(
                menu,
                playerInventory,
                title
        );

        imageWidth = FoundryControllerUiLayout.WIDTH;
        imageHeight = FoundryControllerUiLayout.HEIGHT;
    }

    @Override
    protected void init() {
        super.init();

        titleLabelX = -1000;
        inventoryLabelX = -1000;

        alloyQuantityBox =
                new EditBox(
                        font,
                        leftPos
                                + FoundryControllerUiLayout.QUANTITY_X,
                        topPos
                                + FoundryControllerUiLayout.QUANTITY_Y,
                        FoundryControllerUiLayout.QUANTITY_WIDTH,
                        10,
                        Component.literal(
                                "Alloy batch amount"
                        )
                );

        alloyQuantityBox.setBordered(false);
        alloyQuantityBox.setMaxLength(2);
        alloyQuantityBox.setValue("");
        alloyQuantityBox.setTextColor(
                FoundryControllerUiDrawing.TEXT
        );

        alloyQuantityBox.setFilter(
                value -> {
                    if (value.isEmpty()) {
                        return true;
                    }

                    try {
                        int parsed =
                                Integer.parseInt(value);

                        return parsed >= 1
                                && parsed
                                <= FoundryControllerMenu.MAX_ALLOY_BATCHES;
                    } catch (NumberFormatException ignored) {
                        return false;
                    }
                }
        );

        addRenderableWidget(
                alloyQuantityBox
        );
    }

    @Override
    protected void renderBg(
            GuiGraphics graphics,
            float partialTick,
            int mouseX,
            int mouseY
    ) {
        graphics.blit(
                TEXTURE,
                leftPos,
                topPos,
                0.0f,
                0.0f,
                imageWidth,
                imageHeight,
                FoundryControllerUiLayout.WIDTH,
                FoundryControllerUiLayout.HEIGHT
        );

        overviewRenderer.render(
                graphics,
                mouseX,
                mouseY
        );
    }

    @Override
    protected void renderLabels(
            GuiGraphics graphics,
            int mouseX,
            int mouseY
    ) {
        /* All labels belong to the authored texture or section renderers. */
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (
                overviewRenderer.mouseClicked(
                        mouseX,
                        mouseY,
                        button
                )
        ) {
            return true;
        }

        return super.mouseClicked(
                mouseX,
                mouseY,
                button
        );
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double scrollX,
            double scrollY
    ) {
        if (
                overviewRenderer.mouseScrolled(
                        mouseX,
                        mouseY,
                        scrollY
                )
        ) {
            return true;
        }

        return super.mouseScrolled(
                mouseX,
                mouseY,
                scrollX,
                scrollY
        );
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderBackground(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );

        super.render(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );

        renderTooltip(
                graphics,
                mouseX,
                mouseY
        );

        overviewRenderer.renderTooltips(
                graphics,
                mouseX,
                mouseY
        );
    }

    int guiLeft() {
        return leftPos;
    }

    int guiTop() {
        return topPos;
    }

    Font uiFont() {
        return font;
    }

    Minecraft minecraftClient() {
        return minecraft;
    }

    FoundryControllerMenu controllerMenu() {
        return menu;
    }

    int getAlloyQuantity() {
        if (
                alloyQuantityBox == null
                        || alloyQuantityBox.getValue().isEmpty()
        ) {
            return 1;
        }

        try {
            return Math.max(
                    1,
                    Math.min(
                            alloyQuantityLimit,
                            Integer.parseInt(
                                    alloyQuantityBox.getValue()
                            )
                    )
            );
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    void configureAlloyQuantity(
            boolean enabled,
            int maximum,
            boolean reset
    ) {
        if (alloyQuantityBox == null) {
            return;
        }

        alloyQuantityLimit =
                Math.max(
                        1,
                        Math.min(
                                FoundryControllerMenu.MAX_ALLOY_BATCHES,
                                maximum
                        )
                );

        alloyQuantityEnabled =
                enabled;

        alloyQuantityBox.setEditable(
                enabled
        );

        if (!enabled) {
            alloyQuantityBox.setValue("");
            return;
        }

        if (
                reset
                        || alloyQuantityBox.getValue().isEmpty()
        ) {
            alloyQuantityBox.setValue("1");
        } else {
            alloyQuantityBox.setValue(
                    Integer.toString(
                            getAlloyQuantity()
                    )
            );
        }
    }

    void changeAlloyQuantity(
            int delta
    ) {
        if (
                alloyQuantityBox == null
                        || !alloyQuantityEnabled
        ) {
            return;
        }

        int next =
                Math.max(
                        1,
                        Math.min(
                                alloyQuantityLimit,
                                getAlloyQuantity()
                                        + delta
                        )
                );

        alloyQuantityBox.setValue(
                Integer.toString(next)
        );
    }

    boolean sendMenuButton(
            int buttonId
    ) {
        if (
                minecraft == null
                        || minecraft.gameMode == null
        ) {
            return false;
        }

        minecraft.gameMode.handleInventoryButtonClick(
                menu.containerId,
                buttonId
        );

        return true;
    }
}
