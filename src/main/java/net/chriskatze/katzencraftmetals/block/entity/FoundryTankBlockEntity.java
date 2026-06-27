package net.chriskatze.katzencraftmetals.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class FoundryTankBlockEntity extends BlockEntity {

    /*
     * 108 units represent two complete metal blocks.
     *
     * 54 units = one block
     * 108 units = two blocks
     */
    public static final int CAPACITY = 108;

    @Nullable
    private ResourceLocation storedMetal;

    private int moltenAmount;

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
    // INSERTION
    // =========================

    public boolean canAccept(
            ResourceLocation metal,
            int amount
    ) {
        if (amount <= 0) {
            return false;
        }

        if (
                storedMetal != null
                        && !storedMetal.equals(metal)
        ) {
            return false;
        }

        return moltenAmount + amount <= CAPACITY;
    }

    /**
     * Inserts molten metal and returns the amount actually inserted.
     */
    public int insert(
            ResourceLocation metal,
            int amount
    ) {
        if (amount <= 0) {
            return 0;
        }

        if (
                storedMetal != null
                        && !storedMetal.equals(metal)
        ) {
            return 0;
        }

        int accepted = Math.min(
                amount,
                CAPACITY - moltenAmount
        );

        if (accepted <= 0) {
            return 0;
        }

        if (storedMetal == null) {
            storedMetal = metal;
        }

        moltenAmount += accepted;

        setChanged();
        syncToClient();

        return accepted;
    }

    // =========================
    // EXTRACTION
    // =========================

    /**
     * Extracts molten metal and returns the amount actually removed.
     */
    public int extract(
            int requestedAmount
    ) {
        if (
                requestedAmount <= 0
                        || moltenAmount <= 0
        ) {
            return 0;
        }

        int extracted = Math.min(
                requestedAmount,
                moltenAmount
        );

        moltenAmount -= extracted;

        if (moltenAmount <= 0) {
            moltenAmount = 0;
            storedMetal = null;
        }

        setChanged();
        syncToClient();

        return extracted;
    }

    // =========================
    // GETTERS
    // =========================

    public int getMoltenAmount() {
        return moltenAmount;
    }

    public int getCapacity() {
        return CAPACITY;
    }

    public float getFillPercentage() {
        return Mth.clamp(
                (float) moltenAmount / CAPACITY,
                0.0f,
                1.0f
        );
    }

    public boolean isEmpty() {
        return moltenAmount <= 0;
    }

    public boolean isFull() {
        return moltenAmount >= CAPACITY;
    }

    @Nullable
    public ResourceLocation getStoredMetal() {
        return storedMetal;
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

        storedMetal = null;
        moltenAmount = 0;

        if (tag.contains("StoredMetal")) {
            storedMetal =
                    ResourceLocation.tryParse(
                            tag.getString("StoredMetal")
                    );
        }

        moltenAmount = Mth.clamp(
                tag.getInt("MoltenAmount"),
                0,
                CAPACITY
        );

        /*
         * Prevent malformed data from containing an amount
         * without an associated metal type.
         */
        if (storedMetal == null) {
            moltenAmount = 0;
        }

        if (moltenAmount == 0) {
            storedMetal = null;
        }
    }
}
