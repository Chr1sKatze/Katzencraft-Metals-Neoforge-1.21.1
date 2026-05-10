package net.chriskatze.katzencraftmetals.event;

import net.chriskatze.katzencraftmetals.hunger.CatoFoodValues;
import net.chriskatze.katzencraftmetals.hunger.CatoHunger;
import net.chriskatze.katzencraftmetals.hunger.CatoHungerConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

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

    private static final ResourceLocation HUNGER_MAX_HEALTH_MODIFIER =
            ResourceLocation.fromNamespaceAndPath("katzencraftmetals", "hunger_max_health_reduction");

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        if (player.level().isClientSide) return;

        if ((player.tickCount + player.getId()) % 10 != 0) return;

        // XP removal
        if (player.experienceLevel != 0 || player.experienceProgress != 0 || player.totalExperience != 0) {
            player.experienceLevel = 0;
            player.experienceProgress = 0.0F;
            player.totalExperience = 0;
        }

        // Disable vanilla hunger
        player.getFoodData().setFoodLevel(19);
        player.getFoodData().setSaturation(0.0F);

        boolean isStandingStill = isPlayerStandingStill(player);
        boolean isSprinting = player.isSprinting();

        int drainInterval;

        if (isSprinting) {
            drainInterval = CatoHungerConfig.HUNGER_DRAIN_INTERVAL_SPRINTING_TICKS;
        } else if (isStandingStill) {
            drainInterval = CatoHungerConfig.HUNGER_DRAIN_INTERVAL_STANDING_TICKS;
        } else {
            drainInterval = CatoHungerConfig.HUNGER_DRAIN_INTERVAL_MOVING_TICKS;
        }

        boolean slowTick = (player.tickCount + player.getId()) % drainInterval == 0;

        // Hunger drain
        if (slowTick) {
            CatoHunger.add(player, -CatoHungerConfig.HUNGER_DRAIN_AMOUNT);
        }

        int hunger = CatoHunger.get(player);

        // Hunger max-health penalty
        applyHungerHealthPenalty(player);

        // Custom health regeneration
        boolean regenTick = (player.tickCount + player.getId()) % CatoHungerConfig.REGEN_INTERVAL_TICKS == 0;

        if (regenTick
                && hunger >= CatoHungerConfig.REGEN_HUNGER_THRESHOLD
                && player.getHealth() < player.getMaxHealth()) {

            player.heal(CatoHungerConfig.REGEN_AMOUNT);
        }
    }

    @SubscribeEvent
    public static void onFoodEaten(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;

        ItemStack eatenStack = event.getItem();
        int foodValue = CatoFoodValues.getValue(eatenStack.getItem());

        if (foodValue <= 0) return;

        CatoHunger.add(player, foodValue);

        // Immediately reset vanilla hunger so another food can be eaten right away
        player.getFoodData().setFoodLevel(19);
        player.getFoodData().setSaturation(0.0F);
    }

    private static boolean isPlayerStandingStill(Player player) {
        CompoundTag data = player.getPersistentData();

        if (!data.getBoolean("KatzencraftMoveCheckInitialized")) {
            data.putDouble("KatzencraftLastMoveCheckX", player.getX());
            data.putDouble("KatzencraftLastMoveCheckZ", player.getZ());
            data.putBoolean("KatzencraftMoveCheckInitialized", true);
            return true;
        }

        double lastX = data.getDouble("KatzencraftLastMoveCheckX");
        double lastZ = data.getDouble("KatzencraftLastMoveCheckZ");

        double currentX = player.getX();
        double currentZ = player.getZ();

        data.putDouble("KatzencraftLastMoveCheckX", currentX);
        data.putDouble("KatzencraftLastMoveCheckZ", currentZ);

        double dx = currentX - lastX;
        double dz = currentZ - lastZ;

        return (dx * dx + dz * dz) < 0.0025D;
    }

    private static void applyHungerHealthPenalty(Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) return;

        CompoundTag data = player.getPersistentData();

        float currentPenalty = data.getFloat("KatzencraftHungerHealthPenalty");
        int hunger = CatoHunger.get(player);

        boolean penaltyTick =
                (player.tickCount + player.getId()) % CatoHungerConfig.HEALTH_PENALTY_INTERVAL_TICKS == 0;

        if (penaltyTick) {
            if (hunger < CatoHungerConfig.PENALTY_START_HUNGER) {
                currentPenalty = Math.min(
                        CatoHungerConfig.MAX_HEALTH_PENALTY,
                        currentPenalty + CatoHungerConfig.HEALTH_PENALTY_AMOUNT
                );
            } else {
                if (CatoHungerConfig.RESTORE_HEALTH_PENALTY_GRADUALLY) {
                    currentPenalty = Math.max(
                            0.0F,
                            currentPenalty - CatoHungerConfig.HEALTH_PENALTY_AMOUNT
                    );
                } else {
                    currentPenalty = 0.0F;
                }
            }

            data.putFloat("KatzencraftHungerHealthPenalty", currentPenalty);
        }

        if (currentPenalty <= 0.0F) {
            maxHealth.removeModifier(HUNGER_MAX_HEALTH_MODIFIER);
            return;
        }

        maxHealth.addOrUpdateTransientModifier(new AttributeModifier(
                HUNGER_MAX_HEALTH_MODIFIER,
                -currentPenalty,
                AttributeModifier.Operation.ADD_VALUE
        ));

        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }
}