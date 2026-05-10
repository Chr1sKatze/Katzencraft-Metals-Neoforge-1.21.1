package net.chriskatze.katzencraftmetals.hunger;

public class CatoHungerConfig {

    // =========================
    // HUNGER DRAIN
    // =========================

    public static final int HUNGER_DRAIN_INTERVAL_STANDING_TICKS = 80;
    public static final int HUNGER_DRAIN_INTERVAL_MOVING_TICKS = 40;
    public static final int HUNGER_DRAIN_INTERVAL_SPRINTING_TICKS = 20;

    public static final int HUNGER_DRAIN_AMOUNT = 1;

    // =========================
    // HEALTH REGEN
    // =========================

    // Hunger needed to start regenerating
    public static final int REGEN_HUNGER_THRESHOLD = 75;

    // How often regen ticks
    public static final int REGEN_INTERVAL_TICKS = 20;

    // How much HP per tick (1.0F = half heart)
    public static final float REGEN_AMOUNT = 1.0F;

    // =========================
    // MAX HP PENALTY OVER TIME
    // =========================

    public static final int PENALTY_START_HUNGER = 25;

    // How often the penalty changes.
    // 20 ticks = 1 second.
    public static final int HEALTH_PENALTY_INTERVAL_TICKS = 60;

    // How much max HP is lost each penalty tick.
    // 1.0F = half heart.
    public static final float HEALTH_PENALTY_AMOUNT = 1.0F;

    // Maximum max HP loss.
    // 10.0F = 5 hearts.
    public static final float MAX_HEALTH_PENALTY = 10.0F;

    // If true, max HP is restored gradually when hunger is above threshold.
    // If false, max HP restores instantly.
    public static final boolean RESTORE_HEALTH_PENALTY_GRADUALLY = true;

    private CatoHungerConfig() {}
}