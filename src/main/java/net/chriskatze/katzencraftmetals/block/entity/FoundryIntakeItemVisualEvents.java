package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display.ItemDisplay;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Cosmetic-only falling item visuals for the Foundry Tank intake hatch.
 *
 * Gameplay items are inserted into the Controller immediately. These temporary
 * ItemDisplay entities are display-only visuals: no item pickup, no item
 * physics, no collision, just a simple smooth fall into the molten surface.
 */
@EventBusSubscriber(modid = KatzencraftMetalsMod.MODID)
public final class FoundryIntakeItemVisualEvents {

    private static final String VISUAL_TAG =
            KatzencraftMetalsMod.MODID + ".fake_intake_item";

    /*
     * Spawn just inside the tank, slightly below the top hatch.
     * This avoids spawning inside a hopper that may be sitting above the hatch.
     */
    private static final double SPAWN_Y_OFFSET =
            0.82D;

    /*
     * Visual-only manual fall speed in blocks per tick.
     */
    private static final double FALL_SPEED =
            0.3D;

    /*
     * ItemDisplay with item_display="ground" already looks much closer to a
     * dropped item. Use this only as a final multiplier.
     */
    private static final float VISUAL_ITEM_SCALE =
            1.0F;

    private static final double SURFACE_HIT_PADDING =
            0.045D;

    private static final double IMPACT_PARTICLE_Y_OFFSET =
            0.08D;

    private static final double TANK_INTERIOR_MIN_Y_OFFSET =
            0.075D;

    private static final double TANK_INTERIOR_MAX_Y_OFFSET =
            0.925D;

    private static final int MAX_LIFETIME_TICKS =
            100;

    private static final int MAX_VISUALS_PER_INSERT =
            2;

    private static final Map<UUID, VisualState> VISUAL_STATES =
            new HashMap<>();

    private FoundryIntakeItemVisualEvents() {
    }

    static void spawnVisuals(
            Level level,
            BlockPos hatchPos,
            FoundryControllerBlockEntity controller,
            ItemStack sourceStack,
            int insertedCount
    ) {
        if (
                !(level instanceof ServerLevel serverLevel)
                        || sourceStack.isEmpty()
                        || insertedCount <= 0
        ) {
            return;
        }

        int visualCount =
                Math.min(
                        MAX_VISUALS_PER_INSERT,
                        Math.max(
                                1,
                                (insertedCount + 15) / 16
                        )
                );

        double targetY =
                calculateColumnAwareImpactY(
                        hatchPos,
                        controller
                );

        for (int index = 0; index < visualCount; index++) {
            ItemStack displayStack =
                    sourceStack.copy();

            displayStack.setCount(
                    1
            );

            double offsetX =
                    (
                            serverLevel.random.nextDouble()
                                    - 0.5D
                    ) * 0.18D;

            double offsetZ =
                    (
                            serverLevel.random.nextDouble()
                                    - 0.5D
                    ) * 0.18D;

            ItemDisplay visualItem =
                    new ItemDisplay(
                            EntityType.ITEM_DISPLAY,
                            serverLevel
                    );

            setDisplayDataFromNbt(
                    serverLevel,
                    visualItem,
                    displayStack
            );

            visualItem.addTag(
                    VISUAL_TAG
            );

            visualItem.setNoGravity(
                    true
            );

            visualItem.noPhysics =
                    true;

            visualItem.setPos(
                    hatchPos.getX()
                            + 0.5D
                            + offsetX,
                    hatchPos.getY()
                            + SPAWN_Y_OFFSET
                            + index * 0.035D,
                    hatchPos.getZ()
                            + 0.5D
                            + offsetZ
            );

            visualItem.setYRot(
                    serverLevel.random.nextFloat()
                            * 360.0F
            );

            serverLevel.addFreshEntity(
                    visualItem
            );

            VISUAL_STATES.put(
                    visualItem.getUUID(),
                    new VisualState(
                            targetY,
                            index * 2
                    )
            );
        }
    }

    private static void setDisplayDataFromNbt(
            ServerLevel level,
            ItemDisplay visualItem,
            ItemStack displayStack
    ) {
        CompoundTag displayTag =
                new CompoundTag();

        displayTag.put(
                "item",
                displayStack.save(
                        level.registryAccess()
                )
        );

        /*
         * This is the important part for size:
         * "ground" uses the normal dropped-item presentation instead of the
         * huge default display transform.
         */
        displayTag.putString(
                "item_display",
                "ground"
        );

        /*
         * Smooth one-tick position updates instead of hard client-side snapping.
         */
        displayTag.putInt(
                "teleport_duration",
                1
        );

        CompoundTag transformationTag =
                new CompoundTag();

        transformationTag.put(
                "scale",
                floatList(
                        VISUAL_ITEM_SCALE,
                        VISUAL_ITEM_SCALE,
                        VISUAL_ITEM_SCALE
                )
        );

        displayTag.put(
                "transformation",
                transformationTag
        );

        visualItem.load(
                displayTag
        );
    }

