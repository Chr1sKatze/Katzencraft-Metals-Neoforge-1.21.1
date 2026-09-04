package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.block.custom.FoundryFaucetBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Stable public facade for one Controller-owned Foundry vessel.
 *
 * The facade remains so processing/alloying/faucet/menu code does not need to
 * know how the Controller stores its cached structure. No Tank block owns a
 * network anymore.
 */
public final class FoundryTankNetwork {

    public static final int MAX_SHORT_SIDE = 4;
    public static final int MAX_LONG_SIDE = 4;
    public static final int MAX_HEIGHT = 4;
    public static final int MAX_COLUMN_COUNT =
            MAX_SHORT_SIDE * MAX_LONG_SIDE;
    public static final int MAX_TANK_COUNT =
            MAX_COLUMN_COUNT * MAX_HEIGHT;

    public static final int TANK_CAPACITY = 108;

    @Nullable
    private final FoundryControllerBlockEntity controller;
    @Nullable
    private final Level legacyLevel;
    @Nullable
    private final UUID legacyOwnerId;
    @Nullable
    private final FoundryTankStorage legacyStorage;
    private final Set<BlockPos> tankPositions;
    private final int minY;
    private final long structureRevision;

    FoundryTankNetwork(
            FoundryControllerBlockEntity controller,
            Set<BlockPos> tankPositions,
            int minY,
            long structureRevision
    ) {
        this.controller = controller;
        this.legacyLevel = null;
        this.legacyOwnerId = null;
        this.legacyStorage = null;
        this.tankPositions = Set.copyOf(tankPositions);
        this.minY = minY;
        this.structureRevision = structureRevision;
    }

    /**
     * Source-compatibility constructor for the old Tank-BE structure classes.
     * New gameplay never creates this form; it exists so legacy save migration
     * helpers can remain compiled for one transition release.
     */
    FoundryTankNetwork(
            Level level,
            @Nullable UUID ownerId,
            Set<BlockPos> tankPositions,
            int minY
    ) {
        this.controller = null;
        this.legacyLevel = level;
        this.legacyOwnerId = ownerId;
        this.tankPositions = Set.copyOf(tankPositions);
        this.minY = minY;
        this.structureRevision = 0L;
        this.legacyStorage = new FoundryTankStorage(this);
    }

    // =========================
    // DISCOVERY / COMPATIBILITY
    // =========================

    @Nullable
    public static FoundryTankNetwork find(
            Level level,
            BlockPos tankPos
    ) {
        FoundryControllerBlockEntity controller =
                FoundryControllerTankStructure.findControllerForTank(
                        level,
                        tankPos
                );

        return controller != null
                ? controller.getOwnedTankNetwork()
                : null;
    }

    @Nullable
    public static FoundryTankNetwork claimLargestUnassignedLayoutForController(
            Level level,
            FoundryControllerBlockEntity controller
    ) {
        if (controller == null) {
            return null;
        }

        controller.markTankStructureDirty();
        return controller.getOwnedTankNetwork();
    }

    @Nullable
    public static FoundryTankNetwork claimLargestUnassignedLayout(
            Level level,
            BlockPos startPos,
            UUID controllerId
    ) {
        FoundryControllerBlockEntity controller =
                FoundryControllerTankStructure.findControllerByIdNear(
                        level,
                        startPos,
                        controllerId
                );

        if (controller == null) {
            return null;
        }

        controller.markTankStructureDirty();
        FoundryTankNetwork network = controller.getOwnedTankNetwork();

        return network != null
                && network.getTankPositions().contains(startPos)
                ? network
                : null;
    }

    @Nullable
    public static FoundryTankNetwork claimExactLayout(
            Level level,
            Set<BlockPos> selected,
            UUID controllerId
    ) {
        if (
                level == null
                        || selected == null
                        || selected.isEmpty()
        ) {
            return null;
        }

        BlockPos anchor = selected.iterator().next();
        FoundryControllerBlockEntity controller =
                FoundryControllerTankStructure.findControllerByIdNear(
                        level,
                        anchor,
                        controllerId
                );

        if (controller == null) {
            return null;
        }

        controller.markTankStructureDirty();
        FoundryTankNetwork network = controller.getOwnedTankNetwork();

        return network != null
                && network.getTankPositions().equals(selected)
                ? network
                : null;
    }

