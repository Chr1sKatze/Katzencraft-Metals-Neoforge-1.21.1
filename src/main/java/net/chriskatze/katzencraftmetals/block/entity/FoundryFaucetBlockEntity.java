package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.block.custom.FoundryFaucetBlock;
import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class FoundryFaucetBlockEntity extends BlockEntity {

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
                        faucet.pouringMetal
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
         * network, not only from the physical liquid layer beside the Faucet.
         *
         * The active pour still remains locked to one metal. It stops when that
         * exact metal is gone rather than silently switching to another metal.
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
    // SMART SOURCE / TARGET LOOKUP
    // =========================

    /**
     * Retains the existing method name used by FoundryFaucetBlock, but the
     * Faucet now checks the complete connected Tank network.
     *
     * This allows a Faucet attached to a lower Tank to pour a selected metal
     * stored in a higher density layer or in a Tank above it.
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

        FoundryControllerBlockEntity controller =
                network.getAttachedController();

        ResourceLocation selectedMetal =
                controller != null
                        ? controller.getSelectedOutputMetalOrDefault(
                        network
                )
                        : tank.getTopLocalMetal();

        return selectedMetal != null
                && network.getMoltenAmount(
                selectedMetal
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
            @Nullable ResourceLocation lockedMetal
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

        FoundryControllerBlockEntity controller =
                network.getAttachedController();

        ResourceLocation outputMetal =
                lockedMetal != null
                        ? lockedMetal
                        : controller != null
                        ? controller.getSelectedOutputMetalOrDefault(
                        network
                )
                        : null;

        /*
         * The Controller selects which metal the Faucet draws from the complete
         * Tank network. Physical density layers remain visual and determine the
         * Tank contents, but they no longer restrict Faucet placement height.
         */
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
                        null
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
