package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.chriskatze.katzencraftmetals.block.entity.FoundryControllerBlockEntity;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTankNetwork;
import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.chriskatze.katzencraftmetals.metal.MoltenMetalDefinition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.LIQUID_EPSILON;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.LIQUID_INSET;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.LIQUID_MAX_INSET;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.PIXEL;

/**
 * Renders all molten Tank volume once from the Controller.
 *
 * The renderer consumes only the Controller's cached structure and synchronized
 * pooled metal amounts. It never discovers a Tank network while rendering.
 */
final class FoundryControllerTankLiquidRenderer {

    /*
     * Visual-only boundary treatment between two different molten metals.
     *
     * The actual stored volumes remain perfectly flat and exact. The renderer
     * changes only the visible side boundary. At any point either the lower
     * metal pushes upward OR the upper metal pushes downward, never both.
     * This keeps one continuous wavy boundary and prevents detached strips.
     */
    private static final int INTERFACE_SEGMENTS_PER_BLOCK = 8;
    private static final float INTERFACE_MAX_WAVE_AMPLITUDE = 1.35f * PIXEL;
    private static final float INTERFACE_MIN_WAVE_AMPLITUDE = 0.05f * PIXEL;

    /*
     * Real shader blur width around the moving wavy contact line.
     *
     * The normal side faces are carved away by the complete shader band extent,
     * so this blur has no coplanar geometry underneath it.
     */
    private static final float INTERFACE_SHADER_BLUR_HALF_WIDTH =
            0.35f * PIXEL;

    /*
     * Two broad world-space waves are combined. Their long wavelengths prevent
     * the interface from looking noisy or like a separate wave per Tank block.
     */
    private static final double INTERFACE_PRIMARY_WAVELENGTH = 5.00D;
    private static final double INTERFACE_SECONDARY_WAVELENGTH = 3.25D;
    private static final double INTERFACE_PRIMARY_SPEED = 0.88D;
    private static final double INTERFACE_SECONDARY_SPEED = -0.50D;

    private final FoundryControllerLiquidSmoother smoother =
            new FoundryControllerLiquidSmoother();

    private final FoundryControllerSurfaceEffectsRenderer surfaceEffects =
            new FoundryControllerSurfaceEffectsRenderer();

    void render(
            FoundryControllerBlockEntity controller,
            FoundryTankNetwork network,
            Set<BlockPos> structure,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedOverlay
    ) {
        Level level = controller.getLevel();

        if (
                level == null
                        || network == null
                        || structure == null
                        || structure.isEmpty()
        ) {
            return;
        }

        Map<ResourceLocation, Float> displayedGlobalAmounts =
                smoother.getDisplayedAmounts(
                        controller,
                        network,
                        partialTick
                );

        if (displayedGlobalAmounts.isEmpty()) {
            return;
        }

        Map<Integer, Map<ResourceLocation, Float>> amountsByY =
                distributeAcrossHorizontalLevels(
                        structure,
                        displayedGlobalAmounts
                );

        Map<BlockPos, List<FoundryTankRenderedMetalLayer>> renderedLayers =
                new LinkedHashMap<>();

        for (BlockPos tankPos : structure) {
            renderedLayers.put(
                    tankPos,
                    createRenderedLayers(
                            tankPos,
                            structure,
                            amountsByY
                    )
            );
        }

        BlockPos controllerPos = controller.getBlockPos();

        /*
         * Give the complete Foundry one stable molten-animation phase derived
         * from its Controller position.
         *
         * Every Tank cell and every metal layer in this Foundry receives the
         * exact same frame, so the connected liquid remains visually seamless.
         * A different Foundry normally has a different Controller position and
         * therefore a different phase, preventing all Foundries in the world
         * from pulsing in perfect synchronization.
         */
        MoltenIronAnimation.Frame animationFrame =
                MoltenIronAnimation.getFrame(
                        level.getGameTime(),
                        controllerPos.asLong()
                );

        for (BlockPos tankPos : structure) {
            List<FoundryTankRenderedMetalLayer> localLayers =
                    renderedLayers.getOrDefault(tankPos, List.of());

            if (localLayers.isEmpty()) {
                continue;
            }

            float minX = structure.contains(tankPos.west())
                    ? 0.0f
                    : LIQUID_INSET;
            float maxX = structure.contains(tankPos.east())
                    ? 1.0f
                    : LIQUID_MAX_INSET;
            float minZ = structure.contains(tankPos.north())
                    ? 0.0f
                    : LIQUID_INSET;
            float maxZ = structure.contains(tankPos.south())
                    ? 1.0f
                    : LIQUID_MAX_INSET;

            poseStack.pushPose();
            poseStack.translate(
                    tankPos.getX() - controllerPos.getX(),
                    tankPos.getY() - controllerPos.getY(),
                    tankPos.getZ() - controllerPos.getZ()
            );
            PoseStack.Pose pose = poseStack.last();

            for (FoundryTankRenderedMetalLayer renderedLayer : localLayers) {
                MoltenMetalDefinition definition =
                        ModMoltenMetals.get(renderedLayer.metal())
                                .orElse(null);

                if (definition == null) {
                    continue;
                }

                VertexConsumer consumer = bufferSource.getBuffer(
                        RenderType.entityTranslucentCull(
                                definition.animatedTexture()
                        )
                );

                FoundryTankLiquidGeometry geometry =
                        new FoundryTankLiquidGeometry(
                                minX,
                                maxX,
                                renderedLayer.minY(),
                                renderedLayer.maxY(),
                                minZ,
                                maxZ,
                                renderedLayer.renderTop(),
                                renderedLayer.renderBottom()
                        );

                /*
                 * Do not render the old perfectly flat horizontal face when a
                 * different molten metal begins immediately above this layer.
                 *
                 * The visual interface ribbon now represents that boundary.
                 * Keeping the old internal top face as well makes its projected
                 * edge show through the glass as a straight one-pixel strip.
                 *
                 * True exposed liquid surfaces still render normally.
                 */
                boolean differentMetalDirectlyAbove =
                        hasDifferentMetalDirectlyAbove(
                                tankPos,
                                renderedLayer,
                                localLayers,
                                renderedLayers
                        );

                if (
                        renderedLayer.renderTop()
                                && !differentMetalDirectlyAbove
                ) {
                    FoundryTankLiquidQuads.renderLiquidHorizontalFace(
                            Direction.UP,
                            consumer,
                            pose,
                            minX,
                            maxX,
                            renderedLayer.maxY(),
                            minZ,
                            maxZ,
                            packedOverlay,
                            animationFrame.minV(),
                            animationFrame.maxV()
                    );
                }

                /*
                 * Only the true bottom of the complete liquid volume is emitted
                 * here. Internal metal-to-metal boundaries are closed later by
                 * renderMetalInterfaceSeam() with matching wavy UP/DOWN meshes.
                 */
                if (renderedLayer.renderBottom()) {
                    FoundryTankLiquidQuads.renderLiquidHorizontalFace(
                            Direction.DOWN,
                            consumer,
                            pose,
                            minX,
                            maxX,
                            renderedLayer.minY(),
                            minZ,
                            maxZ,
                            packedOverlay,
                            animationFrame.minV(),
                            animationFrame.maxV()
                    );
                }

                for (Direction side : Direction.Plane.HORIZONTAL) {
                    renderLayerSide(
                            tankPos,
                            side,
                            renderedLayer,
                            geometry,
                            renderedLayers,
                            consumer,
                            pose,
                            packedOverlay,
                            animationFrame.minV(),
                            animationFrame.maxV()
                    );
                }

                boolean isExposedTopSurface =
                        renderedLayer == localLayers.getLast()
                                && allLayersEmpty(
                                renderedLayers.get(tankPos.above())
                        );

                if (renderedLayer.renderTop() && isExposedTopSurface) {
                    surfaceEffects.renderAndSpawn(
                            controller,
                            tankPos,
                            structure,
                            definition,
                            geometry,
                            partialTick,
                            pose,
                            bufferSource,
                            packedOverlay,
                            animationFrame.minV(),
                            animationFrame.maxV()
                    );
                }
            }

            /*
             * Render the material interfaces only after the normal liquid
             * volume. The base slabs therefore remain the source of truth for
             * quantity while the ribbon acts purely as visual polish.
             */
            renderMetalInterfaceSeams(
                    controllerPos,
                    tankPos,
                    structure,
                    localLayers,
                    renderedLayers,
                    minX,
                    maxX,
                    minZ,
                    maxZ,
                    level.getGameTime(),
                    partialTick,
                    pose,
                    bufferSource,
                    packedOverlay,
                    animationFrame.minV(),
                    animationFrame.maxV()
            );

            poseStack.popPose();
        }
    }

