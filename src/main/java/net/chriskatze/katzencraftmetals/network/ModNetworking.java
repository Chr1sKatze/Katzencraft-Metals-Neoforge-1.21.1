package net.chriskatze.katzencraftmetals.network;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = KatzencraftMetalsMod.MODID, value = Dist.CLIENT)
public class ModNetworking {

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(KatzencraftMetalsMod.MODID);

        registrar.playToServer(
                RenameNameTagPayload.TYPE,
                RenameNameTagPayload.STREAM_CODEC,
                RenameNameTagPayload::handle
        );
    }
}