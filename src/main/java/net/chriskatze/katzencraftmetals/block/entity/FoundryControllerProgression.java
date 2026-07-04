package net.chriskatze.katzencraftmetals.block.entity;

import net.minecraft.nbt.CompoundTag;

/**
 * Persistent Foundry XP and tier unlock rules.
 *
 * The thresholds are intentionally isolated here so they can be moved into a
 * config or data file without touching the Controller, menu or renderers.
 */
final class FoundryControllerProgression {

    static final int MIN_TIER = 1;
    static final int MAX_TIER = 4;

    /*
     * Total completed-melt XP required to reach each tier.
     *
     * Tier 1:   0 XP
     * Tier 2:  64 XP
     * Tier 3: 256 XP
     * Tier 4: 1024 XP
     */
    private static final int[] TOTAL_XP_FOR_TIER = {
            0,
            0,
            64,
            256,
            1024
    };

    private int experience;

    int getExperience() {
        return experience;
    }

    int getTier() {
        for (int tier = MAX_TIER; tier >= MIN_TIER; tier--) {
            if (experience >= TOTAL_XP_FOR_TIER[tier]) {
                return tier;
            }
        }

        return MIN_TIER;
    }

    int getUnlockedInputSlots() {
        return getTier() * 2;
    }

    int getUnlockedFuelSlots() {
        return getTier();
    }

    int getCurrentTierStartExperience() {
        return TOTAL_XP_FOR_TIER[getTier()];
    }

    int getNextTierExperience() {
        int tier = getTier();

        if (tier >= MAX_TIER) {
            return TOTAL_XP_FOR_TIER[MAX_TIER];
        }

        return TOTAL_XP_FOR_TIER[tier + 1];
    }

    int getExperienceIntoTier() {
        return Math.max(
                0,
                experience - getCurrentTierStartExperience()
        );
    }

    int getExperienceNeededForTier() {
        if (getTier() >= MAX_TIER) {
            return 1;
        }

        return Math.max(
                1,
                getNextTierExperience()
                        - getCurrentTierStartExperience()
        );
    }

    boolean isMaximumTier() {
        return getTier() >= MAX_TIER;
    }

    boolean addExperience(
            int amount
    ) {
        if (
                amount <= 0
                        || isMaximumTier()
        ) {
            return false;
        }

        int previousTier = getTier();

        experience = Math.min(
                TOTAL_XP_FOR_TIER[MAX_TIER],
                experience + amount
        );

        return getTier() != previousTier;
    }

    /**
     * Old Controllers had three accessible fuel slots but no tier value.
     * Preserve access to any occupied legacy slot by granting the minimum tier
     * required for the highest occupied fuel slot.
     */
    void migrateLegacyFuelAccess(
            int highestOccupiedFuelSlot
    ) {
        if (highestOccupiedFuelSlot < 1) {
            return;
        }

        int requiredTier = Math.min(
                MAX_TIER,
                highestOccupiedFuelSlot + 1
        );

        experience = Math.max(
                experience,
                TOTAL_XP_FOR_TIER[requiredTier]
        );
    }

    void save(
            CompoundTag tag
    ) {
        tag.putInt(
                "FoundryExperience",
                experience
        );

        tag.putInt(
                "FoundryProgressionVersion",
                1
        );
    }

    void load(
            CompoundTag tag
    ) {
        experience = Math.max(
                0,
                Math.min(
                        TOTAL_XP_FOR_TIER[MAX_TIER],
                        tag.getInt(
                                "FoundryExperience"
                        )
                )
        );
    }
}