    private static Map<Integer, Map<ResourceLocation, Float>> distributeAcrossHorizontalLevels(
            Set<BlockPos> structure,
            Map<ResourceLocation, Float> globalAmounts
    ) {
        Map<Integer, Integer> tanksPerY = new TreeMap<>();

        for (BlockPos pos : structure) {
            tanksPerY.merge(pos.getY(), 1, Integer::sum);
        }

        Map<ResourceLocation, Float> remaining =
                new LinkedHashMap<>(globalAmounts);

        Map<Integer, Map<ResourceLocation, Float>> result =
                new TreeMap<>();

        for (Map.Entry<Integer, Integer> yEntry : tanksPerY.entrySet()) {
            int tankCount = Math.max(1, yEntry.getValue());
            float free = tankCount * (float) FoundryTankNetwork.TANK_CAPACITY;
            Map<ResourceLocation, Float> averagePerTank = new LinkedHashMap<>();

            for (MoltenMetalDefinition definition : ModMoltenMetals.heaviestFirst()) {
                if (free <= LIQUID_EPSILON) {
                    break;
                }

                float available = remaining.getOrDefault(definition.id(), 0.0f);

                if (available <= LIQUID_EPSILON) {
                    continue;
                }

                float placed = Math.min(free, available);
                averagePerTank.put(definition.id(), placed / tankCount);

                float left = available - placed;
                if (left > LIQUID_EPSILON) {
                    remaining.put(definition.id(), left);
                } else {
                    remaining.remove(definition.id());
                }

                free -= placed;
            }

            result.put(yEntry.getKey(), Map.copyOf(averagePerTank));
        }

        return result;
    }

    private static List<FoundryTankRenderedMetalLayer> createRenderedLayers(
            BlockPos tankPos,
            Set<BlockPos> structure,
            Map<Integer, Map<ResourceLocation, Float>> amountsByY
    ) {
        Map<ResourceLocation, Float> displayedAmounts =
                amountsByY.getOrDefault(tankPos.getY(), Map.of());

        if (displayedAmounts.isEmpty()) {
            return List.of();
        }

        Map<ResourceLocation, Float> belowAmounts =
                structure.contains(tankPos.below())
                        ? amountsByY.getOrDefault(tankPos.getY() - 1, Map.of())
                        : Map.of();

        Map<ResourceLocation, Float> aboveAmounts =
                structure.contains(tankPos.above())
                        ? amountsByY.getOrDefault(tankPos.getY() + 1, Map.of())
                        : Map.of();

        boolean anyLiquidBelow = sum(belowAmounts) > LIQUID_EPSILON;
        float cumulativeAmount = 0.0f;
        List<FoundryTankRenderedMetalLayer> result = new ArrayList<>();
        List<MoltenMetalDefinition> ordered = ModMoltenMetals.heaviestFirst();

        for (int index = 0; index < ordered.size(); index++) {
            MoltenMetalDefinition definition = ordered.get(index);
            float layerAmount = displayedAmounts.getOrDefault(definition.id(), 0.0f);

            if (layerAmount <= LIQUID_EPSILON) {
                continue;
            }

            float startAmount = cumulativeAmount;
            cumulativeAmount = Math.min(
                    FoundryTankNetwork.TANK_CAPACITY,
                    cumulativeAmount + layerAmount
            );
            float endAmount = cumulativeAmount;

            float minY = startAmount <= LIQUID_EPSILON && anyLiquidBelow
                    ? 0.0f
                    : Mth.lerp(
                            Mth.clamp(
                                    startAmount / FoundryTankNetwork.TANK_CAPACITY,
                                    0.0f,
                                    1.0f
                            ),
                            LIQUID_INSET,
                            LIQUID_MAX_INSET
                    );

            boolean continuesAbove =
                    endAmount >= FoundryTankNetwork.TANK_CAPACITY - LIQUID_EPSILON
                            && sum(aboveAmounts) > LIQUID_EPSILON;

            float maxY = continuesAbove
                    ? 1.0f
                    : Mth.lerp(
                            Mth.clamp(
                                    endAmount / FoundryTankNetwork.TANK_CAPACITY,
                                    0.0f,
                                    1.0f
                            ),
                            LIQUID_INSET,
                            LIQUID_MAX_INSET
                    );

            ResourceLocation nextMetal =
                    getNextDisplayedMetal(displayedAmounts, ordered, index + 1);

            boolean renderTop;
            if (nextMetal != null) {
                renderTop = !nextMetal.equals(definition.id());
            } else if (continuesAbove) {
                ResourceLocation metalAbove = getBottomDisplayedMetal(aboveAmounts);
                renderTop = metalAbove == null || !metalAbove.equals(definition.id());
            } else {
                renderTop = true;
            }

            boolean renderBottom = result.isEmpty() && !anyLiquidBelow;

            if (maxY - minY > LIQUID_EPSILON) {
                result.add(
                        new FoundryTankRenderedMetalLayer(
                                definition.id(),
                                minY,
                                maxY,
                                renderTop,
                                renderBottom
                        )
                );
            }
        }

        return List.copyOf(result);
    }

