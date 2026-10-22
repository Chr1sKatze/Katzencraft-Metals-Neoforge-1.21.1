package net.chriskatze.katzencraftmetals.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Registered on both distributions; there is deliberately no client-to-server storage message. */
@EventBusSubscriber(modid="katzencraftmetals")
public final class FoundryDetachedNetworking {
    private FoundryDetachedNetworking() {}
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("katzencraftmetals").playToClient(FoundryDetachedPayload.TYPE,FoundryDetachedPayload.STREAM_CODEC,
                (payload,context)->context.enqueueWork(()->net.chriskatze.katzencraftmetals.client.FoundryDetachedTankCache.accept(payload)));
    }
}