    public static boolean handleTankPlaced(
            Level level,
            BlockPos placedPos,
            BlockPos clickedAgainstPos
    ) {
        if (level == null || placedPos == null || level.isClientSide()) {
            return false;
        }

        /*
         * Structure edits only invalidate nearby Controller caches. Do NOT
         * immediately call find()/ensureResolved() from Block.onPlace/onRemove.
         * During onRemove Minecraft may still be inside the block-state change,
         * so resolving here can cache the pre-removal layout and clear the dirty
         * flag before the world mutation is complete. The Controller's next
         * server tick resolves the final physical world state exactly once.
         */
        FoundryControllerTankStructure.markNearbyControllersDirty(
                level,
                placedPos
        );

        return true;
    }

    public static boolean hasNearbyFoundryCandidate(
            Level level,
            BlockPos placedPos
    ) {
        if (level == null || placedPos == null) {
            return false;
        }

        if (find(level, placedPos) != null) {
            return true;
        }

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (
                    level.getBlockEntity(
                            placedPos.relative(direction)
                    ) instanceof FoundryControllerBlockEntity controller
                            && controller.isValidTankAttachmentPosition(
                            placedPos
                    )
            ) {
                return true;
            }
        }

        return false;
    }

    // =========================
    // OWNERSHIP / STRUCTURE
    // =========================

    public boolean isActive() {
        if (controller != null) {
            return controller.getLevel() != null
                    && !tankPositions.isEmpty();
        }

        return legacyOwnerId != null
                && getAttachedController() != null;
    }

    @Nullable
    public FoundryControllerBlockEntity getAttachedController() {
        if (controller != null) {
            return controller;
        }

        return legacyLevel == null
                || legacyOwnerId == null
                ? null
                : FoundryTankStructure.findAttachedController(
                legacyLevel,
                tankPositions,
                legacyOwnerId
        );
    }

    /**
     * Controller-owned networks clear storage with their Controller. Legacy
     * migration facades retain the old orphan-release behavior only for source
     * compatibility with pre-rewrite helpers.
     */
    public void releaseOwnership() {
        if (controller != null) {
            controller.releaseFoundry();
        } else {
            FoundryTankStructure.releaseOwnership(this);
        }
    }

    public static boolean isValidStructure(
            Set<BlockPos> positions
    ) {
        return FoundryTankStructureValidation.validate(positions).valid();
    }

    /**
     * Retained for older callers. The final Tank block uses cached structure
     * positions directly and no longer performs ownership discovery here.
     */
    public static Set<BlockPos> findUpwardColumn(
            Level level,
            BlockPos startPos
    ) {
        FoundryTankNetwork network = find(level, startPos);

        if (network == null) {
            return Set.of(startPos.immutable());
        }

        Set<BlockPos> removed = new HashSet<>();

        for (BlockPos tankPos : network.getTankPositions()) {
            if (
                    tankPos.getX() == startPos.getX()
                            && tankPos.getZ() == startPos.getZ()
                            && tankPos.getY() >= startPos.getY()
            ) {
                removed.add(tankPos.immutable());
            }
        }

        return removed;
    }

    /**
     * Storage no longer needs pre-removal redistribution. The Controller keeps
     * its pooled contents and trims only after its event-driven structure cache
     * resolves the new capacity.
     */
    public void prepareUpwardRemoval(
            Set<BlockPos> removedPositions
    ) {
        if (controller != null) {
            controller.markTankStructureDirty();
        } else {
            FoundryTankStructure.prepareUpwardRemoval(
                    this,
                    removedPositions
            );
        }
    }

    public static List<BlockPos> findAttachedFaucets(
            Level level,
            Set<BlockPos> tankSubset
    ) {
        if (
                level == null
                        || tankSubset == null
                        || tankSubset.isEmpty()
        ) {
            return new ArrayList<>();
        }

        Set<BlockPos> faucets = new HashSet<>();

        for (BlockPos tankPos : tankSubset) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos faucetPos = tankPos.relative(direction);
                BlockState faucetState = level.getBlockState(faucetPos);

                if (
                        faucetState.getBlock()
                                instanceof FoundryFaucetBlock
                                && faucetState.hasProperty(
                                FoundryFaucetBlock.FACING
                        )
                                && faucetState.getValue(
                                FoundryFaucetBlock.FACING
                        ) == direction
                ) {
                    faucets.add(faucetPos.immutable());
                }
            }
        }

        return new ArrayList<>(faucets);
    }

    // =========================
    // CONTROLLER-OWNED STORAGE
    // =========================

    public void ensureMoltenContentsMigrated() {
        if (controller != null) {
            controller.getTankStorage()
                    .ensureBound(this);
        } else if (legacyStorage != null) {
            legacyStorage.ensureMigrated();
        }
    }

    public Map<ResourceLocation, Integer> getMoltenContents() {
        return controller != null
                ? controller.getTankStorage()
                .getContents(this)
                : legacyStorage != null
                ? legacyStorage.getContents()
                : Map.of();
    }

    public int getMoltenAmount(
            ResourceLocation metal
    ) {
        return controller != null
                ? controller.getTankStorage()
                .getAmount(this, metal)
                : legacyStorage != null
                ? legacyStorage.getAmount(metal)
                : 0;
    }

    public int getTotalMoltenAmount() {
        return controller != null
                ? controller.getTankStorage()
                .getTotalAmount(this)
                : legacyStorage != null
                ? legacyStorage.getTotalAmount()
                : 0;
    }

    public boolean canAccept(
            ResourceLocation metal,
            int amount
    ) {
        return controller != null
                ? controller.getTankStorage()
                .canAccept(this, metal, amount)
                : legacyStorage != null
                && legacyStorage.canAccept(metal, amount);
    }

    public int insert(
            ResourceLocation metal,
            int amount
    ) {
        return controller != null
                ? controller.getTankStorage()
                .insert(this, metal, amount)
                : legacyStorage != null
                ? legacyStorage.insert(metal, amount)
                : 0;
    }

    public int extract(
            ResourceLocation metal,
            int requestedAmount
    ) {
        return controller != null
                ? controller.getTankStorage()
                .extract(this, metal, requestedAmount)
                : legacyStorage != null
                ? legacyStorage.extract(metal, requestedAmount)
                : 0;
    }

    public int extract(
            int requestedAmount
    ) {
        ResourceLocation metal = getStoredMetal();
        return metal == null
                ? 0
                : extract(metal, requestedAmount);
    }

    public boolean hasMetalAtHeight(
            int tankY,
            ResourceLocation metal
    ) {
        return controller != null
                ? controller.getTankStorage()
                .hasMetalAtHeight(this, tankY, metal)
                : legacyStorage != null
                && legacyStorage.hasMetalAtHeight(tankY, metal);
    }

    public int getMoltenAmount() {
        return getTotalMoltenAmount();
    }

    @Nullable
    public ResourceLocation getStoredMetal() {
        FoundryControllerBlockEntity attachedController =
                getAttachedController();

        if (attachedController != null) {
            ResourceLocation selected =
                    attachedController.getSelectedOutputMetalOrDefault(this);

            if (selected != null) {
                return selected;
            }
        }

        Map<ResourceLocation, Integer> contents = getMoltenContents();

        return contents.size() == 1
                ? contents.keySet().iterator().next()
                : null;
    }

    public boolean isEmpty() {
        return getTotalMoltenAmount() <= 0;
    }

    public boolean isFull() {
        return getTotalMoltenAmount() >= getCapacity();
    }

    public float getLocalVisualMoltenAmount(
            BlockPos tankPos
    ) {
        return controller != null
                ? controller.getTankStorage()
                .getLocalVisualMoltenAmount(this, tankPos)
                : legacyStorage != null
                ? legacyStorage.getLocalVisualMoltenAmount(tankPos)
                : 0.0f;
    }

    // =========================
    // GETTERS
    // =========================

    public int getCapacity() {
        return tankPositions.size()
                * TANK_CAPACITY;
    }

    public int getTankCount() {
        return tankPositions.size();
    }

    @Nullable
    public UUID getOwnerId() {
        return controller != null
                ? controller.getControllerId()
                : legacyOwnerId;
    }

    public Set<BlockPos> getTankPositions() {
        return tankPositions;
    }

    public int getMinY() {
        return minY;
    }

    public long getStructureRevision() {
        return structureRevision;
    }

    public long getStorageRevision() {
        return controller != null
                ? controller.getTankStorage().getRevision()
                : 0L;
    }

    Level level() {
        return controller != null
                ? controller.getLevel()
                : legacyLevel;
    }
}