    private static void renderLayerSide(
            BlockPos tankPos,
            Direction side,
            FoundryTankRenderedMetalLayer renderedLayer,
            FoundryTankLiquidGeometry geometry,
            Map<BlockPos, List<FoundryTankRenderedMetalLayer>> allLayers,
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int packedOverlay,
            float frameMinV,
            float frameMaxV
    ) {
        BlockPos neighborPos = tankPos.relative(side);
        List<FoundryTankRenderedMetalLayer> neighborLayers = allLayers.get(neighborPos);

        if (neighborLayers == null) {
            float visibleMinY =
                    renderedLayer.minY();

            float visibleMaxY =
                    renderedLayer.maxY();

            /*
             * When the custom two-metal shader is active, carve its complete
             * interface strip out of the normal exterior side geometry first.
             *
             * The shader strip will fill exactly this missing region later.
             * Therefore:
             *
             * - no duplicate side geometry;
             * - no coplanar shader/base faces;
             * - no new z-fighting source.
             *
             * Internal Tank-to-Tank faces are deliberately left untouched.
             */
            if (FoundryMetalBlendRenderType.isAvailable()) {
                InterfaceTrim trim =
                        getShaderInterfaceTrim(
                                tankPos,
                                renderedLayer,
                                allLayers
                        );

                visibleMinY += trim.bottom();
                visibleMaxY -= trim.top();
            }

            if (visibleMaxY - visibleMinY <= LIQUID_EPSILON) {
                return;
            }

            FoundryTankLiquidQuads.renderLiquidSideSegment(
                    side,
                    consumer,
                    pose,
                    geometry,
                    visibleMinY,
                    visibleMaxY,
                    false,
                    packedOverlay,
                    frameMinV,
                    frameMaxV
            );
            return;
        }

        /*
         * Connected/internal side handling stays exactly as before. The shader
         * interface is only rendered on the outside of the vessel, so carving
         * shared Tank faces here would create holes.
         */
        List<FoundryTankVerticalInterval> visible = new ArrayList<>();
        visible.add(
                new FoundryTankVerticalInterval(
                        renderedLayer.minY(),
                        renderedLayer.maxY()
                )
        );

        for (FoundryTankRenderedMetalLayer neighborLayer : neighborLayers) {
            visible = subtractInterval(
                    visible,
                    new FoundryTankVerticalInterval(
                            neighborLayer.minY(),
                            neighborLayer.maxY()
                    )
            );

            if (visible.isEmpty()) {
                return;
            }
        }

        for (FoundryTankVerticalInterval interval : visible) {
            FoundryTankLiquidQuads.renderLiquidSideSegment(
                    side,
                    consumer,
                    pose,
                    geometry,
                    interval.minY(),
                    interval.maxY(),
                    true,
                    packedOverlay,
                    frameMinV,
                    frameMaxV
            );
        }
    }

    /**
     * Adds the visual mixing ribbon between different molten metals.
     *
     * Same-block interfaces are handled directly between adjacent local layers.
     * If one metal completely fills this Tank block and a different metal starts
     * in the Tank directly above, the ribbon is allowed to cross the block seam
     * so a large multi-block Foundry still reads as one continuous vessel.
     */
    private static void renderMetalInterfaceSeams(
            BlockPos controllerPos,
            BlockPos tankPos,
            Set<BlockPos> structure,
            List<FoundryTankRenderedMetalLayer> localLayers,
            Map<BlockPos, List<FoundryTankRenderedMetalLayer>> allLayers,
            float minX,
            float maxX,
            float minZ,
            float maxZ,
            long gameTime,
            float partialTick,
            PoseStack.Pose pose,
            MultiBufferSource bufferSource,
            int packedOverlay,
            float frameMinV,
            float frameMaxV
    ) {
        /*
         * Do not early-out when this Tank is surrounded horizontally.
         *
         * The old interface system only drew side ribbons, so an interior Tank
         * had nothing to render. The interface now also contains horizontal
         * closure meshes, which must exist in every Tank cell so the complete
         * liquid stack is closed when viewed from above or below.
         */
        for (int index = 0; index + 1 < localLayers.size(); index++) {
            FoundryTankRenderedMetalLayer lower = localLayers.get(index);
            FoundryTankRenderedMetalLayer upper = localLayers.get(index + 1);

            if (lower.metal().equals(upper.metal())) {
                continue;
            }

            float boundaryY =
                    (lower.maxY() + upper.minY()) * 0.5f;

            renderMetalInterfaceSeam(
                    controllerPos,
                    tankPos,
                    structure,
                    lower,
                    upper,
                    boundaryY,
                    minX,
                    maxX,
                    minZ,
                    maxZ,
                    gameTime,
                    partialTick,
                    pose,
                    bufferSource,
                    packedOverlay,
                    frameMinV,
                    frameMaxV
            );
        }

        /*
         * The density-sorted stack can also change metal exactly at Y=1.0
         * between this Tank block and the one above it.
         */
        FoundryTankRenderedMetalLayer lower = localLayers.getLast();

        if (
                lower.maxY() >= 1.0f - LIQUID_EPSILON
                        && structure.contains(tankPos.above())
        ) {
            List<FoundryTankRenderedMetalLayer> aboveLayers =
                    allLayers.get(tankPos.above());

            if (aboveLayers != null && !aboveLayers.isEmpty()) {
                FoundryTankRenderedMetalLayer upper = aboveLayers.getFirst();

                if (
                        upper.minY() <= LIQUID_EPSILON
                                && !lower.metal().equals(upper.metal())
                ) {
                    renderMetalInterfaceSeam(
                            controllerPos,
                            tankPos,
                            structure,
                            lower,
                            upper,
                            1.0f,
                            minX,
                            maxX,
                            minZ,
                            maxZ,
                            gameTime,
                            partialTick,
                            pose,
                            bufferSource,
                            packedOverlay,
                            frameMinV,
                            frameMaxV
                    );
                }
            }
        }
    }

