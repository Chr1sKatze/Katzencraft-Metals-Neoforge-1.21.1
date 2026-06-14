package net.chriskatze.katzencraftmetals.event;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = KatzencraftMetalsMod.MODID)
public class CatoEnchantingTableEvents {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (!event.getLevel()
                .getBlockState(event.getPos())
                .is(Blocks.ENCHANTING_TABLE)) {
            return;
        }

        event.getEntity().displayClientMessage(
                Component.literal("Enchanting is disabled."),
                true
        );

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
    }
}