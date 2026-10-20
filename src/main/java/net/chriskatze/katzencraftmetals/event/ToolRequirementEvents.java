package net.chriskatze.katzencraftmetals.event;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.config.BlockRequirements;
import net.chriskatze.katzencraftmetals.config.ToolLevels;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid=KatzencraftMetalsMod.MODID)
public final class ToolRequirementEvents {
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void onHarvestCheck(PlayerEvent.HarvestCheck event){
        var requirement=BlockRequirements.find(event.getTargetBlock());
        if(requirement.isPresent()&&!ToolLevels.satisfies(event.getEntity().getMainHandItem(),event.getTargetBlock(),requirement.getAsInt()))
            event.setCanHarvest(false);
        // Never set true: preserve native correct-tool rules and other mods' denials.
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event){
        var requirement=BlockRequirements.find(event.getState());
        if(requirement.isPresent()&&!ToolLevels.satisfies(event.getEntity().getMainHandItem(),event.getState(),requirement.getAsInt()))
            event.setNewSpeed(event.getNewSpeed()*.01f);
    }
}