    private static void renderMetalInterfaceSeam(
            BlockPos controllerPos,
            BlockPos tankPos,
            Set<BlockPos> structure,
            FoundryTankRenderedMetalLayer lowerLayer,
            FoundryTankRenderedMetalLayer upperLayer,
            float boundaryY,
            float minX,
            float maxX,
            float minZ,
            float maxZ,
            long gameTime,
            float partialTick,
            PoseStack.Pose pose,
            MultiBufferSource bufferSource,
            int packedOverlay,
            float frameMinV,
            float frameMaxV
    ) {
        MoltenMetalDefinition lowerDefinition =
                ModMoltenMetals.get(lowerLayer.metal())
                        .orElse(null);

        MoltenMetalDefinition upperDefinition =
                ModMoltenMetals.get(upperLayer.metal())
                        .orElse(null);

        if (lowerDefinition == null || upperDefinition == null) {
            return;
        }

        InterfaceMetrics metrics =
                getInterfaceMetrics(
                        lowerLayer,
                        upperLayer
                );

        long interfaceSeed =
                createInterfaceSeed(
                        controllerPos,
                        lowerLayer.metal(),
                        upperLayer.metal()
                );

        /*
         * Close the metal-to-metal boundary from BOTH viewing directions.
         *
         * IMPORTANT BUFFERING RULE:
         *
         * MultiBufferSource may finish the currently active BufferBuilder when
         * another RenderType is requested. Never keep a VertexConsumer from one
         * molten-metal RenderType, request another RenderType, and then return
         * to the old consumer. That stale consumer throws "Not building!".
         *
         * Therefore every interface pass is deliberately:
         *
         *   getBuffer(...) -> emit ALL vertices for that pass -> move on
         *
         * The lower metal receives an UP-facing wavy mesh and the upper metal
         * receives the exact same geometry with opposite winding as a
         * DOWN-facing mesh.
         */
        float horizontalWaveAmplitude =
                metrics.active()
                        ? metrics.waveAmplitude()
                        : 0.0f;

        VertexConsumer lowerHorizontalConsumer =
                bufferSource.getBuffer(
                        RenderType.entityTranslucentCull(
                                lowerDefinition.animatedTexture()
                        )
                );

        renderInterfaceHorizontalSurface(
                Direction.UP,
                lowerHorizontalConsumer,
                pose,
                tankPos,
                minX,
                maxX,
                minZ,
                maxZ,
                boundaryY,
                horizontalWaveAmplitude,
                interfaceSeed,
                gameTime,
                partialTick,
                packedOverlay,
                frameMinV,
                frameMaxV
        );

        VertexConsumer upperHorizontalConsumer =
                bufferSource.getBuffer(
                        RenderType.entityTranslucentCull(
                                upperDefinition.animatedTexture()
                        )
                );

        renderInterfaceHorizontalSurface(
                Direction.DOWN,
                upperHorizontalConsumer,
                pose,
                tankPos,
                minX,
                maxX,
                minZ,
                maxZ,
                boundaryY,
                horizontalWaveAmplitude,
                interfaceSeed,
                gameTime,
                partialTick,
                packedOverlay,
                frameMinV,
                frameMaxV
        );

        /*
         * Extremely thin interfaces may deliberately disable the animated side
         * treatment. Their horizontal closure still exists above as a flat,
         * correctly culled pair of faces.
         */
        if (!metrics.active()) {
            return;
        }

        RenderType blendRenderType =
                FoundryMetalBlendRenderType.get(
                        lowerDefinition.animatedTexture(),
                        upperDefinition.animatedTexture()
                );

        if (blendRenderType != null) {
            VertexConsumer blendConsumer =
                    bufferSource.getBuffer(blendRenderType);

            for (Direction side : Direction.Plane.HORIZONTAL) {
                if (structure.contains(tankPos.relative(side))) {
                    continue;
                }

                renderInterfaceShaderBand(
                        side,
                        blendConsumer,
                        pose,
                        tankPos,
                        minX,
                        maxX,
                        minZ,
                        maxZ,
                        boundaryY,
                        metrics,
                        interfaceSeed,
                        gameTime,
                        partialTick,
                        frameMinV,
                        frameMaxV
                );
            }

            return;
        }

        /*
         * Shader unavailable: fall back to the clean one-sided wavy correction
         * that was already proven visually stable.
         *
         * Re-acquire the lower consumer here instead of reusing the horizontal
         * pass consumer, because later getBuffer() calls may already have ended
         * that BufferBuilder.
         */
        VertexConsumer lowerConsumer =
                bufferSource.getBuffer(
                        RenderType.entityTranslucentCull(
                                lowerDefinition.animatedTexture()
                        )
                );

        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (structure.contains(tankPos.relative(side))) {
                continue;
            }

            renderInterfaceCorrection(
                    side,
                    true,
                    lowerConsumer,
                    pose,
                    tankPos,
                    minX,
                    maxX,
                    minZ,
                    maxZ,
                    boundaryY,
                    metrics.waveAmplitude(),
                    interfaceSeed,
                    gameTime,
                    partialTick,
                    packedOverlay,
                    frameMinV,
                    frameMaxV
            );
        }

        /*
         * Same rule for the upper fallback pass: request it only when we are
         * ready to emit its complete set of vertices.
         */
        VertexConsumer upperConsumer =
                bufferSource.getBuffer(
                        RenderType.entityTranslucentCull(
                                upperDefinition.animatedTexture()
                        )
                );

        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (structure.contains(tankPos.relative(side))) {
                continue;
            }

