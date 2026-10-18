package net.chriskatze.katzencraftmetals.event;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.block.custom.FoundryBasinBlock;
import net.chriskatze.katzencraftmetals.block.custom.FoundryControllerBlock;
import net.chriskatze.katzencraftmetals.block.custom.FoundryFaucetBlock;
import net.chriskatze.katzencraftmetals.block.custom.FoundryTankBlock;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Stops custom Foundry actions and vanilla spectator menu fallback on both sides. */
@EventBusSubscriber(modid = KatzencraftMetalsMod.MODID)
public final class FoundrySpectatorInteractionEvents {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getEntity().isSpectator()) return;
        var block = event.getLevel().getBlockState(event.getPos()).getBlock();
        if (block instanceof FoundryControllerBlock || block instanceof FoundryFaucetBlock
                || block instanceof FoundryTankBlock || block instanceof FoundryBasinBlock) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    private FoundrySpectatorInteractionEvents() {}
}
