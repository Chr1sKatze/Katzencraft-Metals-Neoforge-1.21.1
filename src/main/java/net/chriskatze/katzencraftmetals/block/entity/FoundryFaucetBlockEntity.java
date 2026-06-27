package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.block.custom.FoundryFaucetBlock;
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

    /*
     * A Casting Cauldron may be one, two, or three blocks below the Faucet.
     */
    public static final int MAX_CAULDRON_DISTANCE = 3;

    private static final float MIN_SOURCE_AMOUNT =
            0.0001f;

    public static final int STREAM_ANIMATION_INTERVAL = 2;
    public static final int STREAM_ANIMATION_STEPS = 8;

    private boolean pouring;
    private int transferTimer;
    private int streamAnimationStep;
    private int streamAnimationTimer;

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

                if (faucet.streamAnimationTimer >= STREAM_ANIMATION_INTERVAL) {
                    faucet.streamAnimationTimer = 0;
                    faucet.streamAnimationStep--;

                    faucet.setChanged();
                    faucet.syncToClient();
                }
            }

            return;
        }

        /*
         * Validate the complete pouring route every tick, including while the
         * stream is extending. This prevents a Faucet from visually continuing
         * when its source layer becomes empty or its Cauldron is removed.
         */
        PouringContext context =
                resolvePouringContext(
                        level,
                        pos,
                        state
                );

        if (context == null) {
            faucet.stopPouring();
            return;
        }

        if (faucet.streamAnimationStep < STREAM_ANIMATION_STEPS) {
            faucet.streamAnimationTimer++;

            if (faucet.streamAnimationTimer >= STREAM_ANIMATION_INTERVAL) {
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
                context.tank()
                        .extract(
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
            context.tank()
                    .insert(
                            context.metal(),
                            extracted
                    );

            faucet.stopPouring();
            return;
        }

        /*
         * The network may still contain molten metal below this Faucet.
         * Stop specifically when the liquid surface has fallen below the
         * attached Tank layer.
         */
        if (
                context.cauldron().isFull()
                        || !hasMoltenAtFaucetHeight(
                        context.tank()
                )
        ) {
            faucet.stopPouring();
        }
    }

    // =========================
    // SMART SOURCE / TARGET LOOKUP
    // =========================

    /**
     * Returns true when molten metal currently occupies the horizontal Tank
     * layer to which this Faucet is attached.
     *
     * The Tank network distributes liquid from its lowest layer upward, so
     * this naturally gives the desired behavior:
     *
     * - bottom Faucet works while the bottom layer contains liquid
     * - middle Faucet works only after liquid reaches the middle layer
     * - top Faucet works only after liquid reaches the top layer
     */
    public static boolean hasMoltenAtFaucetHeight(
            FoundryTankBlockEntity tank
    ) {
        return tank.getStoredMetal() != null
                && tank.getLocalVisualMoltenAmount()
                > MIN_SOURCE_AMOUNT;
    }

    /**
     * Finds the first Casting Cauldron in the vertical column below a Faucet.
     *
     * Valid distances are one through three blocks. Every intermediate block
     * must have an empty collision shape so the molten stream is unobstructed.
     */
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

            /*
             * A solid block terminates the search. A Cauldron farther below
             * it cannot receive a stream through the obstruction.
             */
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
            BlockState faucetState
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

        if (
                !tank.hasActiveController()
                        || !hasMoltenAtFaucetHeight(
                        tank
                )
        ) {
            return null;
        }

        ResourceLocation storedMetal =
                tank.getStoredMetal();

        if (storedMetal == null) {
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
                        storedMetal,
                        TRANSFER_AMOUNT
                )
        ) {
            return null;
        }

        return new PouringContext(
                tank,
                cauldron,
                storedMetal
        );
    }

    public record CauldronTarget(
            CastingCauldronBlockEntity cauldron,
            int distance
    ) {
    }

    private record PouringContext(
            FoundryTankBlockEntity tank,
            CastingCauldronBlockEntity cauldron,
            ResourceLocation metal
    ) {
    }

    // =========================
    // POURING STATE
    // =========================

    public void startPouring() {
        setPouring(true);
    }

    public void stopPouring() {
        setPouring(false);
    }

    public void setPouring(boolean pouring) {
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
                tag.getBoolean("Pouring");

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
    }
}