            renderInterfaceCorrection(
                    side,
                    false,
                    upperConsumer,
                    pose,
                    tankPos,
                    minX,
                    maxX,
                    minZ,
                    maxZ,
                    boundaryY,
                    metrics.waveAmplitude(),
                    interfaceSeed,
                    gameTime,
                    partialTick,
                    packedOverlay,
                    frameMinV,
                    frameMaxV
            );
        }
    }

    /**
     * Builds one horizontal side of the internal material boundary.
     *
     * The mesh is subdivided with the same resolution as the side wave. Every
     * vertex samples sampleInterfaceWave() in world space, which gives:
     *
     * - seamless continuation across neighboring Tank cells;
     * - an outer perimeter that lands on the exact same wave used by the side
     *   shader/correction;
     * - no flat internal edge that can show through the glass.
     *
     * UP and DOWN are emitted with opposite winding but the same diagonal for
     * every quad. Their triangles therefore occupy identical geometry while
     * culling makes the appropriate metal visible from each viewing direction.
     */
    private static void renderInterfaceHorizontalSurface(
            Direction face,
            VertexConsumer consumer,
            PoseStack.Pose pose,
            BlockPos tankPos,
            float minX,
            float maxX,
            float minZ,
            float maxZ,
            float boundaryY,
            float waveAmplitude,
            long interfaceSeed,
            long gameTime,
            float partialTick,
            int packedOverlay,
            float frameMinV,
            float frameMaxV
    ) {
        if (face != Direction.UP && face != Direction.DOWN) {
            throw new IllegalArgumentException(
                    "Interface horizontal face must be UP or DOWN."
            );
        }

        for (int xIndex = 0; xIndex < INTERFACE_SEGMENTS_PER_BLOCK; xIndex++) {
            float x0 =
                    Mth.lerp(
                            xIndex / (float) INTERFACE_SEGMENTS_PER_BLOCK,
                            minX,
                            maxX
                    );

            float x1 =
                    Mth.lerp(
                            (xIndex + 1) / (float) INTERFACE_SEGMENTS_PER_BLOCK,
                            minX,
                            maxX
                    );

            for (int zIndex = 0; zIndex < INTERFACE_SEGMENTS_PER_BLOCK; zIndex++) {
                float z0 =
                        Mth.lerp(
                                zIndex / (float) INTERFACE_SEGMENTS_PER_BLOCK,
                                minZ,
                                maxZ
                        );

                float z1 =
                        Mth.lerp(
                                (zIndex + 1) / (float) INTERFACE_SEGMENTS_PER_BLOCK,
                                minZ,
                                maxZ
                        );

                float y00 =
                        boundaryY
                                + sampleInterfaceWave(
                                tankPos.getX() + x0,
                                tankPos.getZ() + z0,
                                interfaceSeed,
                                gameTime,
                                partialTick,
                                waveAmplitude
                        );

                float y01 =
                        boundaryY
                                + sampleInterfaceWave(
                                tankPos.getX() + x0,
                                tankPos.getZ() + z1,
                                interfaceSeed,
                                gameTime,
                                partialTick,
                                waveAmplitude
                        );

                float y11 =
                        boundaryY
                                + sampleInterfaceWave(
                                tankPos.getX() + x1,
                                tankPos.getZ() + z1,
                                interfaceSeed,
                                gameTime,
                                partialTick,
                                waveAmplitude
                        );

                float y10 =
                        boundaryY
                                + sampleInterfaceWave(
                                tankPos.getX() + x1,
                                tankPos.getZ() + z0,
                                interfaceSeed,
                                gameTime,
                                partialTick,
                                waveAmplitude
                        );

                if (face == Direction.UP) {
                    /*
                     * 00 -> 01 -> 11 -> 10 winds toward +Y.
                     */
                    FoundryTankLiquidQuads.renderLiquidQuad(
                            consumer,
                            pose,
                            x0, y00, z0,
                            x0, y01, z1,
                            x1, y11, z1,
                            x1, y10, z0,
                            x0, z0,
                            x0, z1,
                            x1, z1,
                            x1, z0,
                            0.0f, 1.0f, 0.0f,
                            packedOverlay,
                            frameMinV,
                            frameMaxV
                    );
                } else {
                    /*
                     * 00 -> 10 -> 11 -> 01 winds toward -Y and preserves the
                     * same 00-to-11 triangle diagonal as the UP face.
                     */
                    FoundryTankLiquidQuads.renderLiquidQuad(
                            consumer,
                            pose,
                            x0, y00, z0,
                            x1, y10, z0,
                            x1, y11, z1,
                            x0, y01, z1,
                            x0, z0,
                            x1, z0,
                            x1, z1,
                            x0, z1,
                            0.0f, -1.0f, 0.0f,
                            packedOverlay,
                            frameMinV,
                            frameMaxV
                    );
                }
            }
        }
    }

    /**
     * Draws only the part required to move the visible boundary away from its
     * exact flat quantity line.
     *
     * lowerMetal=true keeps positive wave values only.
     * lowerMetal=false keeps negative wave values only.
     */
    private static void renderInterfaceCorrection(
            Direction side,
            boolean lowerMetal,
            VertexConsumer consumer,
            PoseStack.Pose pose,
            BlockPos tankPos,
            float minX,
            float maxX,
            float minZ,
            float maxZ,
            float boundaryY,
            float waveAmplitude,
            long interfaceSeed,
            long gameTime,
            float partialTick,
            int packedOverlay,
            float frameMinV,
            float frameMaxV
    ) {
        float horizontalMin =
                switch (side) {
                    case NORTH, SOUTH -> minX;
                    case WEST, EAST -> minZ;
                    default -> throw new IllegalArgumentException(
                            "Interface side must be horizontal."
                    );
                };

        float horizontalMax =
                switch (side) {
                    case NORTH, SOUTH -> maxX;
                    case WEST, EAST -> maxZ;
                    default -> throw new IllegalArgumentException(
                            "Interface side must be horizontal."
                    );
                };

        for (int segment = 0; segment < INTERFACE_SEGMENTS_PER_BLOCK; segment++) {
            float start =
                    Mth.lerp(
                            segment / (float) INTERFACE_SEGMENTS_PER_BLOCK,
                            horizontalMin,
                            horizontalMax
                    );

            float end =
                    Mth.lerp(
                            (segment + 1) / (float) INTERFACE_SEGMENTS_PER_BLOCK,
                            horizontalMin,
                            horizontalMax
                    );

            double worldXStart;
            double worldZStart;
            double worldXEnd;
            double worldZEnd;

            switch (side) {
                case NORTH -> {
                    worldXStart = tankPos.getX() + start;
                    worldZStart = tankPos.getZ() + minZ;
                    worldXEnd = tankPos.getX() + end;
                    worldZEnd = tankPos.getZ() + minZ;
                }
                case SOUTH -> {
                    worldXStart = tankPos.getX() + start;
                    worldZStart = tankPos.getZ() + maxZ;
                    worldXEnd = tankPos.getX() + end;
                    worldZEnd = tankPos.getZ() + maxZ;
                }
                case WEST -> {
                    worldXStart = tankPos.getX() + minX;
                    worldZStart = tankPos.getZ() + start;
                    worldXEnd = tankPos.getX() + minX;
                    worldZEnd = tankPos.getZ() + end;
                }
                case EAST -> {
                    worldXStart = tankPos.getX() + maxX;
                    worldZStart = tankPos.getZ() + start;
                    worldXEnd = tankPos.getX() + maxX;
                    worldZEnd = tankPos.getZ() + end;
                }
                default -> throw new IllegalArgumentException(
                        "Interface side must be horizontal."
                );
            }

            float waveStart =
                    sampleInterfaceWave(
                            worldXStart,
                            worldZStart,
                            interfaceSeed,
                            gameTime,
                            partialTick,
                            waveAmplitude
                    );

            float waveEnd =
                    sampleInterfaceWave(
                            worldXEnd,
                            worldZEnd,
                            interfaceSeed,
                            gameTime,
                            partialTick,
                            waveAmplitude
                    );

            if (lowerMetal) {
                waveStart = Math.max(0.0f, waveStart);
                waveEnd = Math.max(0.0f, waveEnd);

                if (
                        waveStart <= LIQUID_EPSILON
                                && waveEnd <= LIQUID_EPSILON
                ) {
                    continue;
                }

                renderInterfaceSideQuad(
                        side,
                        consumer,
                        pose,
                        start,
                        end,
                        minX,
                        maxX,
                        minZ,
                        maxZ,
                        boundaryY,
                        boundaryY,
                        boundaryY + waveStart,
                        boundaryY + waveEnd,
                        packedOverlay,
                        frameMinV,
                        frameMaxV
                );
            } else {
                waveStart = Math.min(0.0f, waveStart);
                waveEnd = Math.min(0.0f, waveEnd);

                if (
                        waveStart >= -LIQUID_EPSILON
                                && waveEnd >= -LIQUID_EPSILON
                ) {
                    continue;
                }

                renderInterfaceSideQuad(
                        side,
                        consumer,
                        pose,
                        start,
                        end,
                        minX,
                        maxX,
                        minZ,
                        maxZ,
                        boundaryY + waveStart,
                        boundaryY + waveEnd,
                        boundaryY,
                        boundaryY,
                        packedOverlay,
                        frameMinV,
                        frameMaxV
                );
            }
        }
    }

    /**
     * Renders the one and only shader-owned interface strip.
     *
     * The normal molten side geometry has already been removed from
     * boundary +/- bandHalfExtent, so this strip fills a real hole rather than
     * sitting on top of another quad.
     *
     * It deliberately uses the exact same exterior X/Z plane as the surrounding
     * liquid side geometry. The old outward interface offset belonged to the
     * previous overlay design and would leave a razor-thin depth crack when the
     * interface is viewed from a steep angle.
     *
     * The strip itself is flat at its outer edges; only the blend center moves
     * with the existing world-continuous wave. That lets the shader create a
     * real smooth transition around the wavy contact line.
     */
    private static void renderInterfaceShaderBand(
            Direction side,
            VertexConsumer consumer,
            PoseStack.Pose pose,
            BlockPos tankPos,
            float minX,
            float maxX,
            float minZ,
            float maxZ,
            float boundaryY,
            InterfaceMetrics metrics,
            long interfaceSeed,
            long gameTime,
            float partialTick,
            float frameMinV,
            float frameMaxV
    ) {
        float horizontalMin =
                switch (side) {
                    case NORTH, SOUTH -> minX;
                    case WEST, EAST -> minZ;
                    default -> throw new IllegalArgumentException(
                            "Interface side must be horizontal."
                    );
                };

        float horizontalMax =
                switch (side) {
                    case NORTH, SOUTH -> maxX;
                    case WEST, EAST -> maxZ;
                    default -> throw new IllegalArgumentException(
                            "Interface side must be horizontal."
                    );
                };

        float bottomY =
                boundaryY - metrics.bandHalfExtent();

        float topY =
                boundaryY + metrics.bandHalfExtent();

        for (int segment = 0; segment < INTERFACE_SEGMENTS_PER_BLOCK; segment++) {
            float start =
                    Mth.lerp(
                            segment / (float) INTERFACE_SEGMENTS_PER_BLOCK,
                            horizontalMin,
                            horizontalMax
                    );

            float end =
                    Mth.lerp(
                            (segment + 1) / (float) INTERFACE_SEGMENTS_PER_BLOCK,
                            horizontalMin,
                            horizontalMax
                    );

            double worldXStart;
            double worldZStart;
            double worldXEnd;
            double worldZEnd;

            switch (side) {
                case NORTH -> {
                    worldXStart = tankPos.getX() + start;
                    worldZStart = tankPos.getZ() + minZ;
                    worldXEnd = tankPos.getX() + end;
                    worldZEnd = tankPos.getZ() + minZ;
                }
                case SOUTH -> {
                    worldXStart = tankPos.getX() + start;
                    worldZStart = tankPos.getZ() + maxZ;
                    worldXEnd = tankPos.getX() + end;
                    worldZEnd = tankPos.getZ() + maxZ;
                }
                case WEST -> {
                    worldXStart = tankPos.getX() + minX;
                    worldZStart = tankPos.getZ() + start;
                    worldXEnd = tankPos.getX() + minX;
                    worldZEnd = tankPos.getZ() + end;
                }
                case EAST -> {
                    worldXStart = tankPos.getX() + maxX;
                    worldZStart = tankPos.getZ() + start;
                    worldXEnd = tankPos.getX() + maxX;
                    worldZEnd = tankPos.getZ() + end;
                }
                default -> throw new IllegalArgumentException(
                        "Interface side must be horizontal."
                );
            }

            float centerStart =
                    boundaryY
                            + sampleInterfaceWave(
                            worldXStart,
                            worldZStart,
                            interfaceSeed,
                            gameTime,
                            partialTick,
                            metrics.waveAmplitude()
                    );

            float centerEnd =
                    boundaryY
                            + sampleInterfaceWave(
                            worldXEnd,
                            worldZEnd,
                            interfaceSeed,
                            gameTime,
                            partialTick,
                            metrics.waveAmplitude()
                    );

            float bottomDistanceStart =
                    (bottomY - centerStart)
                            / metrics.blurHalfWidth();

            float bottomDistanceEnd =
                    (bottomY - centerEnd)
                            / metrics.blurHalfWidth();

            float topDistanceStart =
                    (topY - centerStart)
                            / metrics.blurHalfWidth();

            float topDistanceEnd =
                    (topY - centerEnd)
                            / metrics.blurHalfWidth();

            renderInterfaceShaderBandSideQuad(
                    side,
                    consumer,
                    pose,
                    start,
                    end,
                    minX,
                    maxX,
                    minZ,
                    maxZ,
                    bottomY,
                    topY,
                    bottomDistanceStart,
                    bottomDistanceEnd,
                    topDistanceStart,
                    topDistanceEnd,
                    frameMinV,
                    frameMaxV
            );
        }
    }

    private static void renderInterfaceShaderBandSideQuad(
            Direction side,
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float start,
            float end,
            float minX,
            float maxX,
            float minZ,
            float maxZ,
            float bottomY,
            float topY,
            float bottomDistanceStart,
            float bottomDistanceEnd,
            float topDistanceStart,
            float topDistanceEnd,
            float frameMinV,
            float frameMaxV
    ) {
        switch (side) {
            case NORTH -> {
                float z = minZ;

                addShaderBandVertex(
                        consumer, pose,
                        start, topY, z,
                        start, topDistanceStart,
                        0.0f, 0.0f, -1.0f,
                        frameMinV, frameMaxV
                );
                addShaderBandVertex(
                        consumer, pose,
                        end, topY, z,
                        end, topDistanceEnd,
                        0.0f, 0.0f, -1.0f,
                        frameMinV, frameMaxV
                );
                addShaderBandVertex(
                        consumer, pose,
                        end, bottomY, z,
                        end, bottomDistanceEnd,
                        0.0f, 0.0f, -1.0f,
                        frameMinV, frameMaxV
                );
                addShaderBandVertex(
                        consumer, pose,
                        start, bottomY, z,
                        start, bottomDistanceStart,
                        0.0f, 0.0f, -1.0f,
                        frameMinV, frameMaxV
                );
            }

            case SOUTH -> {
                float z = maxZ;

                addShaderBandVertex(
                        consumer, pose,
                        end, topY, z,
                        1.0f - end, topDistanceEnd,
                        0.0f, 0.0f, 1.0f,
                        frameMinV, frameMaxV
                );
                addShaderBandVertex(
                        consumer, pose,
                        start, topY, z,
                        1.0f - start, topDistanceStart,
                        0.0f, 0.0f, 1.0f,
                        frameMinV, frameMaxV
                );
                addShaderBandVertex(
                        consumer, pose,
                        start, bottomY, z,
                        1.0f - start, bottomDistanceStart,
                        0.0f, 0.0f, 1.0f,
                        frameMinV, frameMaxV
                );
                addShaderBandVertex(
                        consumer, pose,
                        end, bottomY, z,
                        1.0f - end, bottomDistanceEnd,
                        0.0f, 0.0f, 1.0f,
                        frameMinV, frameMaxV
                );
            }

            case WEST -> {
                float x = minX;

                addShaderBandVertex(
                        consumer, pose,
                        x, topY, end,
                        1.0f - end, topDistanceEnd,
                        -1.0f, 0.0f, 0.0f,
                        frameMinV, frameMaxV
                );
                addShaderBandVertex(
                        consumer, pose,
                        x, topY, start,
                        1.0f - start, topDistanceStart,
                        -1.0f, 0.0f, 0.0f,
                        frameMinV, frameMaxV
                );
                addShaderBandVertex(
                        consumer, pose,
                        x, bottomY, start,
                        1.0f - start, bottomDistanceStart,
                        -1.0f, 0.0f, 0.0f,
                        frameMinV, frameMaxV
                );
                addShaderBandVertex(
                        consumer, pose,
                        x, bottomY, end,
                        1.0f - end, bottomDistanceEnd,
                        -1.0f, 0.0f, 0.0f,
                        frameMinV, frameMaxV
                );
            }

            case EAST -> {
                float x = maxX;

                addShaderBandVertex(
                        consumer, pose,
                        x, topY, start,
                        start, topDistanceStart,
                        1.0f, 0.0f, 0.0f,
                        frameMinV, frameMaxV
                );
                addShaderBandVertex(
                        consumer, pose,
                        x, topY, end,
                        end, topDistanceEnd,
                        1.0f, 0.0f, 0.0f,
                        frameMinV, frameMaxV
                );
                addShaderBandVertex(
                        consumer, pose,
                        x, bottomY, end,
                        end, bottomDistanceEnd,
                        1.0f, 0.0f, 0.0f,
                        frameMinV, frameMaxV
                );
                addShaderBandVertex(
                        consumer, pose,
                        x, bottomY, start,
                        start, bottomDistanceStart,
                        1.0f, 0.0f, 0.0f,
                        frameMinV, frameMaxV
                );
            }

            default -> {
            }
        }
    }

    /**
     * Stores signed distance to the wavy contact line in UV1.x.
     *
     * -1 means one blur-half-width into the lower metal.
     * +1 means one blur-half-width into the upper metal.
     *
     * Values outside +/-1 are already pure source metal. We encode a wider
     * +/-2 range so the strip edges remain safely saturated.
     */
    private static void addShaderBandVertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x,
            float y,
            float z,
            float u,
            float signedDistance,
            float normalX,
            float normalY,
            float normalZ,
            float frameMinV,
            float frameMaxV
    ) {
        float textureV =
                Mth.lerp(
                        1.0f - y,
                        frameMinV,
                        frameMaxV
                );

        float encoded =
                Mth.clamp(
                        (signedDistance + 2.0f) / 4.0f,
                        0.0f,
                        1.0f
                );

        int distanceCoordinate =
                Mth.clamp(
                        Math.round(encoded * 32767.0f),
                        0,
                        32767
                );

        consumer.addVertex(
                        pose.pose(),
                        x,
                        y,
                        z
                )
                .setColor(0xFFFFFFFF)
                .setUv(
                        u,
                        textureV
                )
                .setUv1(
                        distanceCoordinate,
                        0
                )
                .setLight(
                        LightTexture.FULL_BRIGHT
                )
                .setNormal(
                        pose,
                        normalX,
                        normalY,
                        normalZ
                );
    }

    private static InterfaceTrim getShaderInterfaceTrim(
            BlockPos tankPos,
            FoundryTankRenderedMetalLayer layer,
            Map<BlockPos, List<FoundryTankRenderedMetalLayer>> allLayers
    ) {
        float bottom = 0.0f;
        float top = 0.0f;

        FoundryTankRenderedMetalLayer below =
                getDifferentMetalDirectlyBelow(
                        tankPos,
                        layer,
                        allLayers
                );

        if (below != null) {
            InterfaceMetrics metrics =
                    getInterfaceMetrics(
                            below,
                            layer
                    );

            if (metrics.active()) {
                bottom = metrics.bandHalfExtent();
            }
        }

        FoundryTankRenderedMetalLayer above =
                getDifferentMetalDirectlyAbove(
                        tankPos,
                        layer,
                        allLayers
                );

        if (above != null) {
            InterfaceMetrics metrics =
                    getInterfaceMetrics(
                            layer,
                            above
                    );

            if (metrics.active()) {
                top = metrics.bandHalfExtent();
            }
        }

        return new InterfaceTrim(
                bottom,
                top
        );
    }

    private static InterfaceMetrics getInterfaceMetrics(
            FoundryTankRenderedMetalLayer lower,
            FoundryTankRenderedMetalLayer upper
    ) {
        float lowerThickness =
                Math.max(
                        0.0f,
                        lower.maxY() - lower.minY()
                );

        float upperThickness =
                Math.max(
                        0.0f,
                        upper.maxY() - upper.minY()
                );

        float thinnerLayer =
                Math.min(
                        lowerThickness,
                        upperThickness
                );

        float waveAmplitude =
                Math.min(
                        INTERFACE_MAX_WAVE_AMPLITUDE,
                        thinnerLayer * 0.30f
                );

        if (waveAmplitude < INTERFACE_MIN_WAVE_AMPLITUDE) {
            return InterfaceMetrics.INACTIVE;
        }

        /*
         * For normal-sized layers this reaches 0.35 texture pixels each side,
         * giving about 0.70 pixels of actual crossfade.
         *
         * Very thin layers automatically receive less blur.
         */
        float blurHalfWidth =
                Math.min(
                        INTERFACE_SHADER_BLUR_HALF_WIDTH,
                        thinnerLayer * 0.12f
                );

        if (blurHalfWidth <= LIQUID_EPSILON) {
            return InterfaceMetrics.INACTIVE;
        }

        float bandHalfExtent =
                waveAmplitude + blurHalfWidth;

        return new InterfaceMetrics(
                waveAmplitude,
                blurHalfWidth,
                bandHalfExtent
        );
    }

    private static FoundryTankRenderedMetalLayer getDifferentMetalDirectlyBelow(
            BlockPos tankPos,
            FoundryTankRenderedMetalLayer layer,
            Map<BlockPos, List<FoundryTankRenderedMetalLayer>> allLayers
    ) {
        List<FoundryTankRenderedMetalLayer> localLayers =
                allLayers.getOrDefault(
                        tankPos,
                        List.of()
                );

        for (FoundryTankRenderedMetalLayer candidate : localLayers) {
            if (candidate == layer) {
                continue;
            }

            if (
                    Math.abs(candidate.maxY() - layer.minY())
                            <= LIQUID_EPSILON
                            && !candidate.metal().equals(layer.metal())
            ) {
                return candidate;
            }
        }

        if (layer.minY() > LIQUID_EPSILON) {
            return null;
        }

        List<FoundryTankRenderedMetalLayer> belowLayers =
                allLayers.get(tankPos.below());

        if (belowLayers == null || belowLayers.isEmpty()) {
            return null;
        }

        FoundryTankRenderedMetalLayer below =
                belowLayers.getLast();

        if (
                below.maxY() >= 1.0f - LIQUID_EPSILON
                        && !below.metal().equals(layer.metal())
        ) {
            return below;
        }

        return null;
    }

    private static FoundryTankRenderedMetalLayer getDifferentMetalDirectlyAbove(
            BlockPos tankPos,
            FoundryTankRenderedMetalLayer layer,
            Map<BlockPos, List<FoundryTankRenderedMetalLayer>> allLayers
    ) {
        List<FoundryTankRenderedMetalLayer> localLayers =
                allLayers.getOrDefault(
                        tankPos,
                        List.of()
                );

        for (FoundryTankRenderedMetalLayer candidate : localLayers) {
            if (candidate == layer) {
                continue;
            }

            if (
                    Math.abs(candidate.minY() - layer.maxY())
                            <= LIQUID_EPSILON
                            && !candidate.metal().equals(layer.metal())
            ) {
                return candidate;
            }
        }

        if (layer.maxY() < 1.0f - LIQUID_EPSILON) {
            return null;
        }

        List<FoundryTankRenderedMetalLayer> aboveLayers =
                allLayers.get(tankPos.above());

        if (aboveLayers == null || aboveLayers.isEmpty()) {
            return null;
        }

        FoundryTankRenderedMetalLayer above =
                aboveLayers.getFirst();

        if (
                above.minY() <= LIQUID_EPSILON
                        && !above.metal().equals(layer.metal())
        ) {
            return above;
        }

        return null;
    }

    private record InterfaceTrim(
            float bottom,
            float top
    ) {
    }

    private record InterfaceMetrics(
            float waveAmplitude,
            float blurHalfWidth,
            float bandHalfExtent
    ) {
        private static final InterfaceMetrics INACTIVE =
                new InterfaceMetrics(
                        0.0f,
                        0.0f,
                        0.0f
                );

        boolean active() {
            return bandHalfExtent > LIQUID_EPSILON;
        }
    }

    private static void renderInterfaceSideQuad(
            Direction side,
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float start,
            float end,
            float minX,
            float maxX,
            float minZ,
            float maxZ,
            float bottomStart,
            float bottomEnd,
            float topStart,
            float topEnd,
            int packedOverlay,
            float frameMinV,
            float frameMaxV
    ) {
        switch (side) {
            case NORTH -> {
                float z = minZ;

                FoundryTankLiquidQuads.renderLiquidQuad(
                        consumer,
                        pose,
                        start,
                        topStart,
                        z,
                        end,
                        topEnd,
                        z,
                        end,
                        bottomEnd,
                        z,
                        start,
                        bottomStart,
                        z,
                        start,
                        1.0f - topStart,
                        end,
                        1.0f - topEnd,
                        end,
                        1.0f - bottomEnd,
                        start,
                        1.0f - bottomStart,
                        0.0f,
                        0.0f,
                        -1.0f,
                        packedOverlay,
                        frameMinV,
                        frameMaxV
                );
            }

            case SOUTH -> {
                float z = maxZ;

                FoundryTankLiquidQuads.renderLiquidQuad(
                        consumer,
                        pose,
                        end,
                        topEnd,
                        z,
                        start,
                        topStart,
                        z,
                        start,
                        bottomStart,
                        z,
                        end,
                        bottomEnd,
                        z,
                        1.0f - end,
                        1.0f - topEnd,
                        1.0f - start,
                        1.0f - topStart,
                        1.0f - start,
                        1.0f - bottomStart,
                        1.0f - end,
                        1.0f - bottomEnd,
                        0.0f,
                        0.0f,
                        1.0f,
                        packedOverlay,
                        frameMinV,
                        frameMaxV
                );
            }

            case WEST -> {
                float x = minX;

                FoundryTankLiquidQuads.renderLiquidQuad(
                        consumer,
                        pose,
                        x,
                        topEnd,
                        end,
                        x,
                        topStart,
                        start,
                        x,
                        bottomStart,
                        start,
                        x,
                        bottomEnd,
                        end,
                        1.0f - end,
                        1.0f - topEnd,
                        1.0f - start,
                        1.0f - topStart,
                        1.0f - start,
                        1.0f - bottomStart,
                        1.0f - end,
                        1.0f - bottomEnd,
                        -1.0f,
                        0.0f,
                        0.0f,
                        packedOverlay,
                        frameMinV,
                        frameMaxV
                );
            }

            case EAST -> {
                float x = maxX;

                FoundryTankLiquidQuads.renderLiquidQuad(
                        consumer,
                        pose,
                        x,
                        topStart,
                        start,
                        x,
                        topEnd,
                        end,
                        x,
                        bottomEnd,
                        end,
                        x,
                        bottomStart,
                        start,
                        start,
                        1.0f - topStart,
                        end,
                        1.0f - topEnd,
                        end,
                        1.0f - bottomEnd,
                        start,
                        1.0f - bottomStart,
                        1.0f,
                        0.0f,
                        0.0f,
                        packedOverlay,
                        frameMinV,
                        frameMaxV
                );
            }

            default -> {
            }
        }
    }

    private static float sampleInterfaceWave(
            double worldX,
            double worldZ,
            long interfaceSeed,
            long gameTime,
            float partialTick,
            float amplitude
    ) {
        double seconds =
                (gameTime + partialTick) / 20.0D;

        double primaryPhase =
                unit(
                        mix64(
                                interfaceSeed
                                        ^ 0x9E3779B97F4A7C15L
                        )
                )
                        * Math.PI
                        * 2.0D;

        double secondaryPhase =
                unit(
                        mix64(
                                interfaceSeed
                                        ^ 0xC2B2AE3D27D4EB4FL
                        )
                )
                        * Math.PI
                        * 2.0D;

        double primary =
                Math.sin(
                        (
                                worldX * 0.82D
                                        + worldZ * 0.57D
                        )
                                * (Math.PI * 2.0D / INTERFACE_PRIMARY_WAVELENGTH)
                                + primaryPhase
                                + seconds * INTERFACE_PRIMARY_SPEED
                );

        double secondary =
                Math.sin(
                        (
                                worldX * -0.46D
                                        + worldZ * 0.89D
                        )
                                * (Math.PI * 2.0D / INTERFACE_SECONDARY_WAVELENGTH)
                                + secondaryPhase
                                + seconds * INTERFACE_SECONDARY_SPEED
                );

        return (float) (
                amplitude
                        * (
                        primary * 0.72D
                                + secondary * 0.28D
                )
        );
    }

    private static long createInterfaceSeed(
            BlockPos controllerPos,
            ResourceLocation lowerMetal,
            ResourceLocation upperMetal
    ) {
        long lowerSalt =
                lowerMetal.toString().hashCode();

        long upperSalt =
                upperMetal.toString().hashCode();

        return mix64(
                controllerPos.asLong()
                        ^ lowerSalt * 0x9E3779B97F4A7C15L
                        ^ upperSalt * 0xC2B2AE3D27D4EB4FL
        );
    }

    /**
     * Returns true when this layer ends exactly where another, different molten
     * metal begins. In that case the visible boundary is provided by the
     * interface system rather than by a flat horizontal quad.
     */
    private static boolean hasDifferentMetalDirectlyAbove(
            BlockPos tankPos,
            FoundryTankRenderedMetalLayer layer,
            List<FoundryTankRenderedMetalLayer> localLayers,
            Map<BlockPos, List<FoundryTankRenderedMetalLayer>> allLayers
    ) {
        return getDifferentMetalDirectlyAbove(
                tankPos,
                layer,
                allLayers
        ) != null;
    }

    private static List<FoundryTankVerticalInterval> subtractInterval(
            List<FoundryTankVerticalInterval> source,
            FoundryTankVerticalInterval removed
    ) {
        List<FoundryTankVerticalInterval> result = new ArrayList<>();

        for (FoundryTankVerticalInterval interval : source) {
            float overlapMin = Math.max(interval.minY(), removed.minY());
            float overlapMax = Math.min(interval.maxY(), removed.maxY());

            if (overlapMax - overlapMin <= LIQUID_EPSILON) {
                result.add(interval);
                continue;
            }

            if (overlapMin - interval.minY() > LIQUID_EPSILON) {
                result.add(
                        new FoundryTankVerticalInterval(
                                interval.minY(),
                                overlapMin
                        )
                );
            }

            if (interval.maxY() - overlapMax > LIQUID_EPSILON) {
                result.add(
                        new FoundryTankVerticalInterval(
                                overlapMax,
                                interval.maxY()
                        )
                );
            }
        }

        return result;
    }

    private static ResourceLocation getNextDisplayedMetal(
            Map<ResourceLocation, Float> displayedAmounts,
            List<MoltenMetalDefinition> ordered,
            int startIndex
    ) {
        for (int index = startIndex; index < ordered.size(); index++) {
            MoltenMetalDefinition definition = ordered.get(index);
            if (displayedAmounts.getOrDefault(definition.id(), 0.0f) > LIQUID_EPSILON) {
                return definition.id();
            }
        }
        return null;
    }

    private static ResourceLocation getBottomDisplayedMetal(
            Map<ResourceLocation, Float> displayedAmounts
    ) {
        for (MoltenMetalDefinition definition : ModMoltenMetals.heaviestFirst()) {
            if (displayedAmounts.getOrDefault(definition.id(), 0.0f) > LIQUID_EPSILON) {
                return definition.id();
            }
        }
        return null;
    }

    private static boolean allLayersEmpty(
            List<FoundryTankRenderedMetalLayer> layers
    ) {
        return layers == null || layers.isEmpty();
    }

    private static float sum(
            Map<ResourceLocation, Float> amounts
    ) {
        float total = 0.0f;
        for (Float amount : amounts.values()) {
            if (amount != null && amount > 0.0f) {
                total += amount;
            }
        }
        return total;
    }

    private static float unit(
            long value
    ) {
        return (float) ((value >>> 40) & 0xFFFFFFL)
                / (float) 0x1000000;
    }

    private static long mix64(
            long value
    ) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return value;
    }
}
