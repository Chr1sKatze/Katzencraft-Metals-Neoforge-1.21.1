package net.chriskatze.katzencraftmetals.screen;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.menu.FuelChamberMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class FuelChamberScreen
        extends AbstractContainerScreen<FuelChamberMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    KatzencraftMetalsMod.MODID,
                    "textures/gui/fuel_chamber.png"
            );

    public FuelChamberScreen(
            FuelChamberMenu menu,
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
    public void render(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        super.render(
                guiGraphics,
                mouseX,
                mouseY,
                partialTick
        );

        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(
            GuiGraphics guiGraphics,
            float partialTick,
            int mouseX,
            int mouseY
    ) {
        guiGraphics.blit(
                TEXTURE,
                leftPos,
                topPos,
                0,
                0,
                imageWidth,
                imageHeight
        );
    }
}