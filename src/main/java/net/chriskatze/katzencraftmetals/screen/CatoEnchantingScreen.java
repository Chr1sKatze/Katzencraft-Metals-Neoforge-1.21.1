package net.chriskatze.katzencraftmetals.screen;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.menu.CatoEnchantingMenu;
import net.chriskatze.katzencraftmetals.network.ApplyCatoEnchantPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class CatoEnchantingScreen extends AbstractContainerScreen<CatoEnchantingMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    KatzencraftMetalsMod.MODID,
                    "textures/gui/cato_enchanting.png"
            );

    public CatoEnchantingScreen(CatoEnchantingMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);

        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF1F1F1F);
        graphics.fill(x + 6, y + 6, x + imageWidth - 6, y + imageHeight - 6, 0xFF2B2B2B);

        // slot highlights
        graphics.fill(x + 34, y + 46, x + 34 + 18, y + 46 + 18, 0x5533AAFF);
        graphics.fill(x + 79, y + 46, x + 79 + 18, y + 46 + 18, 0x55AA33FF);

        graphics.drawString(this.font, "Item", x + 32, y + 68, 0xFFFFFF, false);
        graphics.drawString(this.font, "Scroll", x + 73, y + 68, 0xFFFFFF, false);

        renderEnchantOptions(graphics, x, y);
    }

    private void renderEnchantOptions(GuiGraphics graphics, int x, int y) {
        ItemStack target = menu.getTargetStack();
        ItemStack scroll = menu.getScrollStack();

        if (target.isEmpty()) {
            graphics.drawString(this.font, "Insert item", x + 110, y + 25, 0xAAAAAA, false);
            return;
        }

        var scrollInfo = net.chriskatze.katzencraftmetals.enchantment.scroll.ScrollHelper
                .getScrollInfo(scroll);

        if (scrollInfo.isEmpty()) {
            graphics.drawString(this.font, "Insert scroll", x + 110, y + 25, 0xAAAAAA, false);
            return;
        }

        var options = net.chriskatze.katzencraftmetals.enchantment.CatoEnchantments
                .getAvailableDefinitions(scrollInfo.get().category(), target);

        if (options.isEmpty()) {
            graphics.drawString(this.font, "No enchantments", x + 110, y + 25, 0xFF5555, false);
            return;
        }

        int startX = x + 110;
        int startY = y + 22;

        for (int i = 0; i < options.size(); i++) {
            var option = options.get(i);

            graphics.drawString(
                    this.font,
                    option.id(),
                    startX,
                    startY + i * 14,
                    0xFFFFFF,
                    false
            );
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int x = (width - imageWidth) / 2;
            int y = (height - imageHeight) / 2;

            int startX = x + 110;
            int startY = y + 22;
            int optionWidth = 55;
            int optionHeight = 12;

            ItemStack target = menu.getTargetStack();
            ItemStack scroll = menu.getScrollStack();

            var scrollInfo = net.chriskatze.katzencraftmetals.enchantment.scroll.ScrollHelper
                    .getScrollInfo(scroll);

            if (!target.isEmpty() && scrollInfo.isPresent()) {
                var options = net.chriskatze.katzencraftmetals.enchantment.CatoEnchantments
                        .getAvailableDefinitions(scrollInfo.get().category(), target);

                for (int i = 0; i < options.size(); i++) {
                    int optionY = startY + i * 14;

                    boolean hovered =
                            mouseX >= startX &&
                                    mouseX <= startX + optionWidth &&
                                    mouseY >= optionY &&
                                    mouseY <= optionY + optionHeight;

                    if (hovered) {
                        PacketDistributor.sendToServer(new ApplyCatoEnchantPayload(i));
                        return true;
                    }
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics, mouseX, mouseY, delta);
        super.render(graphics, mouseX, mouseY, delta);
        renderTooltip(graphics, mouseX, mouseY);
    }
}