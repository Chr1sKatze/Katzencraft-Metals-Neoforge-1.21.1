package net.chriskatze.katzencraftmetals.event;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;

@EventBusSubscriber(
        modid = KatzencraftMetalsMod.MODID,
        bus = EventBusSubscriber.Bus.GAME,
        value = net.neoforged.api.distmarker.Dist.CLIENT
)
public class ClientEvents {

    @SubscribeEvent
    public static void onRenderGuiLayerPre(RenderGuiLayerEvent.Pre event) {
        if (event.getName().equals(ResourceLocation.withDefaultNamespace("experience_bar"))) {
            event.setCanceled(true);
        }
    }
}