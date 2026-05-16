package net.chriskatze.katzencraftmetals.event;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.menu.CatoEnchantingMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(
        modid = KatzencraftMetalsMod.MODID,
        bus = EventBusSubscriber.Bus.GAME
)
public class EnchantingTableOverrideEvents {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide) {
            return;
        }

        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        BlockState state = event.getLevel().getBlockState(event.getPos());

        if (!(state.getBlock() instanceof EnchantingTableBlock)) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        MenuProvider provider = new SimpleMenuProvider(
                (containerId, inventory, p) -> new CatoEnchantingMenu(containerId, inventory, event.getPos()),
                Component.literal("Enchanting")
        );

        player.openMenu(provider, buffer -> buffer.writeBlockPos(event.getPos()));

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}