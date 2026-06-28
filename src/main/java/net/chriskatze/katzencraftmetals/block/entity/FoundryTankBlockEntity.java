package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.metal.FoundryMetalLayer;
import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class FoundryTankBlockEntity extends BlockEntity {

    public static final int CAPACITY = 108;

    private static final int MAX_LEGACY_NETWORK_CAPACITY =
            CAPACITY
                    * FoundryTankNetwork.MAX_TANK_COUNT;

    @Nullable
    private UUID networkId;

    @Nullable
    private UUID orphanLayoutId;

    /*
     * Compatibility shadow used by the existing structural Tank-network code.
     * Actual gameplay storage lives in localMetalLayers.
     */
    @Nullable
    private ResourceLocation storedMetal;

    private int moltenAmount;

    private boolean multiMetalStorageInitialized;

    /*
     * True only when an old pre-multi-metal local amount was actually loaded
     * from disk. A newly placed Tank can therefore remain empty even if the
     * structural compatibility shadow temporarily assigns units to it.
     */
    private boolean legacyStorageCandidate;

    /*
     * Bottom-to-top physical segments inside this individual Tank.
     */
    private List<FoundryMetalLayer> localMetalLayers =
            List.of();

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
    // MULTI-METAL STORAGE
    // =========================

    public boolean isMultiMetalStorageInitialized() {
        return multiMetalStorageInitialized;
    }

    boolean hasLegacyStorageCandidate() {
        return legacyStorageCandidate;
    }

    /**
     * Converts one old single-metal local share into the new local layer list.
     */
    public void initializeMultiMetalStorage() {
        initializeMultiMetalStorage(
                true
        );
    }

    void initializeMultiMetalStorage(
            boolean migrateLegacyShadow
    ) {
        if (multiMetalStorageInitialized) {
            return;
        }

        List<FoundryMetalLayer> migrated =
                new ArrayList<>();

        if (
                migrateLegacyShadow
                        && legacyStorageCandidate
                        && storedMetal != null
                        && moltenAmount > 0
                        && ModMoltenMetals.contains(
                        storedMetal
                )
        ) {
            migrated.add(
                    new FoundryMetalLayer(
                            storedMetal,
                            Math.min(
                                    moltenAmount,
                                    CAPACITY
                            )
                    )
            );
        }

        localMetalLayers =
                List.copyOf(migrated);

        multiMetalStorageInitialized =
                true;

        legacyStorageCandidate =
                false;

        setChanged();
        syncToClient();
    }

    /**
     * Returns bottom-to-top local physical layers.
     *
     * Before an old save is migrated, a temporary one-layer view is returned
     * from the legacy fields so rendering and storage reads remain correct.
     */
    public List<FoundryMetalLayer> getLocalMetalLayers() {
        if (multiMetalStorageInitialized) {
            return localMetalLayers;
        }

        if (
                storedMetal == null
                        || moltenAmount <= 0
                        || !ModMoltenMetals.contains(
                        storedMetal
                )
        ) {
            return List.of();
        }

        return List.of(
                new FoundryMetalLayer(
                        storedMetal,
                        Math.min(
                                moltenAmount,
                                CAPACITY
                        )
                )
        );
    }

    public int getLocalActualMoltenAmount() {
        int total =
                0;

        for (FoundryMetalLayer layer : getLocalMetalLayers()) {
            total += layer.amount();
        }

        return Mth.clamp(
                total,
                0,
                CAPACITY
        );
    }

    public int getLocalMetalAmount(
            ResourceLocation metal
    ) {
        if (metal == null) {
            return 0;
        }

        int total =
                0;

        for (FoundryMetalLayer layer : getLocalMetalLayers()) {
            if (layer.metal().equals(metal)) {
                total += layer.amount();
            }
        }

        return total;
    }

    void setLocalMetalLayers(
            List<FoundryMetalLayer> requestedLayers
    ) {
        List<FoundryMetalLayer> normalized =
                normalizeLayers(
                        requestedLayers
                );

        if (
                multiMetalStorageInitialized
                        && localMetalLayers.equals(
                        normalized
                )
        ) {
            return;
        }

        localMetalLayers =
                normalized;

        multiMetalStorageInitialized =
                true;

        legacyStorageCandidate =
                false;

        setChanged();
        syncToClient();
    }

    private static List<FoundryMetalLayer> normalizeLayers(
            List<FoundryMetalLayer> requestedLayers
    ) {
        if (
                requestedLayers == null
                        || requestedLayers.isEmpty()
        ) {
            return List.of();
        }

        List<FoundryMetalLayer> normalized =
                new ArrayList<>();

        int remainingCapacity =
                CAPACITY;

        for (FoundryMetalLayer requestedLayer : requestedLayers) {
            if (
                    requestedLayer == null
                            || requestedLayer.amount() <= 0
                            || !ModMoltenMetals.contains(
                            requestedLayer.metal()
                    )
                            || remainingCapacity <= 0
            ) {
                continue;
            }

            int accepted =
                    Math.min(
                            requestedLayer.amount(),
                            remainingCapacity
                    );

            if (!normalized.isEmpty()) {
                FoundryMetalLayer previous =
                        normalized.getLast();

                if (previous.metal().equals(
                        requestedLayer.metal()
                )) {
                    normalized.set(
                            normalized.size() - 1,
                            new FoundryMetalLayer(
                                    previous.metal(),
                                    previous.amount()
                                            + accepted
                            )
                    );
                } else {
                    normalized.add(
                            new FoundryMetalLayer(
                                    requestedLayer.metal(),
                                    accepted
                            )
                    );
                }
            } else {
                normalized.add(
                        new FoundryMetalLayer(
                                requestedLayer.metal(),
                                accepted
                        )
                );
            }

            remainingCapacity -=
                    accepted;
        }

        return List.copyOf(normalized);
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

        return level != null
                && network != null
                && FoundryMultiMetalStorage.canAccept(
                level,
                network,
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

        return level != null
                && network != null
                ? FoundryMultiMetalStorage.insert(
                level,
                network,
                metal,
                amount
        )
                : 0;
    }

    /**
     * Compatibility extraction uses the Controller's currently selected output
     * metal. Faucets use the explicit metal overload directly.
     */
    public int extract(
            int requestedAmount
    ) {
        FoundryTankNetwork network =
                getNetwork();

        if (
                level == null
                        || network == null
        ) {
            return 0;
        }

        FoundryControllerBlockEntity controller =
                network.getAttachedController();

        ResourceLocation selectedMetal =
                controller != null
                        ? controller.getSelectedOutputMetalOrDefault(
                        network
                )
                        : getTopLocalMetal();

        return selectedMetal != null
                ? FoundryMultiMetalStorage.extract(
                level,
                network,
                selectedMetal,
                requestedAmount
        )
                : 0;
    }

    public int extract(
            ResourceLocation metal,
            int requestedAmount
    ) {
        FoundryTankNetwork network =
                getNetwork();

        return level != null
                && network != null
                ? FoundryMultiMetalStorage.extract(
                level,
                network,
                metal,
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

        return level != null
                && network != null
                ? FoundryMultiMetalStorage.getTotalAmount(
                level,
                network
        )
                : getLocalActualMoltenAmount();
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
        return getLocalActualMoltenAmount();
    }

    public boolean isEmpty() {
        return getMoltenAmount() <= 0;
    }

    public boolean isFull() {
        return getMoltenAmount()
                >= getCapacity();
    }

    /**
     * For existing callers, return the globally selected output metal whenever
     * this Tank belongs to an active Foundry. Otherwise return the physical
     * topmost local metal.
     */
    @Nullable
    public ResourceLocation getStoredMetal() {
        FoundryTankNetwork network =
                getNetwork();

        if (network != null) {
            FoundryControllerBlockEntity controller =
                    network.getAttachedController();

            if (controller != null) {
                ResourceLocation selected =
                        controller.getSelectedOutputMetalOrDefault(
                                network
                        );

                if (selected != null) {
                    return selected;
                }
            }
        }

        return getTopLocalMetal();
    }

    @Nullable
    public ResourceLocation getTopLocalMetal() {
        List<FoundryMetalLayer> layers =
                getLocalMetalLayers();

        return layers.isEmpty()
                ? null
                : layers.getLast()
                .metal();
    }

    // =========================
    // LEGACY STRUCTURAL SHADOW
    // =========================

    int getLocalMoltenAmount() {
        return moltenAmount;
    }

    @Nullable
    ResourceLocation getLocalStoredMetal() {
        return storedMetal;
    }

    /**
     * Used only by the existing structure-management network.
     *
     * It deliberately does not overwrite localMetalLayers after the new system
     * has initialized. The compatibility shadow can therefore be rearranged by
     * placement/ownership code without losing real multi-metal composition.
     */
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

        tag.putBoolean(
                "MultiMetalInitialized",
                multiMetalStorageInitialized
        );

        ListTag layerTags =
                new ListTag();

        for (FoundryMetalLayer layer : localMetalLayers) {
            CompoundTag layerTag =
                    new CompoundTag();

            layerTag.putString(
                    "Metal",
                    layer.metal().toString()
            );

            layerTag.putInt(
                    "Amount",
                    layer.amount()
            );

            layerTags.add(layerTag);
        }

        tag.put(
                "MultiMetalLayers",
                layerTags
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
        multiMetalStorageInitialized = false;
        legacyStorageCandidate = false;
        localMetalLayers = List.of();

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
                            tag.getString(
                                    "StoredMetal"
                            )
                    );
        }

        moltenAmount =
                Mth.clamp(
                        tag.getInt(
                                "MoltenAmount"
                        ),
                        0,
                        MAX_LEGACY_NETWORK_CAPACITY
                );

        if (storedMetal == null) {
            moltenAmount = 0;
        }

        if (moltenAmount == 0) {
            storedMetal = null;
        }

        legacyStorageCandidate =
                storedMetal != null
                        && moltenAmount > 0
                        && !tag.contains(
                        "MultiMetalInitialized"
                )
                        && !tag.contains(
                        "MultiMetalLayers",
                        Tag.TAG_LIST
                );

        multiMetalStorageInitialized =
                tag.getBoolean(
                        "MultiMetalInitialized"
                )
                        || tag.contains(
                        "MultiMetalLayers",
                        Tag.TAG_LIST
                );

        if (
                tag.contains(
                        "MultiMetalLayers",
                        Tag.TAG_LIST
                )
        ) {
            ListTag layerTags =
                    tag.getList(
                            "MultiMetalLayers",
                            Tag.TAG_COMPOUND
                    );

            List<FoundryMetalLayer> loadedLayers =
                    new ArrayList<>();

            for (int index = 0; index < layerTags.size(); index++) {
                CompoundTag layerTag =
                        layerTags.getCompound(index);

                ResourceLocation metal =
                        ResourceLocation.tryParse(
                                layerTag.getString(
                                        "Metal"
                                )
                        );

                int amount =
                        layerTag.getInt(
                                "Amount"
                        );

                if (
                        metal != null
                                && amount > 0
                                && ModMoltenMetals.contains(
                                metal
                        )
                ) {
                    loadedLayers.add(
                            new FoundryMetalLayer(
                                    metal,
                                    amount
                            )
                    );
                }
            }

            localMetalLayers =
                    normalizeLayers(
                            loadedLayers
                    );
        }

        invalidateNetworkCache();
    }
}
