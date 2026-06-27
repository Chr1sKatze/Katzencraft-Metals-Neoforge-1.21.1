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

public class FoundryFaucetBlockEntity extends BlockEntity {

    public static final int TRANSFER_INTERVAL = 2;
    public static final int TRANSFER_AMOUNT = 1;

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

        if (faucet.pouring) {
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
        } else {
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

        faucet.transferTimer++;

        if (faucet.transferTimer < TRANSFER_INTERVAL) {
            return;
        }

        faucet.transferTimer = 0;

        Direction facing =
                state.getValue(FoundryFaucetBlock.FACING);

        BlockPos tankPosition =
                pos.relative(facing.getOpposite());

        BlockEntity tankBlockEntity =
                level.getBlockEntity(tankPosition);

        if (!(tankBlockEntity instanceof FoundryTankBlockEntity tank)) {
            faucet.stopPouring();
            return;
        }

        /*
         * Orphan Tank sections retain their liquid and visuals, but they
         * cannot operate Faucets until a Controller claims them again.
         */
        if (!tank.hasActiveController()) {
            faucet.stopPouring();
            return;
        }

        BlockEntity cauldronBlockEntity =
                level.getBlockEntity(pos.below());

        if (
                !(cauldronBlockEntity
                        instanceof CastingCauldronBlockEntity cauldron)
        ) {
            faucet.stopPouring();
            return;
        }

        ResourceLocation storedMetal =
                tank.getStoredMetal();

        if (storedMetal == null || tank.isEmpty()) {
            faucet.stopPouring();
            return;
        }

        if (cauldron.isFull()) {
            faucet.stopPouring();
            return;
        }

        if (!cauldron.canAccept(
                storedMetal,
                TRANSFER_AMOUNT
        )) {
            faucet.stopPouring();
            return;
        }

        int extracted =
                tank.extract(TRANSFER_AMOUNT);

        if (extracted != TRANSFER_AMOUNT) {
            faucet.stopPouring();
            return;
        }

        int inserted =
                cauldron.insert(
                        storedMetal,
                        extracted
                );

        if (inserted != extracted) {
            tank.insert(
                    storedMetal,
                    extracted
            );

            faucet.stopPouring();
            return;
        }

        if (cauldron.isFull() || tank.isEmpty()) {
            faucet.stopPouring();
        }
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
