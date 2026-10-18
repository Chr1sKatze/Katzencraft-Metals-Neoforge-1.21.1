package net.chriskatze.katzencraftmetals.client.renderer;

import net.chriskatze.katzencraftmetals.block.entity.FoundryBasinBlockEntity;
import net.chriskatze.katzencraftmetals.block.entity.FoundryFaucetBlockEntity;
import net.chriskatze.katzencraftmetals.metal.MoltenMetalDefinition;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

record FoundryFaucetRenderContext(
        MoltenMetalDefinition metal,
        float basinSurfaceY,
        float dripDisappearY
) {

    @Nullable
    static FoundryFaucetRenderContext create(
            FoundryFaucetBlockEntity faucet,
            float partialTick
    ) {
        FoundryFaucetBlockEntity.BasinTarget basinTarget =
                FoundryFaucetBlockEntity.findBasinTarget(
                        faucet.getLevel(),
                        faucet.getBlockPos()
                );

        if (basinTarget == null) {
            return null;
        }

        FoundryBasinBlockEntity basin =
                basinTarget.basin();

        MoltenMetalDefinition metal =
                FoundryFaucetMetalResolver.resolve(
                        faucet,
                        basin
                );

        if (metal == null) {
            return null;
        }

        float displayedMoltenAmount =
                FoundryBasinFillSmoother.getDisplayedMoltenAmount(
                        basin,
                        partialTick
                );

        float fillPercentage =
                Mth.clamp(
                        displayedMoltenAmount
                                / FoundryBasinBlockEntity.REQUIRED_MOLTEN_AMOUNT,
                        0.0f,
                        1.0f
                );

        float basinBlockBaseY =
                -basinTarget.distance();

        float basinBottomY =
                basinBlockBaseY
                        + FoundryFaucetRenderConstants.BASIN_MIN_Y;

        float basinSurfaceY =
                basinBlockBaseY
                        + Mth.lerp(
                        fillPercentage,
                        FoundryFaucetRenderConstants.BASIN_MIN_Y,
                        FoundryFaucetRenderConstants.BASIN_MAX_Y
                );

        float dripDisappearY =
                displayedMoltenAmount > 0.01f
                        ? Math.max(
                        basinBottomY,
                        basinSurfaceY
                                - FoundryFaucetRenderConstants
                                .MOLTEN_SURFACE_PENETRATION
                )
                        : basinBottomY;

        return new FoundryFaucetRenderContext(
                metal,
                basinSurfaceY,
                dripDisappearY
        );
    }
}
