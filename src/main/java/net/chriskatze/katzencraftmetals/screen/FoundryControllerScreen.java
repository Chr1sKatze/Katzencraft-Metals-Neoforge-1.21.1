package net.chriskatze.katzencraftmetals.screen;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.client.renderer.MoltenIronAnimation;
import net.chriskatze.katzencraftmetals.menu.FoundryControllerMenu;
import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.chriskatze.katzencraftmetals.metal.MoltenMetalDefinition;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Locale;

public class FoundryControllerScreen
        extends AbstractContainerScreen<FoundryControllerMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    KatzencraftMetalsMod.MODID,
                    "textures/gui/foundry_controller.png"
            );

    private static final int PROGRESS_WIDTH = 52;
    private static final int PROGRESS_HEIGHT = 6;

    private static final int MOLTEN_TEXTURE_WIDTH = 16;
    private static final int MOLTEN_FRAME_HEIGHT = 16;
    private static final int MOLTEN_TEXTURE_HEIGHT =
            MOLTEN_FRAME_HEIGHT
                    * MoltenIronAnimation.TEXTURE_FRAME_COUNT;

    private static final int CONTENT_PANEL_X = 94;
    private static final int CONTENT_PANEL_Y = 15;
    private static final int CONTENT_PANEL_WIDTH = 76;
    private static final int CONTENT_PANEL_HEIGHT = 38;

    private static final int ROW_HEIGHT = 18;

    private final List<MoltenMetalDefinition> displayedMetals =
            ModMoltenMetals.lightestFirst();

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
        renderMetalList(graphics);
    }

    private void renderMetalList(
            GuiGraphics graphics
    ) {
        int panelX =
                leftPos + CONTENT_PANEL_X;

        int panelY =
                topPos + CONTENT_PANEL_Y;

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
                0xFF8F8F8F
        );

        if (menu.getTotalMoltenAmount() <= 0) {
            graphics.drawCenteredString(
                    font,
                    Component.translatable(
                            "gui.katzencraftmetals.foundry.empty"
                    ),
                    panelX + CONTENT_PANEL_WIDTH / 2,
                    panelY + 15,
                    0x303030
            );

            return;
        }

        MoltenMetalDefinition selected =
                menu.getSelectedMetalDefinition()
                        .orElse(null);

        int visibleRow =
                0;

        for (MoltenMetalDefinition definition : displayedMetals) {
            int amount =
                    menu.getMetalAmount(
                            definition
                    );

            if (amount <= 0) {
                continue;
            }

            int rowY =
                    panelY + 1
                            + visibleRow * ROW_HEIGHT;

            boolean isSelected =
                    selected != null
                            && selected.id().equals(
                            definition.id()
                    );

            renderMetalRow(
                    graphics,
                    definition,
                    amount,
                    panelX + 1,
                    rowY,
                    isSelected
            );

            visibleRow++;

            if (
                    visibleRow * ROW_HEIGHT
                            >= CONTENT_PANEL_HEIGHT - 2
            ) {
                break;
            }
        }
    }

    private void renderMetalRow(
            GuiGraphics graphics,
            MoltenMetalDefinition definition,
            int amount,
            int rowX,
            int rowY,
            boolean selected
    ) {
        int rowWidth =
                CONTENT_PANEL_WIDTH - 2;

        graphics.fill(
                rowX,
                rowY,
                rowX + rowWidth,
                rowY + ROW_HEIGHT,
                selected
                        ? 0xFFD0D0D0
                        : 0xFFA8A8A8
        );

        if (selected) {
            graphics.fill(
                    rowX,
                    rowY,
                    rowX + 2,
                    rowY + ROW_HEIGHT,
                    0xFFFFA32B
            );
        }

        renderAnimatedMoltenTexture(
                graphics,
                definition.animatedTexture(),
                rowX + 3,
                rowY + 1
        );

        graphics.drawString(
                font,
                Component.translatable(
                        definition.translationKey()
                ),
                rowX + 22,
                rowY + 1,
                0x303030,
                false
        );

        graphics.drawString(
                font,
                Component.translatable(
                        "gui.katzencraftmetals.foundry.ore_amount",
                        formatOreAmount(
                                amount,
                                definition.unitsPerOre()
                        )
                ),
                rowX + 22,
                rowY + 10,
                0x484848,
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
                menu.getScaledProgress(
                        PROGRESS_WIDTH
                );

        graphics.fill(
                x - 1,
                y - 1,
                x + PROGRESS_WIDTH + 1,
                y + PROGRESS_HEIGHT + 1,
                0xFF202020
        );

        graphics.fill(
                x,
                y,
                x + PROGRESS_WIDTH,
                y + PROGRESS_HEIGHT,
                0xFF555555
        );

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
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (button == 0) {
            int panelX =
                    leftPos + CONTENT_PANEL_X + 1;

            int panelY =
                    topPos + CONTENT_PANEL_Y + 1;

            int visibleRow =
                    0;

            for (MoltenMetalDefinition definition : displayedMetals) {
                int amount =
                        menu.getMetalAmount(
                                definition
                        );

                if (amount <= 0) {
                    continue;
                }

                int rowY =
                        panelY
                                + visibleRow * ROW_HEIGHT;

                if (
                        mouseX >= panelX
                                && mouseX < panelX
                                + CONTENT_PANEL_WIDTH - 2
                                && mouseY >= rowY
                                && mouseY < rowY + ROW_HEIGHT
                ) {
                    int syncId =
                            ModMoltenMetals.getSyncId(
                                    definition.id()
                            );

                    if (
                            syncId >= 0
                                    && minecraft != null
                                    && minecraft.gameMode != null
                    ) {
                        minecraft.gameMode
                                .handleInventoryButtonClick(
                                        menu.containerId,
                                        syncId
                                );

                        return true;
                    }
                }

                visibleRow++;
            }
        }

        return super.mouseClicked(
                mouseX,
                mouseY,
                button
        );
    }

    private void renderMetalSelectionTooltip(
            GuiGraphics graphics,
            int mouseX,
            int mouseY
    ) {
        int panelX =
                leftPos + CONTENT_PANEL_X + 1;

        int panelY =
                topPos + CONTENT_PANEL_Y + 1;

        int visibleRow =
                0;

        MoltenMetalDefinition selected =
                menu.getSelectedMetalDefinition()
                        .orElse(null);

        for (MoltenMetalDefinition definition : displayedMetals) {
            int amount =
                    menu.getMetalAmount(
                            definition
                    );

            if (amount <= 0) {
                continue;
            }

            int rowY =
                    panelY
                            + visibleRow * ROW_HEIGHT;

            if (
                    mouseX >= panelX
                            && mouseX < panelX
                            + CONTENT_PANEL_WIDTH - 2
                            && mouseY >= rowY
                            && mouseY < rowY + ROW_HEIGHT
            ) {
                boolean isSelected =
                        selected != null
                                && selected.id().equals(
                                definition.id()
                        );

                graphics.renderTooltip(
                        font,
                        Component.translatable(
                                isSelected
                                        ? "gui.katzencraftmetals.foundry.selected_output"
                                        : "gui.katzencraftmetals.foundry.select_output"
                        ),
                        mouseX,
                        mouseY
                );

                return;
            }

            visibleRow++;
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

        renderMetalSelectionTooltip(
                graphics,
                mouseX,
                mouseY
        );
    }
}
