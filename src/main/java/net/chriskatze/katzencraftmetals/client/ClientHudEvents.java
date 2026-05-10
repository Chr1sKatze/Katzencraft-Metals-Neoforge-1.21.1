package net.chriskatze.katzencraftmetals.client;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;

@EventBusSubscriber(
        modid = KatzencraftMetalsMod.MODID,
        bus = EventBusSubscriber.Bus.GAME,
        value = net.neoforged.api.distmarker.Dist.CLIENT
)
public class ClientHudEvents {

    private static float displayedHealthPercent = 100.0F;
    private static float displayedMaxHealthPercent = 100.0F;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null) return;
        if (minecraft.options.hideGui) return;

        GuiGraphics guiGraphics = event.getGuiGraphics();
        ClientHungerData.tickDisplay();

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        // =========================
        // HEALTH BAR (draw first)
        // =========================

        float currentHealth = minecraft.player.getHealth();
        float maxHealth = minecraft.player.getMaxHealth();

        float normalMaxHealth = 20.0F;

        float healthValue = Math.max(0.0F, Math.min(normalMaxHealth, currentHealth));
        float maxHealthValue = Math.max(0.0F, Math.min(normalMaxHealth, maxHealth));

        float targetHealthPercent = (healthValue / normalMaxHealth) * 100.0F;

        displayedHealthPercent += (targetHealthPercent - displayedHealthPercent) * 0.15F;

        if (Math.abs(displayedHealthPercent - targetHealthPercent) < 0.05F) {
            displayedHealthPercent = targetHealthPercent;
        }

        // Do not smooth max health.
        // Otherwise a tiny missing section can appear when max HP is restored.
        displayedMaxHealthPercent = (maxHealthValue / normalMaxHealth) * 100.0F;

        int healthBarWidth = 100;
        int healthBarHeight = 8;

        int healthX = screenWidth / 2 - healthBarWidth / 2;
        int healthY = screenHeight - 62;

        int currentFilledWidth = (int) ((displayedHealthPercent / 100.0F) * healthBarWidth);
        int maxAllowedWidth = (int) ((displayedMaxHealthPercent / 100.0F) * healthBarWidth);

        // Border
        guiGraphics.fill(healthX - 1, healthY - 1, healthX + healthBarWidth + 1, healthY + healthBarHeight + 1, 0xFF000000);

        // Empty
        guiGraphics.fill(healthX, healthY, healthX + healthBarWidth, healthY + healthBarHeight, 0xFF2A0A0A);

        // Current HP
        guiGraphics.fill(healthX, healthY, healthX + currentFilledWidth, healthY + healthBarHeight, 0xFFE02B2B);

        // Locked HP from hunger penalty
        if (maxAllowedWidth < healthBarWidth) {
            guiGraphics.fill(healthX + maxAllowedWidth, healthY, healthX + healthBarWidth, healthY + healthBarHeight, 0xFF4A4A4A);
        }

        String healthText = Math.round(currentHealth) + "/" + Math.round(maxHealth);
        guiGraphics.drawString(
                minecraft.font,
                healthText,
                screenWidth / 2 - minecraft.font.width(healthText) / 2,
                healthY - 10,
                0xFFFFFFFF,
                true
        );

        // =========================
        // HUNGER BAR (draw second)
        // =========================

        int hunger = ClientHungerData.getHunger();
        float displayedHunger = ClientHungerData.getDisplayedHunger();

        int barWidth = 100;
        int barHeight = 8;

        int x = screenWidth / 2 - barWidth / 2;
        int y = screenHeight - 49;

        int filledWidth = (int) ((displayedHunger / 100.0F) * barWidth);

        // Border
        guiGraphics.fill(x - 1, y - 1, x + barWidth + 1, y + barHeight + 1, 0xFF000000);

        // Empty
        guiGraphics.fill(x, y, x + barWidth, y + barHeight, 0xFF3A1F0B);

        // Filled
        guiGraphics.fill(x, y, x + filledWidth, y + barHeight, 0xFFE68A2E);

        String text = hunger + "/100";
        guiGraphics.drawString(
                minecraft.font,
                text,
                screenWidth / 2 - minecraft.font.width(text) / 2,
                y - 10,
                0xFFFFFFFF,
                true
        );

        // =========================
        // ARMOR + TOUGHNESS NUMBERS
        // =========================

        int armorValue = minecraft.player.getArmorValue();
        double toughnessValue = minecraft.player.getAttributeValue(
                net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS
        );

        int armorTextX = healthX - 28;
        int armorTextY = healthY - 1;

        String armorText = String.valueOf(armorValue);
        String toughnessText = String.format("%.1f", toughnessValue);

        guiGraphics.drawString(
                minecraft.font,
                armorText,
                armorTextX,
                armorTextY,
                0xFFFFFFFF,
                true
        );

        guiGraphics.drawString(
                minecraft.font,
                toughnessText,
                armorTextX,
                armorTextY + 10,
                0xFFAAAAAA,
                true
        );
    }

    @SubscribeEvent
    public static void onRenderGuiLayerPre(RenderGuiLayerEvent.Pre event) {
        ResourceLocation name = event.getName();

        if (name.equals(ResourceLocation.withDefaultNamespace("food_level"))
                || name.equals(ResourceLocation.withDefaultNamespace("player_health"))
                || name.equals(ResourceLocation.withDefaultNamespace("health"))
                || name.equals(ResourceLocation.withDefaultNamespace("armor_level"))) {
            event.setCanceled(true);
        }
    }
}