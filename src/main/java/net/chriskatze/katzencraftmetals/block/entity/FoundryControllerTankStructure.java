package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Event-driven physical vessel cache owned by one Foundry Controller.
 *
 * Final invariant:
 * - a face-connected physical Tank component is atomic
 * - normal placement never allows that component to exceed 4 x 4 x 4
 * - a Controller either owns that complete component or none of it
 *
 * This deliberately removes the old "pick the best 4 x 4 subset out of a
 * larger connected mass" behavior. That behavior was the source of visual /
 * gameplay disagreement and made multi-Controller ownership unnecessarily
 * ambiguous.
 */
final class FoundryControllerTankStructure {

    private static final String ROOT_TAG = "ControllerTankStructure";
    private static final String POSITIONS_TAG = "TankPositions";
    private static final String REVISION_TAG = "Revision";

    private static final Comparator<BlockPos> POSITION_ORDER =
            Comparator
                    .comparingInt((BlockPos pos) -> pos.getY())
                    .thenComparingInt(BlockPos::getX)
                    .thenComparingInt(BlockPos::getZ);

    private final FoundryControllerBlockEntity controller;

    private Set<BlockPos> tankPositions = Set.of();

    @Nullable
    private FoundryTankNetwork cachedNetwork;

    private int minY;
    private long revision;
    private boolean dirty = true;

    FoundryControllerTankStructure(
            FoundryControllerBlockEntity controller
    ) {
        this.controller = controller;
    }

    void markDirty() {
        dirty = true;
    }

    long getRevision() {
        return revision;
    }

    Set<BlockPos> getTankPositions() {
        ensureResolved();
        return tankPositions;
    }

    /** Returns only the last resolved/synchronized snapshot. Never discovers. */
    Set<BlockPos> peekTankPositions() {
        return tankPositions;
    }

    int getMinY() {
        ensureResolved();
        return minY;
    }

    @Nullable
    FoundryTankNetwork getNetwork() {
        ensureResolved();
        return cachedNetwork;
    }

    boolean contains(
            BlockPos tankPos
    ) {
        return tankPos != null
                && getTankPositions().contains(tankPos);
    }

    private void rebuildCachedNetwork() {
        cachedNetwork = tankPositions.isEmpty()
                ? null
                : new FoundryTankNetwork(
                controller,
                tankPositions,
                minY,
                revision
        );
    }

    void clearForControllerRemoval() {
        if (tankPositions.isEmpty() && !dirty) {
            return;
        }

        tankPositions = Set.of();
        minY = 0;
        dirty = false;
        revision++;
        cachedNetwork = null;
    }

    /**
     * The only server-side structure resolver.
     *
     * No save method and no render path is allowed to call this implicitly.
     * Tank onPlace/onRemove merely marks dirty; the Controller's next server
     * tick observes the final physical world state and resolves exactly once.
     */
    private void ensureResolved() {
        Level level = controller.getLevel();

        if (level == null) {
            return;
        }

        /* The client consumes the Controller snapshot sent by the server. */
        if (level.isClientSide()) {
            return;
        }

        if (!dirty) {
            return;
        }

        Set<BlockPos> oldPositions = tankPositions;
        Set<BlockPos> discovered = discoverBestPhysicalComponent(level);

        FoundryTankStructureValidation.ValidationResult validation =
                FoundryTankStructureValidation.validate(discovered);

        if (!validation.valid()) {
            discovered = Set.of();
            minY = 0;
        } else {
            minY = validation.minY();
        }

        tankPositions = Set.copyOf(discovered);
        dirty = false;

        boolean structureChanged =
                !oldPositions.equals(tankPositions);

        if (structureChanged) {
            revision++;
        }

        rebuildCachedNetwork();

        if (!structureChanged) {
            return;
        }

        controller.getTankStorage()
                .onTankStructureChanged(
                        oldPositions,
                        tankPositions
                );

        /* One final Controller packet after structure + storage agree. */
        controller.syncToClient();

        Set<BlockPos> changedPositions = new HashSet<>(oldPositions);
        changedPositions.addAll(tankPositions);

        for (BlockPos changedPos : changedPositions) {
            markNearbyControllersDirty(
                    level,
                    changedPos,
                    controller
            );
        }
    }

