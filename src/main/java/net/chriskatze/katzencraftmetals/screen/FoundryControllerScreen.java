package net.chriskatze.katzencraftmetals.screen;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.menu.FoundryControllerMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
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
