package net.chriskatze.katzencraftmetals.client.renderer;

import net.chriskatze.katzencraftmetals.block.entity.FoundryBasinBlockEntity;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTableBlockEntity;
import net.chriskatze.katzencraftmetals.block.entity.FoundryFaucetBlockEntity;
import net.chriskatze.katzencraftmetals.metal.MoltenMetalDefinition;
import net.minecraft.util.Mth;
import net.chriskatze.katzencraftmetals.block.custom.FoundryTableBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

record FoundryFaucetRenderContext(
        MoltenMetalDefinition metal,
        float basinSurfaceY,
        float dripDisappearY,
        @Nullable AABB tableMold
) {

    FoundryFaucetRenderContext(MoltenMetalDefinition metal, float surface, float disappear) {
        this(metal, surface, disappear, null);
    }

    /** Only table impact origins are constrained; the Basin path remains identical. */
    Vec3 impactPosition(double x, double y, double z) {
        if (tableMold == null) return new Vec3(x,y,z);
        double margin = 0.025;
        return new Vec3(Mth.clamp(x, tableMold.minX+margin, tableMold.maxX-margin), y,
                Mth.clamp(z, tableMold.minZ+margin, tableMold.maxZ-margin));
    }

    static AABB tableMold(FoundryTableBlockEntity table) {
        boolean swapped = table.getBlockState().getValue(FoundryTableBlock.FACING).getAxis() == Direction.Axis.X;
        var pos = table.getBlockPos();
        return new AABB(swapped ? FoundryTableBlockEntity.MOLD_MIN_Z : FoundryTableBlockEntity.MOLD_MIN_X,
                FoundryTableBlockEntity.MOLD_BOTTOM,
                swapped ? FoundryTableBlockEntity.MOLD_MIN_X : FoundryTableBlockEntity.MOLD_MIN_Z,
                swapped ? FoundryTableBlockEntity.MOLD_MAX_Z : FoundryTableBlockEntity.MOLD_MAX_X,
                FoundryTableBlockEntity.MOLD_OPENING,
                swapped ? FoundryTableBlockEntity.MOLD_MAX_X : FoundryTableBlockEntity.MOLD_MAX_Z).move(pos);
    }

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
                                / basin.getRequiredMoltenAmount(),
                        0.0f,
                        1.0f
                );

        float basinBlockBaseY =
                -basinTarget.distance();

        boolean tableTarget = basin instanceof FoundryTableBlockEntity;
        float minY = tableTarget ? FoundryTableBlockEntity.MOLD_BOTTOM : FoundryFaucetRenderConstants.BASIN_MIN_Y;
        float maxY = tableTarget ? FoundryTableBlockEntity.MOLD_TOP : FoundryFaucetRenderConstants.BASIN_MAX_Y;
        float basinBottomY =
                basinBlockBaseY
                        + minY;

        float basinSurfaceY =
                basinBlockBaseY
                        + Mth.lerp(
                        fillPercentage,
                        minY,
                        maxY
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
                dripDisappearY,
                basin instanceof FoundryTableBlockEntity table ? tableMold(table) : null
        );
    }
}