    /**
     * Finds complete physical components touching this Controller and chooses
     * one valid atomic vessel. There is no rectangular subset search anymore.
     */
    private Set<BlockPos> discoverBestPhysicalComponent(
            Level level
    ) {
        Candidate best = Candidate.empty();
        Set<BlockPos> alreadyChecked = new HashSet<>();

        for (BlockPos attachmentPos : controller.getValidTankAttachmentPositions()) {
            if (!isTank(level, attachmentPos)) {
                continue;
            }

            if (alreadyChecked.contains(attachmentPos)) {
                continue;
            }

            Set<BlockPos> component =
                    collectPhysicalComponent(
                            level,
                            attachmentPos
                    );

            alreadyChecked.addAll(component);

            FoundryTankStructureValidation.ValidationResult validation =
                    FoundryTankStructureValidation.validate(component);

            if (
                    !validation.valid()
                            || !controller.canOwnTankLayout(component)
                            || !isPreferredControllerForComponent(
                            level,
                            component
                    )
            ) {
                continue;
            }

            Candidate candidate =
                    Candidate.of(
                            component,
                            validation.minY()
                    );

            if (candidate.isBetterThan(best)) {
                best = candidate;
            }
        }

        return best.positions();
    }

    /**
     * Bounded BFS over ordinary Tank blocks.
     *
     * We stop as soon as the component is known to exceed the hard maximum;
     * validation will reject the returned sentinel-sized set. Normal placement
     * prevents such a component from being created in the first place.
     */
    private static Set<BlockPos> collectPhysicalComponent(
            Level level,
            BlockPos startPos
    ) {
        Set<BlockPos> connected = new LinkedHashSet<>();

        if (!isTank(level, startPos)) {
            return connected;
        }

        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        BlockPos start = startPos.immutable();

        connected.add(start);
        queue.addLast(start);

        while (!queue.isEmpty()) {
            BlockPos current = queue.removeFirst();

            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);

                if (connected.contains(next) || !isTank(level, next)) {
                    continue;
                }

                BlockPos immutable = next.immutable();
                connected.add(immutable);

                if (connected.size() > FoundryTankNetwork.MAX_TANK_COUNT) {
                    return connected;
                }

                queue.addLast(immutable);
            }
        }

        return connected;
    }

    /**
     * Exactly one Controller may own one complete physical component.
     *
     * Existing ownership wins over a newly placed competing Controller. If two
     * cached incumbents exist (e.g. recovery from an old/bad save), block-position
     * order gives a deterministic winner independent of tick order.
     */
    private boolean isPreferredControllerForComponent(
            Level level,
            Set<BlockPos> component
    ) {
        boolean currentIncumbent =
                intersects(
                        peekTankPositions(),
                        component
                );

        BlockPos currentPos = controller.getBlockPos();
        Set<BlockPos> checkedControllerPositions = new HashSet<>();

        for (BlockPos tankPos : component) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos possibleControllerPos = tankPos.relative(direction);

                if (!checkedControllerPositions.add(possibleControllerPos)) {
                    continue;
                }

                BlockEntity blockEntity =
                        level.getBlockEntity(possibleControllerPos);

                if (
                        !(blockEntity instanceof FoundryControllerBlockEntity other)
                                || other == controller
                                || !other.canOwnTankLayout(component)
                ) {
                    continue;
                }

                boolean otherIncumbent =
                        intersects(
                                other.getTankStructure()
                                        .peekTankPositions(),
                                component
                        );

                if (currentIncumbent != otherIncumbent) {
                    if (otherIncumbent) {
                        return false;
                    }

                    continue;
                }

                if (
                        POSITION_ORDER.compare(
                                other.getBlockPos(),
                                currentPos
                        ) < 0
                ) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean intersects(
            Set<BlockPos> first,
            Set<BlockPos> second
    ) {
        if (
                first == null
                        || second == null
                        || first.isEmpty()
                        || second.isEmpty()
        ) {
            return false;
        }

        Set<BlockPos> smaller =
                first.size() <= second.size()
                        ? first
                        : second;

        Set<BlockPos> larger =
                smaller == first
                        ? second
                        : first;

        for (BlockPos pos : smaller) {
            if (larger.contains(pos)) {
                return true;
            }
        }

        return false;
    }

    private static int countColumns(
            Set<BlockPos> positions
    ) {
        Set<Long> columns = new HashSet<>();

        for (BlockPos pos : positions) {
            columns.add(
                    (((long) pos.getX()) << 32)
                            ^ (pos.getZ() & 0xFFFFFFFFL)
            );
        }

        return columns.size();
    }

    static boolean isTank(
            Level level,
            BlockPos pos
    ) {
        return level != null
                && pos != null
                && level.getBlockState(pos)
                .is(ModBlocks.FOUNDRY_TANK.get());
    }

    static void markNearbyControllersDirty(
            Level level,
            BlockPos changedTankPos
    ) {
        markNearbyControllersDirty(
                level,
                changedTankPos,
                null
        );
    }

    private static void markNearbyControllersDirty(
            Level level,
            BlockPos changedTankPos,
            @Nullable FoundryControllerBlockEntity excludedController
    ) {
        if (
                level == null
                        || level.isClientSide()
                        || changedTankPos == null
        ) {
            return;
        }

        int horizontalRadius = FoundryTankNetwork.MAX_LONG_SIDE;
        int verticalRadius = FoundryTankNetwork.MAX_HEIGHT - 1;

        for (
                int y = changedTankPos.getY() - verticalRadius;
                y <= changedTankPos.getY() + verticalRadius;
                y++
        ) {
            for (
                    int x = changedTankPos.getX() - horizontalRadius;
                    x <= changedTankPos.getX() + horizontalRadius;
                    x++
            ) {
                for (
                        int z = changedTankPos.getZ() - horizontalRadius;
                        z <= changedTankPos.getZ() + horizontalRadius;
                        z++
                ) {
                    BlockEntity blockEntity =
                            level.getBlockEntity(
                                    new BlockPos(x, y, z)
                            );

                    if (
                            blockEntity instanceof FoundryControllerBlockEntity foundryController
                                    && foundryController != excludedController
                    ) {
                        foundryController.markTankStructureDirty();
                    }
                }
            }
        }
    }

    @Nullable
    static FoundryControllerBlockEntity findControllerForTank(
            Level level,
            BlockPos tankPos
    ) {
        if (level == null || tankPos == null) {
            return null;
        }

        int horizontalRadius = FoundryTankNetwork.MAX_LONG_SIDE;
        int verticalRadius = FoundryTankNetwork.MAX_HEIGHT - 1;
        FoundryControllerBlockEntity best = null;

        for (
                int y = tankPos.getY() - verticalRadius;
                y <= tankPos.getY() + verticalRadius;
                y++
        ) {
            for (
                    int x = tankPos.getX() - horizontalRadius;
                    x <= tankPos.getX() + horizontalRadius;
                    x++
            ) {
                for (
                        int z = tankPos.getZ() - horizontalRadius;
                        z <= tankPos.getZ() + horizontalRadius;
                        z++
                ) {
                    BlockEntity blockEntity =
                            level.getBlockEntity(
                                    new BlockPos(x, y, z)
                            );

                    if (
                            !(blockEntity instanceof FoundryControllerBlockEntity candidate)
                    ) {
                        continue;
                    }

                    FoundryTankNetwork network =
                            candidate.getOwnedTankNetwork();

                    if (
                            network == null
                                    || !network.getTankPositions()
                                    .contains(tankPos)
                    ) {
                        continue;
                    }

                    if (
                            best == null
                                    || POSITION_ORDER.compare(
                                    candidate.getBlockPos(),
                                    best.getBlockPos()
                            ) < 0
                    ) {
                        best = candidate;
                    }
                }
            }
        }

        return best;
    }

    @Nullable
    static FoundryControllerBlockEntity findControllerByIdNear(
            Level level,
            BlockPos around,
            java.util.UUID controllerId
    ) {
        if (
                level == null
                        || around == null
                        || controllerId == null
        ) {
            return null;
        }

        int horizontalRadius = FoundryTankNetwork.MAX_LONG_SIDE + 1;
        int verticalRadius = FoundryTankNetwork.MAX_HEIGHT;

        for (
                int y = around.getY() - verticalRadius;
                y <= around.getY() + verticalRadius;
                y++
        ) {
            for (
                    int x = around.getX() - horizontalRadius;
                    x <= around.getX() + horizontalRadius;
                    x++
            ) {
                for (
                        int z = around.getZ() - horizontalRadius;
                        z <= around.getZ() + horizontalRadius;
                        z++
                ) {
                    BlockEntity blockEntity =
                            level.getBlockEntity(
                                    new BlockPos(x, y, z)
                            );

                    if (
                            blockEntity instanceof FoundryControllerBlockEntity candidate
                                    && controllerId.equals(
                                    candidate.getControllerId()
                            )
                    ) {
                        return candidate;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Persistence is deliberately PURE.
     *
     * Never call ensureResolved() here. A save/update packet must not be able to
     * consume the dirty flag while a block mutation is in flight. The last
     * resolved snapshot is safe to save; server load keeps dirty=true and the
     * next Controller tick revalidates the real world.
     */
    void save(
            CompoundTag parent
    ) {
        CompoundTag tag = new CompoundTag();

        long[] positions =
                tankPositions.stream()
                        .sorted(POSITION_ORDER)
                        .mapToLong(BlockPos::asLong)
                        .toArray();

        tag.putLongArray(POSITIONS_TAG, positions);
        tag.putLong(REVISION_TAG, revision);
        parent.put(ROOT_TAG, tag);
    }

    void load(
            CompoundTag parent
    ) {
        tankPositions = Set.of();
        cachedNetwork = null;
        minY = 0;
        dirty = true;

        if (!parent.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            return;
        }

        CompoundTag tag = parent.getCompound(ROOT_TAG);
        Set<BlockPos> loaded = new LinkedHashSet<>();

        if (tag.contains(POSITIONS_TAG, Tag.TAG_LONG_ARRAY)) {
            for (long packed : tag.getLongArray(POSITIONS_TAG)) {
                loaded.add(BlockPos.of(packed).immutable());
            }
        }

        tankPositions = Set.copyOf(loaded);
        revision = Math.max(
                revision,
                tag.getLong(REVISION_TAG)
        );

        FoundryTankStructureValidation.ValidationResult validation =
                FoundryTankStructureValidation.validate(tankPositions);

        if (validation.valid()) {
            minY = validation.minY();
        } else {
            tankPositions = Set.of();
            minY = 0;
        }

        rebuildCachedNetwork();
    }

    private record Candidate(
            Set<BlockPos> positions,
            int columnCount,
            int minX,
            int minZ,
            int baseY
    ) {
        static Candidate empty() {
            return new Candidate(
                    Set.of(),
                    0,
                    Integer.MAX_VALUE,
                    Integer.MAX_VALUE,
                    Integer.MAX_VALUE
            );
        }

        static Candidate of(
                Set<BlockPos> positions,
                int baseY
        ) {
            int minX = positions.stream()
                    .mapToInt(BlockPos::getX)
                    .min()
                    .orElse(Integer.MAX_VALUE);

            int minZ = positions.stream()
                    .mapToInt(BlockPos::getZ)
                    .min()
                    .orElse(Integer.MAX_VALUE);

            return new Candidate(
                    Set.copyOf(positions),
                    countColumns(positions),
                    minX,
                    minZ,
                    baseY
            );
        }

        boolean isBetterThan(
                Candidate other
        ) {
            if (other == null) {
                return true;
            }

            if (positions.size() != other.positions.size()) {
                return positions.size() > other.positions.size();
            }

            if (columnCount != other.columnCount) {
                return columnCount > other.columnCount;
            }

            if (baseY != other.baseY) {
                return baseY < other.baseY;
            }

            if (minX != other.minX) {
                return minX < other.minX;
            }

            return minZ < other.minZ;
        }
    }
}
