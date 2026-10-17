package net.chriskatze.katzencraftmetals.screen;

import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/** Rendering-only customization; vanilla EditBox owns all editing and validation. */
final class FoundryQuantityEditBox extends EditBox {
    private final Font font;
    private int selectionAnchor;
    private long blinkStart;

    FoundryQuantityEditBox(Font font, int x, int y, int width, int height, Component message) {
        super(font, x, y, width, height, message);
        this.font = font;
        setBordered(false);
        setTextShadow(false);
    }

    @Override
    public void setHighlightPos(int position) {
        super.setHighlightPos(position);
        selectionAnchor = Math.clamp(position, 0, getValue().length());
    }

    @Override
    public void setCursorPosition(int position) {
        super.setCursorPosition(position);
        blinkStart = Util.getMillis();
    }

    @Override
    public void setFocused(boolean focused) {
        if (focused && !isFocused()) blinkStart = Util.getMillis();
        super.setFocused(focused);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!isVisible()) return;
        // At most two digits fit fully in the existing centered quantity field.
        // Draw no background, suggestion, focus outline, or vanilla invert highlight.
        String value = getValue();
        int cursor = Math.clamp(getCursorPosition(), 0, value.length());
        int anchor = Math.clamp(selectionAnchor, 0, value.length());
        int cursorX = getX() + font.width(value.substring(0, cursor));
        int anchorX = getX() + font.width(value.substring(0, anchor));
        if (isFocused() && cursor != anchor) {
            graphics.fill(Math.min(cursorX, anchorX), getY(), Math.max(cursorX, anchorX),
                    getY() + 8, 0xFF454545);
        }
        graphics.drawString(font, value, getX(), getY(), FoundryControllerUiDrawing.HEADING_TEXT, false);
        if (isFocused() && (Util.getMillis() - blinkStart) / 300L % 2L == 0L) {
            graphics.fill(cursorX, getY(), cursorX + 1, getY() + 8,
                    FoundryControllerUiDrawing.HEADING_TEXT);
        }
    }
}
