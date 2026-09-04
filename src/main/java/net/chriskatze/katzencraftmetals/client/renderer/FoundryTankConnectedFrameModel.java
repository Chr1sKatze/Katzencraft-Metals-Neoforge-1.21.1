package net.chriskatze.katzencraftmetals.client.renderer;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/**
 * Chunk-rendered diagonal/concave frame corrections for Foundry Tanks.
 *
 * This is deliberately a BakedModel wrapper instead of a Tank BlockEntity
 * renderer:
 *
 * - works before a Controller is attached;
 * - works after a Controller is removed;
 * - has no Tank BlockEntity;
 * - performs topology reads only when chunk geometry/model data is rebuilt;
 * - does not perform a per-frame world scan;
 * - Controller renderer remains liquid-only.
 *
 * The normal multipart Tank model still owns all geometry expressible through
 * the six direct-neighbor BlockState properties. This wrapper adds only the
 * diagonal-aware pieces those properties cannot represent.
 */
public final class FoundryTankConnectedFrameModel
        extends BakedModelWrapper<BakedModel> {

    private static final ModelProperty<Long> CORRECTION_MASK =
            new ModelProperty<>();

    private final EnumMap<CorrectionPart, BakedModel> correctionModels;

    private FoundryTankConnectedFrameModel(
            BakedModel originalModel,
            EnumMap<CorrectionPart, BakedModel> correctionModels
    ) {
        super(originalModel);
        this.correctionModels = correctionModels;
    }

    public static void registerAdditionalModels(
            ModelEvent.RegisterAdditional event
    ) {
        for (CorrectionPart part : CorrectionPart.values()) {
            event.register(part.location());
        }
    }

    public static void modifyBakingResult(
            ModelEvent.ModifyBakingResult event
    ) {
        EnumMap<CorrectionPart, BakedModel> correctionModels =
                new EnumMap<>(CorrectionPart.class);

        for (CorrectionPart part : CorrectionPart.values()) {
            BakedModel model =
                    event.getModels().get(part.location());

            if (model == null) {
                KatzencraftMetalsMod.LOGGER.warn(
                        "Missing Foundry Tank correction model {}",
                        part.location()
                );
                continue;
            }

            correctionModels.put(part, model);
        }

        for (BlockState state :
                ModBlocks.FOUNDRY_TANK.get()
                        .getStateDefinition()
                        .getPossibleStates()) {

            ModelResourceLocation location =
                    BlockModelShaper.stateToModelLocation(state);

            BakedModel original =
                    event.getModels().get(location);

            if (original == null) {
                continue;
            }

            event.getModels().put(
                    location,
                    new FoundryTankConnectedFrameModel(
                            original,
                            correctionModels
                    )
            );
        }
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(
            @Nullable BlockState state,
            @Nullable Direction side,
            @NotNull RandomSource rand,
            @NotNull ModelData modelData,
            @Nullable RenderType renderType
    ) {
        List<BakedQuad> base =
                originalModel.getQuads(
                        state,
                        side,
                        rand,
                        modelData,
                        renderType
                );

        /*
         * The standalone correction models contain non-cull faces. Append them
         * only in the side == null pass; otherwise the same correction could be
         * emitted again during directional cull passes.
         */
        if (state == null || side != null) {
            return base;
        }

        Long maskObject =
                modelData.get(CORRECTION_MASK);

        long mask =
                maskObject == null
                        ? 0L
                        : maskObject;

        if (mask == 0L) {
            return base;
        }

        ArrayList<BakedQuad> result =
                new ArrayList<>(base);

        for (CorrectionPart part : CorrectionPart.values()) {
            if ((mask & part.bit()) == 0L) {
                continue;
            }

            BakedModel correction =
                    correctionModels.get(part);

            if (correction == null) {
                continue;
            }

            result.addAll(
                    correction.getQuads(
                            state,
                            null,
                            rand,
                            ModelData.EMPTY,
                            renderType
                    )
            );
        }

        return result;
    }

    @Override
    public @NotNull ModelData getModelData(
            @NotNull BlockAndTintGetter level,
            @NotNull BlockPos pos,
            @NotNull BlockState state,
            @NotNull ModelData modelData
    ) {
        ModelData base =
                originalModel.getModelData(
                        level,
                        pos,
                        state,
                        modelData
                );

        if (!state.is(ModBlocks.FOUNDRY_TANK.get())) {
            return base;
        }

        long mask =
                computeCorrectionMask(
                        level,
                        pos
                );

        if (mask == 0L) {
            return base;
        }

        return base.derive()
                .with(CORRECTION_MASK, mask)
                .build();
    }

    private static long computeCorrectionMask(
            BlockAndTintGetter level,
            BlockPos pos
    ) {
        long mask = 0L;

        // -------------------------
        // Four vertical side faces
        // -------------------------
        mask |= sideFaceMask(
                level,
                pos,
                Direction.NORTH,
                Direction.WEST,
                Direction.EAST,
                CorrectionPart.SIDE_NORTH_TOP,
                CorrectionPart.SIDE_NORTH_BOTTOM,
                CorrectionPart.SIDE_NORTH_WEST,
                CorrectionPart.SIDE_NORTH_EAST,
                CorrectionPart.CAP_NORTH_TOP_WEST,
                CorrectionPart.CAP_NORTH_TOP_EAST,
                CorrectionPart.CAP_NORTH_BOTTOM_WEST,
                CorrectionPart.CAP_NORTH_BOTTOM_EAST
        );

        mask |= sideFaceMask(
                level,
                pos,
                Direction.SOUTH,
                Direction.EAST,
                Direction.WEST,
                CorrectionPart.SIDE_SOUTH_TOP,
                CorrectionPart.SIDE_SOUTH_BOTTOM,
                CorrectionPart.SIDE_SOUTH_EAST,
                CorrectionPart.SIDE_SOUTH_WEST,
                CorrectionPart.CAP_SOUTH_TOP_EAST,
                CorrectionPart.CAP_SOUTH_TOP_WEST,
                CorrectionPart.CAP_SOUTH_BOTTOM_EAST,
                CorrectionPart.CAP_SOUTH_BOTTOM_WEST
        );

        mask |= sideFaceMask(
                level,
                pos,
                Direction.WEST,
                Direction.SOUTH,
                Direction.NORTH,
                CorrectionPart.SIDE_WEST_TOP,
                CorrectionPart.SIDE_WEST_BOTTOM,
                CorrectionPart.SIDE_WEST_SOUTH,
                CorrectionPart.SIDE_WEST_NORTH,
                CorrectionPart.CAP_WEST_TOP_SOUTH,
                CorrectionPart.CAP_WEST_TOP_NORTH,
                CorrectionPart.CAP_WEST_BOTTOM_SOUTH,
                CorrectionPart.CAP_WEST_BOTTOM_NORTH
        );

        mask |= sideFaceMask(
                level,
                pos,
                Direction.EAST,
                Direction.NORTH,
                Direction.SOUTH,
                CorrectionPart.SIDE_EAST_TOP,
                CorrectionPart.SIDE_EAST_BOTTOM,
                CorrectionPart.SIDE_EAST_NORTH,
                CorrectionPart.SIDE_EAST_SOUTH,
                CorrectionPart.CAP_EAST_TOP_NORTH,
                CorrectionPart.CAP_EAST_TOP_SOUTH,
                CorrectionPart.CAP_EAST_BOTTOM_NORTH,
                CorrectionPart.CAP_EAST_BOTTOM_SOUTH
        );

        // -------------------------
        // UP / DOWN faces
        // -------------------------
        mask |= horizontalFaceMask(
                level,
                pos,
                Direction.UP,
                CorrectionPart.UP_NORTH,
                CorrectionPart.UP_SOUTH,
                CorrectionPart.UP_WEST,
                CorrectionPart.UP_EAST,
                CorrectionPart.CAP_UP_NORTH_WEST,
                CorrectionPart.CAP_UP_NORTH_EAST,
                CorrectionPart.CAP_UP_SOUTH_WEST,
                CorrectionPart.CAP_UP_SOUTH_EAST
        );

        mask |= horizontalFaceMask(
                level,
                pos,
                Direction.DOWN,
                CorrectionPart.DOWN_NORTH,
                CorrectionPart.DOWN_SOUTH,
                CorrectionPart.DOWN_WEST,
                CorrectionPart.DOWN_EAST,
                CorrectionPart.CAP_DOWN_NORTH_WEST,
                CorrectionPart.CAP_DOWN_NORTH_EAST,
                CorrectionPart.CAP_DOWN_SOUTH_WEST,
                CorrectionPart.CAP_DOWN_SOUTH_EAST
        );

        return mask;
    }

    private static long sideFaceMask(
            BlockAndTintGetter level,
            BlockPos pos,
            Direction face,
            Direction left,
            Direction right,
            CorrectionPart topPart,
            CorrectionPart bottomPart,
            CorrectionPart leftPart,
            CorrectionPart rightPart,
            CorrectionPart topLeftCap,
            CorrectionPart topRightCap,
            CorrectionPart bottomLeftCap,
            CorrectionPart bottomRightCap
    ) {
        if (isTank(level, pos.relative(face))) {
            return 0L;
        }

        boolean topCorrection =
                needsDiagonalBoundaryCorrection(
                        level,
                        pos,
                        face,
                        Direction.UP
                );

        boolean bottomCorrection =
                needsDiagonalBoundaryCorrection(
                        level,
                        pos,
                        face,
                        Direction.DOWN
                );

        boolean leftCorrection =
                needsDiagonalBoundaryCorrection(
                        level,
                        pos,
                        face,
                        left
                );

        boolean rightCorrection =
                needsDiagonalBoundaryCorrection(
                        level,
                        pos,
                        face,
                        right
                );

        boolean topBoundary =
                !hasAdjacentExposedFace(
                        level,
                        pos,
                        face,
                        Direction.UP
                );

        boolean bottomBoundary =
                !hasAdjacentExposedFace(
                        level,
                        pos,
                        face,
                        Direction.DOWN
                );

        boolean leftBoundary =
                !hasAdjacentExposedFace(
                        level,
                        pos,
                        face,
                        left
                );

        boolean rightBoundary =
                !hasAdjacentExposedFace(
                        level,
                        pos,
                        face,
                        right
                );

        long mask = 0L;

        if (topCorrection) {
            mask |= topPart.bit();
        }

        if (bottomCorrection) {
            mask |= bottomPart.bit();
        }

        if (leftCorrection) {
            mask |= leftPart.bit();
        }

        if (rightCorrection) {
            mask |= rightPart.bit();
        }

        /*
         * The explicit 1x1 side cap is required whenever a concave/diagonal
         * correction reaches another real frame boundary. At least one of the
         * two meeting edges must be diagonal-aware; ordinary convex corners are
         * already fully owned by the static model.
         */
        if (
                topBoundary
                        && leftBoundary
                        && (topCorrection || leftCorrection)
        ) {
            mask |= topLeftCap.bit();
        }

        if (
                topBoundary
                        && rightBoundary
                        && (topCorrection || rightCorrection)
        ) {
            mask |= topRightCap.bit();
        }

        if (
                bottomBoundary
                        && leftBoundary
                        && (bottomCorrection || leftCorrection)
        ) {
            mask |= bottomLeftCap.bit();
        }

        if (
                bottomBoundary
                        && rightBoundary
                        && (bottomCorrection || rightCorrection)
        ) {
            mask |= bottomRightCap.bit();
        }

        return mask;
    }

    private static long horizontalFaceMask(
            BlockAndTintGetter level,
            BlockPos pos,
            Direction face,
            CorrectionPart northPart,
            CorrectionPart southPart,
            CorrectionPart westPart,
            CorrectionPart eastPart,
            CorrectionPart northWestCap,
            CorrectionPart northEastCap,
            CorrectionPart southWestCap,
            CorrectionPart southEastCap
    ) {
        if (isTank(level, pos.relative(face))) {
            return 0L;
        }

        long mask = 0L;

        if (needsDiagonalBoundaryCorrection(
                level,
                pos,
                face,
                Direction.NORTH
        )) {
            mask |= northPart.bit();
        }

        if (needsDiagonalBoundaryCorrection(
                level,
                pos,
                face,
                Direction.SOUTH
        )) {
            mask |= southPart.bit();
        }

        if (needsDiagonalBoundaryCorrection(
                level,
                pos,
                face,
                Direction.WEST
        )) {
            mask |= westPart.bit();
        }

        if (needsDiagonalBoundaryCorrection(
                level,
                pos,
                face,
                Direction.EAST
        )) {
            mask |= eastPart.bit();
        }

        /*
         * Original top/bottom concave-corner rule:
         * both orthogonal neighbors exist, but the diagonal Tank does not.
         */
        if (needsHorizontalConcaveCap(
                level,
                pos,
                Direction.NORTH,
                Direction.WEST
        )) {
            mask |= northWestCap.bit();
        }

        if (needsHorizontalConcaveCap(
                level,
                pos,
                Direction.NORTH,
                Direction.EAST
        )) {
            mask |= northEastCap.bit();
        }

        if (needsHorizontalConcaveCap(
                level,
                pos,
                Direction.SOUTH,
                Direction.WEST
        )) {
            mask |= southWestCap.bit();
        }

        if (needsHorizontalConcaveCap(
                level,
                pos,
                Direction.SOUTH,
                Direction.EAST
        )) {
            mask |= southEastCap.bit();
        }

        return mask;
    }

    private static boolean needsDiagonalBoundaryCorrection(
            BlockAndTintGetter level,
            BlockPos pos,
            Direction face,
            Direction edge
    ) {
        BlockPos adjacent =
                pos.relative(edge);

        if (!isTank(level, adjacent)) {
            /*
             * Direct missing neighbor is already represented by the static
             * BlockState model.
             */
            return false;
        }

        /*
         * The adjacent Tank exists, but its corresponding face is occluded by
         * the Tank diagonally in front of it. This is the exact concave edge
         * that direct-neighbor BlockState properties cannot express.
         */
        return isTank(
                level,
                adjacent.relative(face)
        );
    }

    private static boolean hasAdjacentExposedFace(
            BlockAndTintGetter level,
            BlockPos pos,
            Direction face,
            Direction edge
    ) {
        BlockPos adjacent =
                pos.relative(edge);

        return isTank(level, adjacent)
                && !isTank(
                level,
                adjacent.relative(face)
        );
    }

    private static boolean needsHorizontalConcaveCap(
            BlockAndTintGetter level,
            BlockPos pos,
            Direction first,
            Direction second
    ) {
        return isTank(
                level,
                pos.relative(first)
        )
                && isTank(
                level,
                pos.relative(second)
        )
                && !isTank(
                level,
                pos.relative(first)
                        .relative(second)
        );
    }

    private static boolean isTank(
            BlockAndTintGetter level,
            BlockPos pos
    ) {
        return level.getBlockState(pos)
                .is(ModBlocks.FOUNDRY_TANK.get());
    }

    private enum CorrectionPart {
        SIDE_NORTH_TOP("foundry_tank_correction_side_north_top"),
        SIDE_NORTH_BOTTOM("foundry_tank_correction_side_north_bottom"),
        SIDE_NORTH_WEST("foundry_tank_correction_side_north_west"),
        SIDE_NORTH_EAST("foundry_tank_correction_side_north_east"),

        SIDE_SOUTH_TOP("foundry_tank_correction_side_south_top"),
        SIDE_SOUTH_BOTTOM("foundry_tank_correction_side_south_bottom"),
        SIDE_SOUTH_WEST("foundry_tank_correction_side_south_west"),
        SIDE_SOUTH_EAST("foundry_tank_correction_side_south_east"),

        SIDE_WEST_TOP("foundry_tank_correction_side_west_top"),
        SIDE_WEST_BOTTOM("foundry_tank_correction_side_west_bottom"),
        SIDE_WEST_NORTH("foundry_tank_correction_side_west_north"),
        SIDE_WEST_SOUTH("foundry_tank_correction_side_west_south"),

        SIDE_EAST_TOP("foundry_tank_correction_side_east_top"),
        SIDE_EAST_BOTTOM("foundry_tank_correction_side_east_bottom"),
        SIDE_EAST_NORTH("foundry_tank_correction_side_east_north"),
        SIDE_EAST_SOUTH("foundry_tank_correction_side_east_south"),

        UP_NORTH("foundry_tank_correction_up_north"),
        UP_SOUTH("foundry_tank_correction_up_south"),
        UP_WEST("foundry_tank_correction_up_west"),
        UP_EAST("foundry_tank_correction_up_east"),

        DOWN_NORTH("foundry_tank_correction_down_north"),
        DOWN_SOUTH("foundry_tank_correction_down_south"),
        DOWN_WEST("foundry_tank_correction_down_west"),
        DOWN_EAST("foundry_tank_correction_down_east"),

        CAP_NORTH_TOP_WEST("foundry_tank_correction_cap_north_top_west"),
        CAP_NORTH_TOP_EAST("foundry_tank_correction_cap_north_top_east"),
        CAP_NORTH_BOTTOM_WEST("foundry_tank_correction_cap_north_bottom_west"),
        CAP_NORTH_BOTTOM_EAST("foundry_tank_correction_cap_north_bottom_east"),

        CAP_SOUTH_TOP_WEST("foundry_tank_correction_cap_south_top_west"),
        CAP_SOUTH_TOP_EAST("foundry_tank_correction_cap_south_top_east"),
        CAP_SOUTH_BOTTOM_WEST("foundry_tank_correction_cap_south_bottom_west"),
        CAP_SOUTH_BOTTOM_EAST("foundry_tank_correction_cap_south_bottom_east"),

        CAP_WEST_TOP_NORTH("foundry_tank_correction_cap_west_top_north"),
        CAP_WEST_TOP_SOUTH("foundry_tank_correction_cap_west_top_south"),
        CAP_WEST_BOTTOM_NORTH("foundry_tank_correction_cap_west_bottom_north"),
        CAP_WEST_BOTTOM_SOUTH("foundry_tank_correction_cap_west_bottom_south"),

        CAP_EAST_TOP_NORTH("foundry_tank_correction_cap_east_top_north"),
        CAP_EAST_TOP_SOUTH("foundry_tank_correction_cap_east_top_south"),
        CAP_EAST_BOTTOM_NORTH("foundry_tank_correction_cap_east_bottom_north"),
        CAP_EAST_BOTTOM_SOUTH("foundry_tank_correction_cap_east_bottom_south"),

        CAP_UP_NORTH_WEST("foundry_tank_correction_cap_up_north_west"),
        CAP_UP_NORTH_EAST("foundry_tank_correction_cap_up_north_east"),
        CAP_UP_SOUTH_WEST("foundry_tank_correction_cap_up_south_west"),
        CAP_UP_SOUTH_EAST("foundry_tank_correction_cap_up_south_east"),

        CAP_DOWN_NORTH_WEST("foundry_tank_correction_cap_down_north_west"),
        CAP_DOWN_NORTH_EAST("foundry_tank_correction_cap_down_north_east"),
        CAP_DOWN_SOUTH_WEST("foundry_tank_correction_cap_down_south_west"),
        CAP_DOWN_SOUTH_EAST("foundry_tank_correction_cap_down_south_east");

        private final ModelResourceLocation location;
        private final long bit;

        CorrectionPart(
                String modelName
        ) {
            this.location =
                    ModelResourceLocation.standalone(
                            ResourceLocation.fromNamespaceAndPath(
                                    KatzencraftMetalsMod.MODID,
                                    "block/" + modelName
                            )
                    );

            this.bit =
                    1L << ordinal();
        }

        ModelResourceLocation location() {
            return location;
        }

        long bit() {
            return bit;
        }
    }
}
