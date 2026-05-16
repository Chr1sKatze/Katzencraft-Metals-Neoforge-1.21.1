package net.chriskatze.katzencraftmetals.network;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = KatzencraftMetalsMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModNetworking {

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(KatzencraftMetalsMod.MODID);

        registrar.playToServer(
                RenameNameTagPayload.TYPE,
                RenameNameTagPayload.STREAM_CODEC,
                RenameNameTagPayload::handle
        );

        registrar.playToServer(
                ApplyCatoEnchantPayload.TYPE,
                ApplyCatoEnchantPayload.STREAM_CODEC,
                ApplyCatoEnchantPayload::handle
        );

        registrar.playToClient(
                SyncHungerPayload.TYPE,
                SyncHungerPayload.STREAM_CODEC,
                SyncHungerPayload::handle
        );
    }
}