package net.chriskatze.katzencraftmetals.screen;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.menu.KatzencraftAnvilMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class KatzencraftAnvilScreen extends AbstractContainerScreen<KatzencraftAnvilMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    KatzencraftMetalsMod.MODID,
                    "textures/gui/katzencraft_anvil.png"
            );

    public KatzencraftAnvilScreen(KatzencraftAnvilMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        graphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        renderRepairArrow(graphics, x, y);
    }

    private void renderRepairArrow(GuiGraphics graphics, int x, int y) {
        // temporary proof-of-concept arrow
        graphics.fill(
                x + 100,
                y + 48,
                x + 122,
                y + 52,
                0xFFAAAAAA
        );

        graphics.fill(
                x + 118,
                y + 44,
                x + 124,
                y + 56,
                0xFFAAAAAA
        );
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics, mouseX, mouseY, delta);
        super.render(graphics, mouseX, mouseY, delta);
        renderTooltip(graphics, mouseX, mouseY);
    }
}