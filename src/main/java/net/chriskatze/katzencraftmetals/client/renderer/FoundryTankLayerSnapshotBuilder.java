package net.chriskatze.katzencraftmetals.client.renderer;

import net.chriskatze.katzencraftmetals.block.entity.FoundryTankBlockEntity;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTankNetwork;
import net.chriskatze.katzencraftmetals.metal.FoundryMetalLayer;
import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.chriskatze.katzencraftmetals.metal.MoltenMetalDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class FoundryTankLayerSnapshotBuilder {

    private static final float LIQUID_EPSILON =
            0.0001f;

    private FoundryTankLayerSnapshotBuilder() {
    }

    static FoundryTankHorizontalLayerSnapshot create(
            FoundryTankBlockEntity tank
    ) {
        Level level =
                tank.getLevel();

        FoundryTankNetwork network =
                tank.getNetwork();

        Set<BlockPos> networkPositions =
                network != null
                        ? network.getTankPositions()
                        : Set.of(
                        tank.getBlockPos()
                );

        /*
         * Deliberately avoid stream pipelines here.
         *
         * This code runs inside the block entity renderer every frame, and we
         * already had a crash from this exact area caused by a reused/closed
         * stream. A small defensive loop is safer and easier to debug.
         */
        List<BlockPos> horizontalPositions =
                new ArrayList<>();

        for (BlockPos tankPos : networkPositions) {
            if (tankPos.getY() == tank.getBlockPos().getY()) {
                horizontalPositions.add(
                        tankPos.immutable()
                );
            }
        }

        horizontalPositions.sort(
                Comparator
                        .comparingInt(
                                (BlockPos tankPos) ->
                                        tankPos.getX()
                        )
                        .thenComparingInt(
                                tankPos ->
                                        tankPos.getZ()
                        )
        );

        if (horizontalPositions.isEmpty()) {
            horizontalPositions =
                    List.of(
                            tank.getBlockPos()
                                    .immutable()
                    );
        }

        Map<ResourceLocation, Integer> aggregateAmounts =
                new LinkedHashMap<>();

        for (BlockPos tankPos : horizontalPositions) {
            if (
                    level.getBlockEntity(
                            tankPos
                    )
                            instanceof FoundryTankBlockEntity layerTank
            ) {
                for (
                        FoundryMetalLayer layer :
                        layerTank.getLocalMetalLayers()
                ) {
                    if (
                            layer.amount() > 0
                                    && ModMoltenMetals.contains(
                                    layer.metal()
                            )
                    ) {
                        aggregateAmounts.merge(
                                layer.metal(),
                                layer.amount(),
                                Integer::sum
                        );
                    }
                }
            }
        }

        int tankCount =
                horizontalPositions.size();

        float remainingCapacity =
                FoundryTankBlockEntity.CAPACITY;

        Map<ResourceLocation, Float> targetAmounts =
                new LinkedHashMap<>();

        for (
                MoltenMetalDefinition definition :
                ModMoltenMetals.heaviestFirst()
        ) {
            int aggregateAmount =
                    aggregateAmounts.getOrDefault(
                            definition.id(),
                            0
                    );

            if (
                    aggregateAmount <= 0
                            || remainingCapacity <= LIQUID_EPSILON
            ) {
                continue;
            }

            float averageAmount =
                    Math.min(
                            remainingCapacity,
                            (float) aggregateAmount
                                    / tankCount
                    );

            if (averageAmount > LIQUID_EPSILON) {
                targetAmounts.put(
                        definition.id(),
                        averageAmount
                );

                remainingCapacity -=
                        averageAmount;
            }
        }

        BlockPos anchor =
                horizontalPositions.getFirst()
                        .immutable();

        return new FoundryTankHorizontalLayerSnapshot(
                new FoundryTankHorizontalLayerKey(
                        level,
                        anchor,
                        tank.getBlockPos().getY()
                ),
                Map.copyOf(
                        targetAmounts
                )
        );
    }
}
