package net.chriskatze.katzencraftmetals.block.entity;

import net.minecraft.resources.ResourceLocation;

/** Temporary central melting-temperature table until recipe JSON owns it. */
final class FoundryMeltingTemperatures {

    private FoundryMeltingTemperatures() {
    }

    static int getRequiredTemperature(ResourceLocation moltenMetal) {
        String path = moltenMetal.getPath();

        if (path.contains("mythril")) {
            return 2000;
        }
        if (path.contains("platinum")) {
            return 1500;
        }
        if (path.contains("steel")) {
            return 1200;
        }
        if (path.contains("iron")) {
            return 750;
        }
        if (path.contains("copper")) {
            return 600;
        }

        return 600;
    }
}