    private static ListTag floatList(
            float... values
    ) {
        ListTag listTag =
                new ListTag();

        for (float value : values) {
            listTag.add(
                    FloatTag.valueOf(
                            value
                    )
            );
        }

        return listTag;
    }

    @SubscribeEvent
    public static void onEntityTickPost(
            EntityTickEvent.Post event
    ) {
        Entity entity =
                event.getEntity();

        if (!(entity instanceof ItemDisplay itemDisplay)) {
            return;
        }

        if (
                itemDisplay.level() == null
                        || itemDisplay.level()
                        .isClientSide()
        ) {
            return;
        }

        if (!itemDisplay.getTags()
                .contains(
                        VISUAL_TAG
                )) {
            return;
        }

        VisualState state =
                VISUAL_STATES.get(
                        itemDisplay.getUUID()
                );

        if (state == null) {
            itemDisplay.discard();
            return;
        }

        if (!itemDisplay.isAlive()) {
            VISUAL_STATES.remove(
                    itemDisplay.getUUID()
            );
            return;
        }

        state.age++;

        itemDisplay.setNoGravity(
                true
        );

        itemDisplay.noPhysics =
                true;

        if (state.delay > 0) {
            state.delay--;
            return;
        }

        double nextY =
                itemDisplay.getY()
                        - FALL_SPEED;

        itemDisplay.setPos(
                itemDisplay.getX(),
                nextY,
                itemDisplay.getZ()
        );

        itemDisplay.setYRot(
                itemDisplay.getYRot()
                        + 7.0F
        );

        if (
                nextY <= state.targetSurfaceY
                        + SURFACE_HIT_PADDING
                        || state.age >= MAX_LIFETIME_TICKS
        ) {
            burnUp(
                    itemDisplay,
                    state.targetSurfaceY
            );

            VISUAL_STATES.remove(
                    itemDisplay.getUUID()
            );

            itemDisplay.discard();
        }
    }

    private static double calculateColumnAwareImpactY(
            BlockPos hatchPos,
            FoundryControllerBlockEntity controller
    ) {
        FoundryTankNetwork network =
                controller.getOwnedTankNetwork();

        if (network == null) {
            return hatchPos.getY()
                    + 0.12D;
        }

        Set<BlockPos> tankPositions =
                network.getTankPositions();

        if (tankPositions.isEmpty()) {
            return hatchPos.getY()
                    + 0.12D;
        }

        ColumnBounds hatchColumn =
                findHatchColumnBounds(
                        hatchPos,
                        tankPositions
                );

        double columnBottomY =
                hatchColumn.minY()
                        + TANK_INTERIOR_MIN_Y_OFFSET;

        double columnTopY =
                hatchColumn.maxY()
                        + TANK_INTERIOR_MAX_Y_OFFSET;

        double moltenSurfaceY =
                calculateNetworkMoltenSurfaceY(
                        network,
                        hatchPos
                );

        /*
         * With overhangs, the global molten surface can be below this intake
         * hatch's physical vertical column.
         *
         * Example:
         *
         * Layer 2: [H][T][T]
         * Layer 1:    [T]
         *
         * If the liquid is still only in the bottom center Tank, an item dropped
         * into the outer overhang must not visually fall down to that distant
         * liquid surface. It hits the bottom of the overhang Tank instead.
         *
         * If this hatch has a real vertical stack below it, columnBottomY is the
         * bottom of that stack, so the item can still fall through the stack
         * until it hits either molten metal or the physical bottom.
         */
        return Mth.clamp(
                Math.max(
                        moltenSurfaceY,
                        columnBottomY
                ),
                columnBottomY,
                columnTopY
        );
    }

