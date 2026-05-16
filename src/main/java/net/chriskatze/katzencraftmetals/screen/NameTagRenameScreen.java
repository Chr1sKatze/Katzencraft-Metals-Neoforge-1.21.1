package net.chriskatze.katzencraftmetals.screen;

import net.chriskatze.katzencraftmetals.network.RenameNameTagPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.PacketDistributor;

public class NameTagRenameScreen extends Screen {

    private final InteractionHand hand;
    private final String currentName;

    private EditBox nameBox;

    public NameTagRenameScreen(InteractionHand hand, String currentName) {
        super(Component.literal("Rename Nametag"));
        this.hand = hand;
        this.currentName = currentName;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.nameBox = new EditBox(
                this.font,
                centerX - 100,
                centerY - 20,
                200,
                20,
                Component.literal("Name")
        );

        this.nameBox.setMaxLength(50);
        this.nameBox.setValue(currentName);

        this.addRenderableWidget(nameBox);

        this.nameBox.setFocused(true);
        this.setFocused(this.nameBox);

        this.addRenderableWidget(Button.builder(
                Component.literal("Done"),
                button -> {
                    PacketDistributor.sendToServer(new RenameNameTagPayload(hand, nameBox.getValue()));
                    Minecraft.getInstance().setScreen(null);
                }
        ).bounds(centerX - 100, centerY + 10, 95, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Cancel"),
                button -> Minecraft.getInstance().setScreen(null)
        ).bounds(centerX + 5, centerY + 10, 95, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(
                this.font,
                this.title,
                this.width / 2,
                this.height / 2 - 45,
                0xFFFFFF
        );

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {

        // ENTER
        if (keyCode == 257 || keyCode == 335) {

            PacketDistributor.sendToServer(
                    new RenameNameTagPayload(hand, nameBox.getValue())
            );

            Minecraft.getInstance().setScreen(null);
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}