package net.chriskatze.katzencraftmetals.event;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

@EventBusSubscriber(modid = KatzencraftMetalsMod.MODID)
public class HeroOfTheVillageEvents {

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {

        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (event.getEffectInstance().getEffect().value() == MobEffects.HERO_OF_THE_VILLAGE) {
            player.removeEffect(MobEffects.HERO_OF_THE_VILLAGE);
        }
    }
}