    private static ColumnBounds findHatchColumnBounds(
            BlockPos hatchPos,
            Set<BlockPos> tankPositions
    ) {
        int minY =
                hatchPos.getY();

        int maxY =
                hatchPos.getY();

        int x =
                hatchPos.getX();

        int z =
                hatchPos.getZ();

        while (
                tankPositions.contains(
                        new BlockPos(
                                x,
                                minY - 1,
                                z
                        )
                )
        ) {
            minY--;
        }

        while (
                tankPositions.contains(
                        new BlockPos(
                                x,
                                maxY + 1,
                                z
                        )
                )
        ) {
            maxY++;
        }

        return new ColumnBounds(
                minY,
                maxY
        );
    }

    private static double calculateNetworkMoltenSurfaceY(
            FoundryTankNetwork network,
            BlockPos fallbackPos
    ) {
        List<BlockPos> tankPositions =
                new ArrayList<>(
                        network.getTankPositions()
                );

        if (tankPositions.isEmpty()) {
            return fallbackPos.getY()
                    + 0.12D;
        }

        tankPositions.sort(
                Comparator
                        .comparingInt(
                                (BlockPos position) ->
                                        position.getY()
                        )
                        .thenComparingInt(
                                position ->
                                        position.getX()
                        )
                        .thenComparingInt(
                                position ->
                                        position.getZ()
                        )
        );

        Map<Integer, Integer> tankCountsByY =
                new HashMap<>();

        for (BlockPos tankPos : tankPositions) {
            tankCountsByY.merge(
                    tankPos.getY(),
                    1,
                    Integer::sum
            );
        }

        List<Integer> yLevels =
                new ArrayList<>(
                        tankCountsByY.keySet()
                );

        yLevels.sort(
                Integer::compareTo
        );

        int remainingAmount =
                Math.max(
                        0,
                        network.getTotalMoltenAmount()
                );

        int lowestY =
                yLevels.getFirst();

        for (Integer y : yLevels) {
            int tanksAtY =
                    Math.max(
                            1,
                            tankCountsByY.getOrDefault(
                                    y,
                                    1
                            )
                    );

            int levelCapacity =
                    tanksAtY
                            * FoundryTankNetwork.TANK_CAPACITY;

            if (remainingAmount <= levelCapacity) {
                double fill =
                        levelCapacity <= 0
                                ? 0.0D
                                : (double) remainingAmount
                                / (double) levelCapacity;

                return y
                        + TANK_INTERIOR_MIN_Y_OFFSET
                        + Mth.clamp(
                        fill,
                        0.0D,
                        1.0D
                ) * (
                        TANK_INTERIOR_MAX_Y_OFFSET
                                - TANK_INTERIOR_MIN_Y_OFFSET
                );
            }

            remainingAmount -=
                    levelCapacity;
        }

        int highestY =
                yLevels.getLast();

        return Math.max(
                lowestY + TANK_INTERIOR_MIN_Y_OFFSET,
                highestY + TANK_INTERIOR_MAX_Y_OFFSET
        );
    }

    private static void burnUp(
            Entity entity,
            double surfaceY
    ) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        double x =
                entity.getX();

        /*
         * Always spawn the impact burst slightly above the calculated impact
         * surface. The falling display can overshoot the target by FALL_SPEED;
         * using the entity Y would place particles down inside a tank block,
         * making the burst look trapped by that block.
         */
        double y =
                surfaceY
                        + IMPACT_PARTICLE_Y_OFFSET;

        double z =
                entity.getZ();

        serverLevel.sendParticles(
                ParticleTypes.SMOKE,
                x,
                y,
                z,
                4,
                0.14D,
                0.035D,
                0.14D,
                0.006D
        );

        /*
         * Use flame instead of lava droplets here. Lava particles have their
         * own heavy droplet-style movement and tend to look collision-bound
         * inside the tank. Flame/smoke gives the same quick burn-up read while
         * behaving much more like the old non-colliding tank fire effect.
         */
        serverLevel.sendParticles(
                ParticleTypes.FLAME,
                x,
                y + 0.02D,
                z,
                7,
                0.20D,
                0.05D,
                0.20D,
                0.015D
        );

        serverLevel.playSound(
                null,
                BlockPos.containing(
                        x,
                        y,
                        z
                ),
                SoundEvents.FIRE_EXTINGUISH,
                SoundSource.BLOCKS,
                0.18F,
                1.65F
                        + serverLevel.random.nextFloat()
                        * 0.25F
        );
    }

    private record ColumnBounds(
            int minY,
            int maxY
    ) {
    }

    private static final class VisualState {

        private final double targetSurfaceY;

        private int delay;

        private int age;

        private VisualState(
                double targetSurfaceY,
                int delay
        ) {
            this.targetSurfaceY =
                    targetSurfaceY;

            this.delay =
                    delay;
        }
    }
}
