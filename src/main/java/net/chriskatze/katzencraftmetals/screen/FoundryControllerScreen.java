package net.chriskatze.katzencraftmetals.screen;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTankBlockEntity;
import net.chriskatze.katzencraftmetals.menu.FoundryControllerMenu;
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

    private static final int PROGRESS_WIDTH = 52;
    private static final int PROGRESS_HEIGHT = 6;

    public FoundryControllerScreen(
            FoundryControllerMenu menu,
            Inventory playerInventory,
            Component title
    ) {
        super(menu, playerInventory, title);

        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();

        this.titleLabelX = 8;
        this.titleLabelY = 6;

        this.inventoryLabelX = 8;
        this.inventoryLabelY = 72;
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
                0,
                0,
                imageWidth,
                imageHeight
        );

        renderProgressBar(graphics);

        graphics.drawString(
                font,
                "Molten Iron: "
                        + menu.getMoltenIronAmount()
                        + " / "
                        + FoundryTankBlockEntity.CAPACITY,
                leftPos + 92,
                topPos + 38,
                0x404040,
                false
        );
    }

    private void renderProgressBar(
            GuiGraphics graphics
    ) {
        int x = leftPos + 62;
        int y = topPos + 56;

        int progress =
                menu.getScaledProgress(PROGRESS_WIDTH);

        /*
         * Border
         */
        graphics.fill(
                x - 1,
                y - 1,
                x + PROGRESS_WIDTH + 1,
                y + PROGRESS_HEIGHT + 1,
                0xFF202020
        );

        /*
         * Empty bar
         */
        graphics.fill(
                x,
                y,
                x + PROGRESS_WIDTH,
                y + PROGRESS_HEIGHT,
                0xFF555555
        );

        /*
         * Melting progress
         */
        if (progress > 0) {
            graphics.fill(
                    x,
                    y,
                    x + progress,
                    y + PROGRESS_HEIGHT,
                    0xFFFF8C22
            );
        }
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
    }
}