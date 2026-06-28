package net.chriskatze.katzencraftmetals.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

public class FoundryTankBlockEntity extends BlockEntity {

    public static final int CAPACITY = 108;

    /*
     * Allows older saves from the previous master-Tank prototype to
     * load one temporarily oversized local value. The next successful
     * network claim redistributes it into normal 0-108 local shares.
     */
    private static final int MAX_LEGACY_NETWORK_CAPACITY =
            CAPACITY
                    * FoundryTankNetwork.MAX_TANK_COUNT;

    /*
     * This is the persistent UUID of the owning Controller.
     *
     * null means that the Tank belongs to an unassigned/orphan section.
     */
    @Nullable
    private UUID networkId;

    /*
     * Persistent identity of an unassigned Tank layout.
     *
     * This is deliberately separate from networkId:
     *
     * - networkId belongs to an active Foundry Controller
     * - orphanLayoutId belongs to a free-standing Tank layout
     *
     * Two touching orphan layouts can therefore remain visually and
     * structurally separate.
     */
    @Nullable
    private UUID orphanLayoutId;

    @Nullable
    private ResourceLocation storedMetal;

    /*
     * Every Tank stores an integer local share.
     */
    private int moltenAmount;

    @Nullable
    private FoundryTankNetwork cachedNetwork;

    private long cachedNetworkGameTime =
            Long.MIN_VALUE;

