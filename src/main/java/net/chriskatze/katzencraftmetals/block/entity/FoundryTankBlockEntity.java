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

    @Nullable
    private UUID networkId;

    @Nullable
    private UUID orphanLayoutId;

    /*
     * Temporary read-only migration staging for saves created before the
     * multi-metal layer format existed. These values are never used as a
     * gameplay shadow and disappear as soon as the network migrates them.
     */
    @Nullable
    private ResourceLocation pendingLegacyMetal;

    private int pendingLegacyAmount;

    /*
     * Bottom-to-top physical segments inside this individual Tank.
     */
    private List<FoundryMetalLayer> localMetalLayers =
            List.of();

    @Nullable
    private FoundryTankNetwork cachedNetwork;

    private long cachedNetworkGameTime =
            Long.MIN_VALUE;

    private boolean intakeHatchOpen;

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
    // INTAKE HATCH
    // =========================

    public boolean isIntakeHatchOpen() {
        return intakeHatchOpen;
    }

    public void setIntakeHatchOpen(
            boolean open
    ) {
        if (intakeHatchOpen == open) {
            return;
        }

        intakeHatchOpen =
                open;

        setChanged();
        syncToClient();
    }

    public boolean isTopTank() {
        return level != null
                && !(level.getBlockEntity(
                worldPosition.above()
        ) instanceof FoundryTankBlockEntity);
    }

    // =========================
    // MULTI-METAL STORAGE
    // =========================

    /**
     * Returns the authoritative bottom-to-top local physical layers.
     *
     * A temporary one-layer view is exposed only while a pre-multi-metal save
     * is waiting for its one-time network migration.
     */
    public List<FoundryMetalLayer> getLocalMetalLayers() {
        if (!localMetalLayers.isEmpty()) {
            return localMetalLayers;
        }

        if (
                pendingLegacyMetal == null
                        || pendingLegacyAmount <= 0
                        || !ModMoltenMetals.contains(
                        pendingLegacyMetal
                )
        ) {
            return List.of();
        }

        return List.of(
                new FoundryMetalLayer(
                        pendingLegacyMetal,
                        Math.min(
                                pendingLegacyAmount,
                                CAPACITY
                        )
                )
        );
    }

    List<FoundryMetalLayer> getAuthoritativeLocalMetalLayers() {
        return localMetalLayers;
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

        boolean pendingChanged =
                pendingLegacyMetal != null
                        || pendingLegacyAmount != 0;

        pendingLegacyMetal =
                null;

        pendingLegacyAmount =
                0;

        if (
                localMetalLayers.equals(
                        normalized
                )
                        && !pendingChanged
        ) {
            return;
        }

        localMetalLayers =
                normalized;

        setChanged();
        syncToClient();
    }

    boolean hasPendingLegacyStorage() {
        return pendingLegacyMetal != null
                && pendingLegacyAmount > 0
                && ModMoltenMetals.contains(
                pendingLegacyMetal
        );
    }

    @Nullable
    ResourceLocation getPendingLegacyMetal() {
        return pendingLegacyMetal;
    }

    int getPendingLegacyAmount() {
        return pendingLegacyAmount;
    }

    void clearPendingLegacyStorage() {
        if (
                pendingLegacyMetal == null
                        && pendingLegacyAmount == 0
        ) {
            return;
        }

        pendingLegacyMetal =
                null;

        pendingLegacyAmount =
                0;

        setChanged();
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
                ? network.extract(
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

        return network != null
                ? network.extract(
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

        return network != null
                ? network.getTotalMoltenAmount()
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

        tag.putBoolean(
                "IntakeHatchOpen",
                intakeHatchOpen
        );

        /*
         * Preserve an untouched pre-multi-metal save until its owning network
         * gets one chance to migrate it. Do not write an empty new-format list
         * beside it, because that would hide the pending legacy data on reload.
         */
        boolean hasPendingLegacy =
                pendingLegacyMetal != null
                        && pendingLegacyAmount > 0
                        && localMetalLayers.isEmpty();

        if (hasPendingLegacy) {
            tag.putString(
                    "StoredMetal",
                    pendingLegacyMetal.toString()
            );

            tag.putInt(
                    "MoltenAmount",
                    pendingLegacyAmount
            );
        } else {
            tag.putInt(
                    "MoltenStorageVersion",
                    2
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

        networkId =
                null;

        orphanLayoutId =
                null;

        pendingLegacyMetal =
                null;

        pendingLegacyAmount =
                0;

        localMetalLayers =
                List.of();

        if (tag.contains("TankNetworkId")) {
            try {
                networkId =
                        UUID.fromString(
                                tag.getString(
                                        "TankNetworkId"
                                )
                        );
            } catch (IllegalArgumentException ignored) {
                networkId =
                        null;
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
                orphanLayoutId =
                        null;
            }
        }

        intakeHatchOpen =
                tag.getBoolean(
                        "IntakeHatchOpen"
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
        } else {
            /*
             * One-time migration path for saves from before
             * MultiMetalLayers existed. The amount may represent more than one
             * Tank, so it is staged until the complete network can redistribute
             * it without loss.
             */
            ResourceLocation legacyMetal =
                    tag.contains(
                            "StoredMetal"
                    )
                            ? ResourceLocation.tryParse(
                            tag.getString(
                                    "StoredMetal"
                            )
                    )
                            : null;

            int legacyAmount =
                    Math.max(
                            0,
                            tag.getInt(
                                    "MoltenAmount"
                            )
                    );

            if (
                    legacyMetal != null
                            && legacyAmount > 0
                            && ModMoltenMetals.contains(
                            legacyMetal
                    )
            ) {
                pendingLegacyMetal =
                        legacyMetal;

                pendingLegacyAmount =
                        legacyAmount;
            }
        }

        invalidateNetworkCache();
    }

}
