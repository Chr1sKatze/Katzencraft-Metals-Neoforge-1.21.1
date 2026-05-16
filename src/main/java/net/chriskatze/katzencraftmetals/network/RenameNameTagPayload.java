package net.chriskatze.katzencraftmetals.network;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RenameNameTagPayload(InteractionHand hand, String name) implements CustomPacketPayload {

    public static final Type<RenameNameTagPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(KatzencraftMetalsMod.MODID, "rename_name_tag")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, RenameNameTagPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> {
                        buffer.writeEnum(payload.hand());
                        buffer.writeUtf(payload.name(), 64);
                    },
                    buffer -> new RenameNameTagPayload(
                            buffer.readEnum(InteractionHand.class),
                            buffer.readUtf(64)
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RenameNameTagPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ItemStack stack = context.player().getItemInHand(payload.hand());

            if (!stack.is(Items.NAME_TAG)) {
                return;
            }

            String cleanName = payload.name().trim();

            if (cleanName.isEmpty()) {
                stack.remove(DataComponents.CUSTOM_NAME);
                return;
            }

            if (cleanName.length() > 50) {
                cleanName = cleanName.substring(0, 50);
            }

            stack.set(DataComponents.CUSTOM_NAME, Component.literal(cleanName));
        });
    }
}