    public FoundryTankBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                ModBlockEntities.FOUNDRY_TANK.get(),
                pos,
                state
        );
    }

    // =========================
    // NETWORK
    // =========================

    @Nullable
    public FoundryTankNetwork getNetwork() {
        if (level == null) {
            return null;
        }

        long gameTime =
                level.getGameTime();

        if (cachedNetworkGameTime != gameTime) {
            cachedNetwork =
                    FoundryTankNetwork.find(
                            level,
                            worldPosition
                    );

            cachedNetworkGameTime =
                    gameTime;
        }

        return cachedNetwork;
    }

    public void invalidateNetworkCache() {
        cachedNetwork = null;
        cachedNetworkGameTime =
                Long.MIN_VALUE;
    }

    @Nullable
    public UUID getNetworkId() {
        return networkId;
    }

    public void setNetworkId(
            @Nullable UUID networkId
    ) {
        if (Objects.equals(
                this.networkId,
                networkId
        )) {
            return;
        }

        this.networkId =
                networkId;

        invalidateNetworkCache();
        setChanged();
        syncToClient();
    }

    @Nullable
    public UUID getOrphanLayoutId() {
        return orphanLayoutId;
    }

    public void setOrphanLayoutId(
            @Nullable UUID orphanLayoutId
    ) {
        if (Objects.equals(
                this.orphanLayoutId,
                orphanLayoutId
        )) {
            return;
        }

        this.orphanLayoutId =
                orphanLayoutId;

        invalidateNetworkCache();
        setChanged();
        syncToClient();
    }

    public boolean hasActiveController() {
        FoundryTankNetwork network =
                getNetwork();

        return network != null
                && network.isActive();
    }

    // =========================
    // INSERTION / EXTRACTION
    // =========================

    public boolean canAccept(
            ResourceLocation metal,
            int amount
    ) {
        FoundryTankNetwork network =
                getNetwork();

        return network != null
                && network.canAccept(
                metal,
                amount
        );
    }

    public int insert(
            ResourceLocation metal,
            int amount
    ) {
        FoundryTankNetwork network =
                getNetwork();

        return network != null
                ? network.insert(
                metal,
                amount
        )
                : 0;
    }

    public int extract(
            int requestedAmount
    ) {
        FoundryTankNetwork network =
                getNetwork();

        return network != null
                ? network.extract(
                requestedAmount
        )
                : 0;
    }

    // =========================
    // GETTERS
    // =========================

    public int getMoltenAmount() {
        FoundryTankNetwork network =
                getNetwork();

        return network != null
                ? network.getMoltenAmount()
                : moltenAmount;
    }

    public int getCapacity() {
        FoundryTankNetwork network =
                getNetwork();

        return network != null
                ? network.getCapacity()
                : CAPACITY;
    }

    public float getFillPercentage() {
        return Mth.clamp(
                (float) getMoltenAmount()
                        / getCapacity(),
                0.0f,
                1.0f
        );
    }

    public float getLocalVisualMoltenAmount() {
        FoundryTankNetwork network =
                getNetwork();

        if (network != null) {
            return network.getLocalVisualMoltenAmount(
                    worldPosition
            );
        }

        return Mth.clamp(
                moltenAmount,
                0,
                CAPACITY
        );
    }

    public boolean isEmpty() {
        return getMoltenAmount() <= 0;
    }

    public boolean isFull() {
        return getMoltenAmount()
                >= getCapacity();
    }

    @Nullable
    public ResourceLocation getStoredMetal() {
        FoundryTankNetwork network =
                getNetwork();

        return network != null
                ? network.getStoredMetal()
                : storedMetal;
    }

    // =========================
    // LOCAL STORAGE
    // =========================

    int getLocalMoltenAmount() {
        return moltenAmount;
    }

    @Nullable
    ResourceLocation getLocalStoredMetal() {
        return storedMetal;
    }

    void setLocalStorage(
            @Nullable ResourceLocation metal,
            int amount
    ) {
        int clampedAmount =
                Mth.clamp(
                        amount,
                        0,
                        CAPACITY
                );

        ResourceLocation normalizedMetal =
                clampedAmount > 0
                        ? metal
                        : null;

        if (
                moltenAmount == clampedAmount
                        && Objects.equals(
                        storedMetal,
                        normalizedMetal
                )
        ) {
            return;
        }

        storedMetal =
                normalizedMetal;

        moltenAmount =
                clampedAmount;

        setChanged();
        syncToClient();
    }

    // =========================
    // CLIENT SYNCHRONIZATION
    // =========================

    @Override
    public CompoundTag getUpdateTag(
            HolderLookup.Provider registries
    ) {
        CompoundTag tag =
                new CompoundTag();

        saveAdditional(
                tag,
                registries
        );

        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    void syncToClient() {
        if (
                level == null
                        || level.isClientSide()
        ) {
            return;
        }

        BlockState state =
                getBlockState();

        level.sendBlockUpdated(
                worldPosition,
                state,
                state,
                Block.UPDATE_CLIENTS
        );
    }

    // =========================
    // SAVE / LOAD
    // =========================

    @Override
    protected void saveAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.saveAdditional(
                tag,
                registries
        );

        if (networkId != null) {
            tag.putString(
                    "TankNetworkId",
                    networkId.toString()
            );
        }

        if (orphanLayoutId != null) {
            tag.putString(
                    "TankOrphanLayoutId",
                    orphanLayoutId.toString()
            );
        }

        if (storedMetal != null) {
            tag.putString(
                    "StoredMetal",
                    storedMetal.toString()
            );
        }

        tag.putInt(
                "MoltenAmount",
                moltenAmount
        );
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.loadAdditional(
                tag,
                registries
        );

        networkId = null;
        orphanLayoutId = null;
        storedMetal = null;
        moltenAmount = 0;

        if (tag.contains("TankNetworkId")) {
            try {
                networkId =
                        UUID.fromString(
                                tag.getString(
                                        "TankNetworkId"
                                )
                        );
            } catch (IllegalArgumentException ignored) {
                networkId = null;
            }
        }

        if (tag.contains("TankOrphanLayoutId")) {
            try {
                orphanLayoutId =
                        UUID.fromString(
                                tag.getString(
                                        "TankOrphanLayoutId"
                                )
                        );
            } catch (IllegalArgumentException ignored) {
                orphanLayoutId = null;
            }
        }

        if (tag.contains("StoredMetal")) {
            storedMetal =
                    ResourceLocation.tryParse(
                            tag.getString("StoredMetal")
                    );
        }

        moltenAmount =
                Mth.clamp(
                        tag.getInt("MoltenAmount"),
                        0,
                        MAX_LEGACY_NETWORK_CAPACITY
                );

        if (storedMetal == null) {
            moltenAmount = 0;
        }

        if (moltenAmount == 0) {
            storedMetal = null;
        }

        invalidateNetworkCache();
    }
}
