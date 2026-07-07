package net.chriskatze.katzencraftmetals.client.renderer;

import net.chriskatze.katzencraftmetals.block.entity.CastingCauldronBlockEntity;
import net.chriskatze.katzencraftmetals.block.entity.FoundryFaucetBlockEntity;
import net.chriskatze.katzencraftmetals.metal.MoltenMetalDefinition;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

record FoundryFaucetRenderContext(
        MoltenMetalDefinition metal,
        float cauldronSurfaceY,
        float dripDisappearY
) {

    @Nullable
    static FoundryFaucetRenderContext create(
            FoundryFaucetBlockEntity faucet,
            float partialTick
    ) {
        FoundryFaucetBlockEntity.CauldronTarget cauldronTarget =
                FoundryFaucetBlockEntity.findCauldronTarget(
                        faucet.getLevel(),
                        faucet.getBlockPos()
                );

        if (cauldronTarget == null) {
            return null;
        }

        CastingCauldronBlockEntity cauldron =
                cauldronTarget.cauldron();

        MoltenMetalDefinition metal =
                FoundryFaucetMetalResolver.resolve(
                        faucet,
                        cauldron
                );

        if (metal == null) {
            return null;
        }

        float displayedMoltenAmount =
                CastingCauldronFillSmoother.getDisplayedMoltenAmount(
                        cauldron,
                        partialTick
                );

        float fillPercentage =
                Mth.clamp(
                        displayedMoltenAmount
                                / CastingCauldronBlockEntity.REQUIRED_MOLTEN_AMOUNT,
                        0.0f,
                        1.0f
                );

        float cauldronBlockBaseY =
                -cauldronTarget.distance();

        float cauldronBottomY =
                cauldronBlockBaseY
                        + FoundryFaucetRenderConstants.CAULDRON_MIN_Y;

        float cauldronSurfaceY =
                cauldronBlockBaseY
                        + Mth.lerp(
                        fillPercentage,
                        FoundryFaucetRenderConstants.CAULDRON_MIN_Y,
                        FoundryFaucetRenderConstants.CAULDRON_MAX_Y
                );

        float dripDisappearY =
                displayedMoltenAmount > 0.01f
                        ? Math.max(
                        cauldronBottomY,
                        cauldronSurfaceY
                                - FoundryFaucetRenderConstants
                                .MOLTEN_SURFACE_PENETRATION
                )
                        : cauldronBottomY;

        return new FoundryFaucetRenderContext(
                metal,
                cauldronSurfaceY,
                dripDisappearY
        );
    }
}
