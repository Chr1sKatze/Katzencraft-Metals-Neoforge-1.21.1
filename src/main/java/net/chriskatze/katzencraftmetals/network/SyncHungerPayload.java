package net.chriskatze.katzencraftmetals.network;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.client.ClientHungerData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncHungerPayload(int hunger) implements CustomPacketPayload {

    public static final Type<SyncHungerPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KatzencraftMetalsMod.MODID, "sync_hunger"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncHungerPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    SyncHungerPayload::hunger,
                    SyncHungerPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncHungerPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientHungerData.setHunger(payload.hunger()));
    }
}