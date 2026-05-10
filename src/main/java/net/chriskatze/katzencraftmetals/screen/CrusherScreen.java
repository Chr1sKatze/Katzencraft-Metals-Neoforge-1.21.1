package net.chriskatze.katzencraftmetals.screen;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.menu.CrusherMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class CrusherScreen extends AbstractContainerScreen<CrusherMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    KatzencraftMetalsMod.MODID,
                    "textures/gui/crusher.png"
            );

    public CrusherScreen(CrusherMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        graphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        renderFuelFlame(graphics, x, y);
        renderProgressArrow(graphics, x, y);

        renderProgressArrow(graphics, x, y);
    }

    private void renderProgressArrow(GuiGraphics graphics, int x, int y) {
        int progress = menu.getScaledProgress();

        if (progress <= 0) {
            return;
        }

        // background bar
        graphics.fill(
                x + 79,
                y + 34,
                x + 79 + 24,
                y + 34 + 16,
                0xFF3A3A3A
        );

        // filling bar
        graphics.fill(
                x + 79,
                y + 34,
                x + 79 + progress,
                y + 34 + 16,
                0xFFFFAA00
        );
    }

    private void renderFuelFlame(GuiGraphics graphics, int x, int y) {
        int fuelProgress = menu.getScaledFuelProgress();

        if (fuelProgress <= 0) {
            return;
        }

        graphics.fill(
                x + 56,
                y + 36 + 13 - fuelProgress,
                x + 56 + 14,
                y + 36 + 14,
                0xFFFF5500
        );
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics, mouseX, mouseY, delta);
        super.render(graphics, mouseX, mouseY, delta);
        renderTooltip(graphics, mouseX, mouseY);
    }
}