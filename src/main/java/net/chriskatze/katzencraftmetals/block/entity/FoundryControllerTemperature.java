package net.chriskatze.katzencraftmetals.block.entity;

import net.minecraft.nbt.CompoundTag;

/** Owns the persistent heat state of one Foundry Controller. */
final class FoundryControllerTemperature {

    static final int AMBIENT_TEMPERATURE = 20;
    static final int HEATING_PER_TICK = 2;
    static final int COOLING_INTERVAL_TICKS = 20;

    private final FoundryControllerBlockEntity controller;

    private int currentTemperature = AMBIENT_TEMPERATURE;
    private int coolingTicker;

    FoundryControllerTemperature(FoundryControllerBlockEntity controller) {
        this.controller = controller;
    }

    void heatTick(int fuelMaximumTemperature) {
        coolingTicker = 0;

        int maximum = Math.min(
                Math.max(AMBIENT_TEMPERATURE, fuelMaximumTemperature),
                getTierMaximumTemperature()
        );

        int next = Math.min(
                maximum,
                currentTemperature + HEATING_PER_TICK
        );

        if (next != currentTemperature) {
            currentTemperature = next;
            controller.setChanged();
        }
    }

    void coolTick() {
        if (currentTemperature <= AMBIENT_TEMPERATURE) {
            currentTemperature = AMBIENT_TEMPERATURE;
            coolingTicker = 0;
            return;
        }

        coolingTicker++;
        if (coolingTicker < COOLING_INTERVAL_TICKS) {
            return;
        }

        coolingTicker = 0;
        currentTemperature--;
        controller.setChanged();
    }

    boolean isHotEnough(int requiredTemperature) {
        return currentTemperature >= requiredTemperature;
    }

    int getCurrentTemperature() {
        return currentTemperature;
    }

    int getTierMaximumTemperature() {
        return switch (controller.getFoundryTier()) {
            case 1 -> 900;
            case 2 -> 1300;
            case 3 -> 1700;
            default -> 2200;
        };
    }

    void save(CompoundTag tag) {
        tag.putInt("CurrentTemperature", currentTemperature);
    }

    void load(CompoundTag tag) {
        currentTemperature = Math.max(
                AMBIENT_TEMPERATURE,
                Math.min(
                        getTierMaximumTemperature(),
                        tag.contains("CurrentTemperature")
                                ? tag.getInt("CurrentTemperature")
                                : AMBIENT_TEMPERATURE
                )
        );
        coolingTicker = 0;
    }
}
