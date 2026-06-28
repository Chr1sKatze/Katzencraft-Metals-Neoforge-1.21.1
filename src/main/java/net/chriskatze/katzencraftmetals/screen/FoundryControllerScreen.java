package net.chriskatze.katzencraftmetals.screen;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.client.renderer.MoltenIronAnimation;
import net.chriskatze.katzencraftmetals.menu.FoundryControllerMenu;
import net.chriskatze.katzencraftmetals.metal.MoltenMetalDefinition;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;
import java.util.Optional;

public class FoundryControllerScreen
        extends AbstractContainerScreen<FoundryControllerMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    KatzencraftMetalsMod.MODID,
                    "textures/gui/foundry_controller.png"
            );

    private static final int PROGRESS_WIDTH = 52;
    private static final int PROGRESS_HEIGHT = 6;

    /*
     * Every current molten-metal sheet contains twenty vertically stacked
     * 16x16 frames, matching MoltenIronAnimation.
     */
    private static final int MOLTEN_TEXTURE_WIDTH = 16;
    private static final int MOLTEN_FRAME_HEIGHT = 16;
    private static final int MOLTEN_TEXTURE_HEIGHT =
            MOLTEN_FRAME_HEIGHT
                    * MoltenIronAnimation.TEXTURE_FRAME_COUNT;

    private static final int CONTENT_PANEL_X = 95;
    private static final int CONTENT_PANEL_Y = 17;
    private static final int CONTENT_PANEL_WIDTH = 73;
    private static final int CONTENT_PANEL_HEIGHT = 36;

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
        renderStoredMetal(graphics);
    }

    private void renderStoredMetal(
            GuiGraphics graphics
    ) {
        int panelX =
                leftPos + CONTENT_PANEL_X;

        int panelY =
                topPos + CONTENT_PANEL_Y;

        /*
         * A subtle self-contained contents card. It does not require changing
         * the existing foundry_controller.png GUI texture.
         */
        graphics.fill(
                panelX,
                panelY,
                panelX + CONTENT_PANEL_WIDTH,
                panelY + CONTENT_PANEL_HEIGHT,
                0xFF202020
        );

        graphics.fill(
                panelX + 1,
                panelY + 1,
                panelX + CONTENT_PANEL_WIDTH - 1,
                panelY + CONTENT_PANEL_HEIGHT - 1,
                0xFFB8B8B8
        );

        Optional<MoltenMetalDefinition> definitionOptional =
                menu.getStoredMetalDefinition();

        int moltenAmount =
                menu.getMoltenAmount();

        if (
                definitionOptional.isEmpty()
                        || moltenAmount <= 0
        ) {
            graphics.drawCenteredString(
                    font,
                    Component.translatable(
                            "gui.katzencraftmetals.foundry.empty"
                    ),
                    panelX + CONTENT_PANEL_WIDTH / 2,
                    panelY + 14,
                    0x404040
            );

            return;
        }

        MoltenMetalDefinition definition =
                definitionOptional.get();

        graphics.drawCenteredString(
                font,
                Component.translatable(
                        definition.translationKey()
                ),
                panelX + CONTENT_PANEL_WIDTH / 2,
                panelY + 3,
                0x303030
        );

        renderAnimatedMoltenTexture(
                graphics,
                definition.animatedTexture(),
                panelX + 6,
                panelY + 16
        );

        String oreAmount =
                formatOreAmount(
                        moltenAmount,
                        definition.unitsPerOre()
                );

        graphics.drawString(
                font,
                Component.translatable(
                        "gui.katzencraftmetals.foundry.ore_amount",
                        oreAmount
                ),
                panelX + 27,
                panelY + 16,
                0x303030,
                false
        );
    }

    private void renderAnimatedMoltenTexture(
            GuiGraphics graphics,
            ResourceLocation texture,
            int x,
            int y
    ) {
        long gameTime =
                minecraft != null
                        && minecraft.level != null
                        ? minecraft.level.getGameTime()
                        : 0L;

        MoltenIronAnimation.Frame frame =
                MoltenIronAnimation.getFrame(
                        gameTime
                );

        graphics.blit(
                texture,
                x,
                y,
                0.0f,
                frame.textureFrame()
                        * MOLTEN_FRAME_HEIGHT,
                MOLTEN_TEXTURE_WIDTH,
                MOLTEN_FRAME_HEIGHT,
                MOLTEN_TEXTURE_WIDTH,
                MOLTEN_TEXTURE_HEIGHT
        );
    }

    private static String formatOreAmount(
            int moltenUnits,
            int unitsPerOre
    ) {
        if (unitsPerOre <= 0) {
            return "0";
        }

        int hundredths =
                Math.round(
                        moltenUnits
                                * 100.0f
                                / unitsPerOre
                );

        int whole =
                hundredths / 100;

        int fraction =
                Math.floorMod(
                        hundredths,
                        100
                );

        if (fraction == 0) {
            return Integer.toString(whole);
        }

        if (fraction % 10 == 0) {
            return whole
                    + "."
                    + fraction / 10;
        }

        return String.format(
                Locale.ROOT,
                "%d.%02d",
                whole,
                fraction
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
