package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.block.custom.CastingCauldronBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class CastingCauldronBlockEntity extends BlockEntity {

    /*
     * Six molten units represent one ingot.
     *
     * Nine ingots form one metal block:
     *
     * 6 × 9 = 54 molten units.
     */
    public static final int REQUIRED_MOLTEN_AMOUNT = 54;

    /*
     * 100 ticks = 5 seconds.
     *
     * Cooling begins only once the cauldron is completely full.
     */
    public static final int MAX_COOLING_PROGRESS = 100;

    private static final ResourceLocation IRON =
            ResourceLocation.fromNamespaceAndPath(
                    "minecraft",
                    "iron"
            );

    @Nullable
    private ResourceLocation storedMetal;

    private int moltenAmount;
    private int coolingProgress;
    private boolean cooled;

    public CastingCauldronBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                ModBlockEntities.CASTING_CAULDRON.get(),
                pos,
                state
        );
    }

    // =========================
    // TICKING
    // =========================

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            CastingCauldronBlockEntity cauldron
    ) {
        if (level.isClientSide()) {
            return;
        }

        /*
         * Ensures that the blockstate matches the saved contents
         * after loading an existing world.
         */
        cauldron.updateVisualState();

        /*
         * Empty and already-cooled cauldrons do nothing.
         */
        if (cauldron.isEmpty() || cauldron.cooled) {
            return;
        }

        /*
         * A partially filled cauldron does not cool yet.
         *
         * Cooling starts only after all 54 units have
         * entered the basin.
         */
        if (!cauldron.isFull()) {
            return;
        }

        cauldron.coolingProgress++;

        if (
                cauldron.coolingProgress
                        >= MAX_COOLING_PROGRESS
        ) {
            cauldron.coolingProgress =
                    MAX_COOLING_PROGRESS;

            cauldron.cooled = true;

            cauldron.updateVisualState();
            cauldron.setChanged();
            cauldron.syncToClient();

            return;
        }

        cauldron.setChanged();
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

        /*
         * Only iron is supported during this prototype.
         */
        if (!IRON.equals(metal)) {
            return false;
        }

        /*
         * A finished cast cannot accept more molten metal.
         */
        if (cooled) {
            return false;
        }

        /*
         * An empty cauldron accepts iron.
         *
         * A partially filled cauldron only accepts more of
         * the same metal.
         */
        if (
                storedMetal != null
                        && !storedMetal.equals(metal)
        ) {
            return false;
        }

        int remainingCapacity =
                REQUIRED_MOLTEN_AMOUNT - moltenAmount;

        /*
         * The full requested amount must fit.
         */
        return amount <= remainingCapacity;
    }

    public int insert(
            ResourceLocation metal,
            int amount
    ) {
        if (!canAccept(metal, amount)) {
            return 0;
        }

        if (storedMetal == null) {
            storedMetal = metal;
        }

        moltenAmount += amount;

        /*
         * The cauldron cannot be considered cooled while
         * new molten metal is being inserted.
         */
        coolingProgress = 0;
        cooled = false;

        updateVisualState();
        setChanged();
        syncToClient();

        return amount;
    }

    // =========================
    // RESULT
    // =========================

    public ItemStack getResultCopy() {
        if (!cooled) {
            return ItemStack.EMPTY;
        }

        if (!isFull()) {
            return ItemStack.EMPTY;
        }

        if (storedMetal == null) {
            return ItemStack.EMPTY;
        }

        if (IRON.equals(storedMetal)) {
            return new ItemStack(Items.IRON_BLOCK);
        }

        return ItemStack.EMPTY;
    }

    public ItemStack takeResult() {
        ItemStack result =
                getResultCopy();

        if (result.isEmpty()) {
            return ItemStack.EMPTY;
        }

        clear();

        return result;
    }

    private void clear() {
        storedMetal = null;
        moltenAmount = 0;
        coolingProgress = 0;
        cooled = false;

        updateVisualState();
        setChanged();
        syncToClient();
    }

    // =========================
    // GETTERS
    // =========================

    public boolean isEmpty() {
        return storedMetal == null
                || moltenAmount <= 0;
    }

    public boolean isFull() {
        return moltenAmount
                >= REQUIRED_MOLTEN_AMOUNT;
    }

    public int getRemainingCapacity() {
        return Math.max(
                0,
                REQUIRED_MOLTEN_AMOUNT - moltenAmount
        );
    }

    public boolean isCooled() {
        return cooled;
    }

    public int getMoltenAmount() {
        return moltenAmount;
    }

    public int getCoolingProgress() {
        return coolingProgress;
    }

    @Nullable
    public ResourceLocation getStoredMetal() {
        return storedMetal;
    }

    // =========================
    // VISUAL BLOCKSTATE
    // =========================

    private void updateVisualState() {
        if (level == null || level.isClientSide()) {
            return;
        }

        BlockState currentState =
                getBlockState();

        if (
                !currentState.hasProperty(
                        CastingCauldronBlock.CAST_STATE
                )
        ) {
            return;
        }

        CastingCauldronBlock.CastState desiredState;

        if (isEmpty()) {
            desiredState =
                    CastingCauldronBlock.CastState.EMPTY;
        } else if (cooled) {
            desiredState =
                    CastingCauldronBlock.CastState.COOLED;
        } else {
            /*
             * Both partially filled and completely filled
             * hot metal use the molten state.
             */
            desiredState =
                    CastingCauldronBlock.CastState.MOLTEN;
        }

        CastingCauldronBlock.CastState currentVisualState =
                currentState.getValue(
                        CastingCauldronBlock.CAST_STATE
                );

        if (currentVisualState == desiredState) {
            return;
        }

        level.setBlock(
                worldPosition,
                currentState.setValue(
                        CastingCauldronBlock.CAST_STATE,
                        desiredState
                ),
                Block.UPDATE_ALL
        );
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
        if (level == null || level.isClientSide()) {
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

        tag.putInt(
                "CoolingProgress",
                coolingProgress
        );

        tag.putBoolean(
                "Cooled",
                cooled
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
        coolingProgress = 0;
        cooled = false;

        if (tag.contains("StoredMetal")) {
            storedMetal =
                    ResourceLocation.tryParse(
                            tag.getString("StoredMetal")
                    );
        }

        moltenAmount = Mth.clamp(
                tag.getInt("MoltenAmount"),
                0,
                REQUIRED_MOLTEN_AMOUNT
        );

        coolingProgress = Mth.clamp(
                tag.getInt("CoolingProgress"),
                0,
                MAX_COOLING_PROGRESS
        );

        cooled =
                tag.getBoolean("Cooled");

        /*
         * Remove invalid or unsupported stored metals.
         */
        if (
                storedMetal == null
                        || !IRON.equals(storedMetal)
                        || moltenAmount <= 0
        ) {
            storedMetal = null;
            moltenAmount = 0;
            coolingProgress = 0;
            cooled = false;

            return;
        }

        /*
         * A partially filled basin cannot already be cooling
         * or contain a finished cast.
         */
        if (!isFull()) {
            coolingProgress = 0;
            cooled = false;
        }

        /*
         * Normalize a finished cast.
         */
        if (cooled) {
            coolingProgress =
                    MAX_COOLING_PROGRESS;
        }
    }
}