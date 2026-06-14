package net.chriskatze.katzencraftmetals.event;

import net.minecraft.world.entity.ExperienceOrb;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

@EventBusSubscriber
public class ModEvents {


    @SubscribeEvent
    public static void onLivingExperienceDrop(LivingExperienceDropEvent event) {
        event.setDroppedExperience(0);
    }

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        event.setDroppedExperience(0);
    }

    @SubscribeEvent
    public static void onExperienceOrbJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ExperienceOrb) {
            event.setCanceled(true);
        }
    }
}