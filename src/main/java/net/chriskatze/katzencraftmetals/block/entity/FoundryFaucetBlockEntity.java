package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.block.custom.FoundryFaucetBlock;
import net.chriskatze.katzencraftmetals.menu.FoundryFaucetOutputMenu;
import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.chriskatze.katzencraftmetals.metal.MoltenMetalDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

public class FoundryFaucetBlockEntity
        extends BlockEntity
        implements MenuProvider {

    public static final int TRANSFER_INTERVAL = 2;
    public static final int TRANSFER_AMOUNT = 1;

    public static final int MAX_CAULDRON_DISTANCE = 3;

    public static final int STREAM_ANIMATION_INTERVAL = 2;
    public static final int STREAM_ANIMATION_STEPS = 8;

    private boolean pouring;
    private int transferTimer;
    private int streamAnimationStep;
    private int streamAnimationTimer;

    /*
     * Per-Faucet output selection. Null means "automatic": use the old
     * Controller-selected/default output behavior.
     */
    @Nullable
    private ResourceLocation selectedOutputMetal;

    /*
     * Locked when pouring starts. The Faucet never silently changes to another
     * metal while its channel or stream is active.
     */
    @Nullable
    private ResourceLocation pouringMetal;

    public FoundryFaucetBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                ModBlockEntities.FOUNDRY_FAUCET.get(),
                pos,
                state
        );
    }

    // =========================
    // MENU PROVIDER
    // =========================

    @Override
    public Component getDisplayName() {
        return Component.literal("Faucet Output");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(
            int containerId,
            Inventory playerInventory,
            Player player
    ) {
        return new FoundryFaucetOutputMenu(
                containerId,
                playerInventory,
                this
        );
    }

    // =========================
    // SERVER TICK
    // =========================

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            FoundryFaucetBlockEntity faucet
    ) {
        if (level.isClientSide()) {
            return;
        }

        if (!faucet.pouring) {
            if (faucet.streamAnimationStep > 0) {
                faucet.streamAnimationTimer++;

                if (
                        faucet.streamAnimationTimer
                                >= STREAM_ANIMATION_INTERVAL
                ) {
                    faucet.streamAnimationTimer = 0;
                    faucet.streamAnimationStep--;

                    if (faucet.streamAnimationStep <= 0) {
                        faucet.streamAnimationStep = 0;
                        faucet.pouringMetal = null;
                    }

                    faucet.setChanged();
                    faucet.syncToClient();
                }
            } else if (faucet.pouringMetal != null) {
                faucet.pouringMetal = null;
                faucet.setChanged();
                faucet.syncToClient();
            }

            return;
        }

        PouringContext context =
                resolvePouringContext(
                        level,
                        pos,
                        state,
                        faucet
                );

        if (context == null) {
            faucet.stopPouring();
            return;
        }

        /*
         * The first valid server tick after activation permanently locks the
         * output metal for this pour.
         */
        if (faucet.pouringMetal == null) {
            faucet.pouringMetal =
                    context.metal();

            faucet.setChanged();
            faucet.syncToClient();
        }

        if (faucet.streamAnimationStep < STREAM_ANIMATION_STEPS) {
            faucet.streamAnimationTimer++;

            if (
                    faucet.streamAnimationTimer
                            >= STREAM_ANIMATION_INTERVAL
            ) {
                faucet.streamAnimationTimer = 0;
                faucet.streamAnimationStep++;

                faucet.setChanged();
                faucet.syncToClient();
            }

            return;
        }

        faucet.transferTimer++;

        if (faucet.transferTimer < TRANSFER_INTERVAL) {
            return;
        }

        faucet.transferTimer = 0;

        int extracted =
                context.network()
                        .extract(
                                context.metal(),
                                TRANSFER_AMOUNT
                        );

        if (extracted != TRANSFER_AMOUNT) {
            faucet.stopPouring();
            return;
        }

        int inserted =
                context.cauldron()
                        .insert(
                                context.metal(),
                                extracted
                        );

        if (inserted != extracted) {
            context.network()
                    .insert(
                            context.metal(),
                            extracted
                    );

            faucet.stopPouring();
            return;
        }

        /*
         * A Faucet draws the selected metal from the complete connected Tank
         * network. The active pour still remains locked to one metal. It stops
         * when that exact metal is gone rather than silently switching to
         * another metal.
         */
        if (
                context.cauldron().isFull()
                        || context.network()
                        .getMoltenAmount(
                                context.metal()
                        ) < TRANSFER_AMOUNT
        ) {
            faucet.stopPouring();
        }
    }

    // =========================
    // PER-FAUCET OUTPUT SELECTION
    // =========================

    public Optional<ResourceLocation> getSelectedOutputMetalId() {
        return Optional.ofNullable(
                selectedOutputMetal
        );
    }

    public void clearSelectedOutputMetal() {
        setSelectedOutputMetal(null);
    }

    public void setSelectedOutputMetal(
            @Nullable ResourceLocation metal
    ) {
        ResourceLocation normalized =
                metal != null
                        && ModMoltenMetals.contains(metal)
                        ? metal
                        : null;

        if (Objects.equals(
                selectedOutputMetal,
                normalized
        )) {
            return;
        }

        selectedOutputMetal =
                normalized;

        /*
         * If the player changes this Faucet to a different explicit metal while
         * it is already pouring, stop the current pour instead of silently
         * switching the active stream.
         */
        if (
                pouring
                        && pouringMetal != null
                        && selectedOutputMetal != null
                        && !selectedOutputMetal.equals(
                        pouringMetal
                )
        ) {
            stopPouring();
        }

        setChanged();
        syncToClient();
    }

    public Optional<ResourceLocation> resolveOutputMetal(
            FoundryTankBlockEntity tank
    ) {
        if (
                tank == null
                        || tank.getLevel() == null
        ) {
            return Optional.empty();
        }

        FoundryTankNetwork network =
                tank.getNetwork();

        if (
                network == null
                        || !network.isActive()
        ) {
            return Optional.empty();
        }

        network.ensureMoltenContentsMigrated();

        if (selectedOutputMetal != null) {
            return network.getMoltenAmount(
                    selectedOutputMetal
            ) >= TRANSFER_AMOUNT
                    ? Optional.of(selectedOutputMetal)
                    : Optional.empty();
        }

        FoundryControllerBlockEntity controller =
                network.getAttachedController();

        ResourceLocation automaticMetal =
                controller != null
                        ? controller.getSelectedOutputMetalOrDefault(
                        network
                )
                        : tank.getTopLocalMetal();

        if (
                automaticMetal != null
                        && network.getMoltenAmount(
                        automaticMetal
                ) >= TRANSFER_AMOUNT
        ) {
            return Optional.of(automaticMetal);
        }

        for (MoltenMetalDefinition definition : ModMoltenMetals.heaviestFirst()) {
            if (
                    network.getMoltenAmount(
                            definition.id()
                    ) >= TRANSFER_AMOUNT
            ) {
                return Optional.of(
                        definition.id()
                );
            }
        }

        return Optional.empty();
    }

    // =========================
    // SMART SOURCE / TARGET LOOKUP
    // =========================

    /**
     * Retained for compatibility with older callers. This method has the old
     * name, but the current Foundry uses the complete connected Tank network.
     */
    public static boolean hasMoltenAtFaucetHeight(
            FoundryTankBlockEntity tank
    ) {
        if (
                tank == null
                        || tank.getLevel() == null
        ) {
            return false;
        }

        FoundryTankNetwork network =
                tank.getNetwork();

        if (
                network == null
                        || !network.isActive()
        ) {
            return false;
        }

        network.ensureMoltenContentsMigrated();

        FoundryControllerBlockEntity controller =
                network.getAttachedController();

        ResourceLocation selectedMetal =
                controller != null
                        ? controller.getSelectedOutputMetalOrDefault(
                        network
                )
                        : tank.getTopLocalMetal();

        if (
                selectedMetal != null
                        && network.getMoltenAmount(
                        selectedMetal
                ) >= TRANSFER_AMOUNT
        ) {
            return true;
        }

        for (MoltenMetalDefinition definition : ModMoltenMetals.heaviestFirst()) {
            if (
                    network.getMoltenAmount(
                            definition.id()
                    ) >= TRANSFER_AMOUNT
            ) {
                return true;
            }
        }

        return false;
    }

    public static boolean hasMoltenAtFaucetHeight(
            FoundryTankBlockEntity tank,
            ResourceLocation metal
    ) {
        if (
                tank == null
                        || tank.getLevel() == null
                        || metal == null
        ) {
            return false;
        }

        FoundryTankNetwork network =
                tank.getNetwork();

        if (
                network == null
                        || !network.isActive()
        ) {
            return false;
        }

        network.ensureMoltenContentsMigrated();

        return network.getMoltenAmount(
                metal
        ) >= TRANSFER_AMOUNT;
    }

    @Nullable
    public static CauldronTarget findCauldronTarget(
            Level level,
            BlockPos faucetPos
    ) {
        for (
                int distance = 1;
                distance <= MAX_CAULDRON_DISTANCE;
                distance++
        ) {
            BlockPos checkedPos =
                    faucetPos.below(distance);

            BlockEntity blockEntity =
                    level.getBlockEntity(
                            checkedPos
                    );

            if (
                    blockEntity
                            instanceof CastingCauldronBlockEntity cauldron
            ) {
                return new CauldronTarget(
                        cauldron,
                        distance
                );
            }

            if (!level.getBlockState(
                    checkedPos
            ).getCollisionShape(
                    level,
                    checkedPos
            ).isEmpty()) {
                return null;
            }
        }

        return null;
    }

    @Nullable
    private static PouringContext resolvePouringContext(
            Level level,
            BlockPos faucetPos,
            BlockState faucetState,
            FoundryFaucetBlockEntity faucet
    ) {
        Direction facing =
                faucetState.getValue(
                        FoundryFaucetBlock.FACING
                );

        BlockPos tankPosition =
                faucetPos.relative(
                        facing.getOpposite()
                );

        BlockEntity tankBlockEntity =
                level.getBlockEntity(
                        tankPosition
                );

        if (!(tankBlockEntity instanceof FoundryTankBlockEntity tank)) {
            return null;
        }

        FoundryTankNetwork network =
                tank.getNetwork();

        if (
                network == null
                        || !network.isActive()
        ) {
            return null;
        }

        network.ensureMoltenContentsMigrated();

        ResourceLocation outputMetal =
                faucet.pouringMetal != null
                        ? faucet.pouringMetal
                        : faucet.resolveOutputMetal(
                        tank
                ).orElse(null);

        if (
                outputMetal == null
                        || network.getMoltenAmount(
                        outputMetal
                ) < TRANSFER_AMOUNT
        ) {
            return null;
        }

        CauldronTarget target =
                findCauldronTarget(
                        level,
                        faucetPos
                );

        if (target == null) {
            return null;
        }

        CastingCauldronBlockEntity cauldron =
                target.cauldron();

        if (
                cauldron.isFull()
                        || !cauldron.canAccept(
                        outputMetal,
                        TRANSFER_AMOUNT
                )
        ) {
            return null;
        }

        return new PouringContext(
                network,
                tank,
                cauldron,
                outputMetal
        );
    }

    public record CauldronTarget(
            CastingCauldronBlockEntity cauldron,
            int distance
    ) {
    }

    private record PouringContext(
            FoundryTankNetwork network,
            FoundryTankBlockEntity tank,
            CastingCauldronBlockEntity cauldron,
            ResourceLocation metal
    ) {
    }

    // =========================
    // POURING STATE
    // =========================

    public void startPouring(
            ResourceLocation metal
    ) {
        if (
                level == null
                        || level.isClientSide()
                        || metal == null
                        || !ModMoltenMetals.contains(
                        metal
                )
        ) {
            return;
        }

        pouringMetal =
                metal;

        setPouring(true);
    }

    public void startPouring() {
        if (
                level == null
                        || level.isClientSide()
        ) {
            return;
        }

        PouringContext context =
                resolvePouringContext(
                        level,
                        worldPosition,
                        getBlockState(),
                        this
                );

        if (context == null) {
            return;
        }

        pouringMetal =
                context.metal();

        setPouring(true);
    }

    public void stopPouring() {
        setPouring(false);
    }

    public void setPouring(
            boolean pouring
    ) {
        if (this.pouring == pouring) {
            return;
        }

        this.pouring = pouring;
        this.transferTimer = 0;
        this.streamAnimationTimer = 0;

        setChanged();
        syncToClient();
    }

    public boolean isPouring() {
        return pouring;
    }

    public boolean isDraining() {
        return !pouring
                && streamAnimationStep > 0;
    }

    public int getStreamAnimationStep() {
        return streamAnimationStep;
    }

    @Nullable
    public ResourceLocation getPouringMetal() {
        return pouringMetal;
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

    private void syncToClient() {
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

        tag.putBoolean(
                "Pouring",
                pouring
        );

        tag.putInt(
                "StreamAnimationStep",
                streamAnimationStep
        );

        if (selectedOutputMetal != null) {
            tag.putString(
                    "SelectedOutputMetal",
                    selectedOutputMetal.toString()
            );
        }

        if (pouringMetal != null) {
            tag.putString(
                    "PouringMetal",
                    pouringMetal.toString()
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

        pouring =
                tag.getBoolean(
                        "Pouring"
                );

        transferTimer = 0;

        streamAnimationStep =
                Math.max(
                        0,
                        Math.min(
                                STREAM_ANIMATION_STEPS,
                                tag.getInt(
                                        "StreamAnimationStep"
                                )
                        )
                );

        streamAnimationTimer = 0;

        selectedOutputMetal =
                null;

        if (tag.contains("SelectedOutputMetal")) {
            selectedOutputMetal =
                    ResourceLocation.tryParse(
                            tag.getString(
                                    "SelectedOutputMetal"
                            )
                    );
        }

        if (
                selectedOutputMetal != null
                        && !ModMoltenMetals.contains(
                        selectedOutputMetal
                )
        ) {
            selectedOutputMetal = null;
        }

        pouringMetal =
                null;

        if (tag.contains("PouringMetal")) {
            pouringMetal =
                    ResourceLocation.tryParse(
                            tag.getString(
                                    "PouringMetal"
                            )
                    );
        }

        if (
                pouringMetal != null
                        && !ModMoltenMetals.contains(
                        pouringMetal
                )
        ) {
            pouringMetal = null;
        }

        if (
                pouringMetal == null
                        && streamAnimationStep <= 0
        ) {
            pouring = false;
        }
    }
}
