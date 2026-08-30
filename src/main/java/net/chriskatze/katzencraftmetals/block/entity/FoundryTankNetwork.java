package net.chriskatze.katzencraftmetals.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Public facade for one valid, face-connected Foundry Tank network.
 *
 * Structure discovery/ownership is implemented by FoundryTankStructure and
 * FoundryTankPlacement. Molten contents are implemented by
 * FoundryTankStorage. Keeping this facade small gives the rest of the mod one
 * stable API while the internal systems can grow independently.
 */
public final class FoundryTankNetwork {

    public static final int MAX_SHORT_SIDE = 4;
    public static final int MAX_LONG_SIDE = 4;
    public static final int MAX_HEIGHT = 4;
    public static final int MAX_COLUMN_COUNT =
            MAX_SHORT_SIDE * MAX_LONG_SIDE;
    public static final int MAX_TANK_COUNT =
            MAX_COLUMN_COUNT * MAX_HEIGHT;

    private final Level level;

    @Nullable
    private final UUID ownerId;

    private final Set<BlockPos> tankPositions;
    private final int minY;
    private final FoundryTankStorage storage;

    FoundryTankNetwork(
            Level level,
            @Nullable UUID ownerId,
            Set<BlockPos> tankPositions,
            int minY
    ) {
        this.level = level;
        this.ownerId = ownerId;
        this.tankPositions = Set.copyOf(tankPositions);
        this.minY = minY;
        this.storage = new FoundryTankStorage(this);
    }

    // =========================
    // DISCOVERY / PLACEMENT
    // =========================

    @Nullable
    public static FoundryTankNetwork find(
            Level level,
            BlockPos startPos
    ) {
        return FoundryTankStructure.find(level, startPos);
    }

    @Nullable
    public static FoundryTankNetwork claimLargestUnassignedLayoutForController(
            Level level,
            FoundryControllerBlockEntity controller
    ) {
        return FoundryTankPlacement.claimLargestUnassignedLayoutForController(
                level,
                controller
        );
    }

    /** Retained for compatibility with older callers. */
    @Nullable
    public static FoundryTankNetwork claimLargestUnassignedLayout(
            Level level,
            BlockPos startPos,
            UUID controllerId
    ) {
        return FoundryTankPlacement.claimLargestUnassignedLayout(
                level,
                startPos,
                controllerId
        );
    }

    @Nullable
    public static FoundryTankNetwork claimExactLayout(
            Level level,
            Set<BlockPos> selected,
            UUID controllerId
    ) {
        return FoundryTankPlacement.claimExactLayout(
                level,
                selected,
                controllerId
        );
    }

    public static boolean handleTankPlaced(
            Level level,
            BlockPos placedPos,
            BlockPos clickedAgainstPos
    ) {
        return FoundryTankPlacement.handleTankPlaced(
                level,
                placedPos,
                clickedAgainstPos
        );
    }

    public static boolean hasNearbyFoundryCandidate(
            Level level,
            BlockPos placedPos
    ) {
        return FoundryTankPlacement.hasNearbyFoundryCandidate(
                level,
                placedPos
        );
    }

    // =========================
    // OWNERSHIP / STRUCTURE
    // =========================

    public boolean isActive() {
        return ownerId != null
                && getAttachedController() != null;
    }

    @Nullable
    public FoundryControllerBlockEntity getAttachedController() {
        return ownerId == null
                ? null
                : FoundryTankStructure.findAttachedController(
                level,
                tankPositions,
                ownerId
        );
    }

    /** Turns this complete section into an orphan while preserving contents. */
    public void releaseOwnership() {
        FoundryTankStructure.releaseOwnership(this);
    }

    public static boolean isValidStructure(
            Set<BlockPos> positions
    ) {
        return FoundryTankStructure.validateStructure(positions).valid();
    }

    public static Set<BlockPos> findUpwardColumn(
            Level level,
            BlockPos startPos
    ) {
        return FoundryTankStructure.findUpwardColumn(level, startPos);
    }

    public void prepareUpwardRemoval(
            Set<BlockPos> removedPositions
    ) {
        FoundryTankStructure.prepareUpwardRemoval(
                this,
                removedPositions
        );
    }

    public static List<BlockPos> findAttachedFaucets(
            Level level,
            Set<BlockPos> tankSubset
    ) {
        return FoundryTankStructure.findAttachedFaucets(
                level,
                tankSubset
        );
    }

    // =========================
    // STORAGE
    // =========================

    public void ensureMoltenContentsMigrated() {
        storage.ensureMigrated();
    }

    public Map<ResourceLocation, Integer> getMoltenContents() {
        return storage.getContents();
    }

    public int getMoltenAmount(
            ResourceLocation metal
    ) {
        return storage.getAmount(metal);
    }

    public int getTotalMoltenAmount() {
        return storage.getTotalAmount();
    }

    public boolean canAccept(
            ResourceLocation metal,
            int amount
    ) {
        return storage.canAccept(metal, amount);
    }

    public int insert(
            ResourceLocation metal,
            int amount
    ) {
        return storage.insert(metal, amount);
    }

    public int extract(
            ResourceLocation metal,
            int requestedAmount
    ) {
        return storage.extract(metal, requestedAmount);
    }

    /** Compatibility overload for older callers. */
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
        return storage.hasMetalAtHeight(tankY, metal);
    }

    /** Compatibility name: this is the total of every stored metal. */
    public int getMoltenAmount() {
        return getTotalMoltenAmount();
    }

    /**
     * Active Foundries report their selected output. An orphan reports a metal
     * only when exactly one distinct metal is stored.
     */
    @Nullable
    public ResourceLocation getStoredMetal() {
        FoundryControllerBlockEntity controller = getAttachedController();

        if (controller != null) {
            ResourceLocation selected =
                    controller.getSelectedOutputMetalOrDefault(this);

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
        return storage.getLocalVisualMoltenAmount(tankPos);
    }

    // =========================
    // GETTERS / INTERNAL ACCESS
    // =========================

    public int getCapacity() {
        return tankPositions.size()
                * FoundryTankBlockEntity.CAPACITY;
    }

    public int getTankCount() {
        return tankPositions.size();
    }

    @Nullable
    public UUID getOwnerId() {
        return ownerId;
    }

    public Set<BlockPos> getTankPositions() {
        return tankPositions;
    }

    public int getMinY() {
        return minY;
    }

    Level level() {
        return level;
    }
}
