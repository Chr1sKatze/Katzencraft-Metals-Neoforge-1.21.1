package net.chriskatze.katzencraftmetals.event;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.block.entity.FoundryMeltingRecipes;
import net.chriskatze.katzencraftmetals.metal.MetalNames;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** Uses the synchronized recipe manager and the same actual-stack recovery as the Controller. */
@EventBusSubscriber(modid = KatzencraftMetalsMod.MODID, value = Dist.CLIENT)
public final class FoundryRecoveryTooltipEvents {
    private FoundryRecoveryTooltipEvents() {}
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event) {
        FoundryMeltingRecipes.find(Minecraft.getInstance().level, event.getItemStack()).ifPresent(recipe -> {
            // Never round a nugget (or another fractional ore yield) up to a whole ore.
            if (recipe.moltenAmount() >= 6 && recipe.moltenAmount() % 6 == 0)
                event.getToolTip().add(Component.translatable("tooltip.katzencraftmetals.foundry_recovery",
                        recipe.moltenAmount() / 6, MetalNames.name(recipe.moltenMetal())).withStyle(ChatFormatting.GRAY));
        });
    }
}
