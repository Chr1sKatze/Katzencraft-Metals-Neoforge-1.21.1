package net.chriskatze.katzencraftmetals.network;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.menu.CatoEnchantingMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ApplyCatoEnchantPayload(int optionIndex) implements CustomPacketPayload {

    public static final Type<ApplyCatoEnchantPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(KatzencraftMetalsMod.MODID, "apply_cato_enchant")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ApplyCatoEnchantPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> buffer.writeInt(payload.optionIndex()),
                    buffer -> new ApplyCatoEnchantPayload(buffer.readInt())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ApplyCatoEnchantPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof CatoEnchantingMenu menu) {
                menu.applyEnchant(context.player(), payload.optionIndex());
            }
        });
    }
}