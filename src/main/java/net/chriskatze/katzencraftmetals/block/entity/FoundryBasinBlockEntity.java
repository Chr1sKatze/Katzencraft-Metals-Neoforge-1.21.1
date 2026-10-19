package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.block.custom.FoundryBasinBlock;
import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class FoundryBasinBlockEntity
        extends BlockEntity
        implements WorldlyContainer {

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
     * Cooling begins only once the basin is completely full.
     */
    public static final int MAX_COOLING_PROGRESS = 100;

    private static final int OUTPUT_SLOT = 0;
    private static final int[] OUTPUT_SLOTS = new int[]{OUTPUT_SLOT};

    @Nullable
    private ResourceLocation storedMetal;

    private int moltenAmount;
    private int coolingProgress;
    private boolean cooled;

    public FoundryBasinBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        this(ModBlockEntities.FOUNDRY_BASIN.get(), pos, state);
    }

    protected FoundryBasinBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type,
                                     BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // =========================
    // TICKING
    // =========================

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            FoundryBasinBlockEntity basin
    ) {
        if (level.isClientSide()) {
            return;
        }

        basin.updateVisualState();

        if (
                basin.isEmpty()
                        || !basin.isFull()
                        || basin.cooled
        ) {
            return;
        }

        basin.coolingProgress++;

        if (
                basin.coolingProgress
                        >= MAX_COOLING_PROGRESS
        ) {
            basin.coolingProgress =
                    MAX_COOLING_PROGRESS;

            basin.cooled = true;
            basin.updateVisualState();
        }

        basin.setChanged();
        basin.syncToClient();
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
         * Every registered molten metal may be cast.
         */
        if (!ModMoltenMetals.contains(metal)) {
            return false;
        }

        if (cooled) {
            return false;
        }

        /*
         * An empty Basin accepts any registered metal.
         *
         * A partially filled Basin only accepts more of the same metal.
         */
        if (
                storedMetal != null
                        && !storedMetal.equals(metal)
        ) {
            return false;
        }

        int remainingCapacity =
                getRequiredMoltenAmount() - moltenAmount;

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
        onMoltenInserted();

        coolingProgress = 0;
        cooled = false;

        updateVisualState();
        setChanged();
        syncToClient();

        return amount;
    }

    // =========================
    // HOPPER / SIDED OUTPUT
    // =========================

    protected void onMoltenInserted() {
        // Specialized casting targets may notify clients after a successful transfer.
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public ItemStack getItem(
            int slot
    ) {
        if (slot != OUTPUT_SLOT) {
            return ItemStack.EMPTY;
        }

        return getResultCopy();
    }

    @Override
    public ItemStack removeItem(
            int slot,
            int amount
    ) {
        if (
                slot != OUTPUT_SLOT
                        || amount <= 0
        ) {
            return ItemStack.EMPTY;
        }

        ItemStack result =
                getResultCopy();

        if (
                result.isEmpty()
                        || amount < result.getCount()
        ) {
            return ItemStack.EMPTY;
        }

        clear();

        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(
            int slot
    ) {
        if (slot != OUTPUT_SLOT) {
            return ItemStack.EMPTY;
        }

        ItemStack result =
                getResultCopy();

        if (result.isEmpty()) {
            return ItemStack.EMPTY;
        }

        clear();

        return result;
    }

    @Override
    public void setItem(
            int slot,
            ItemStack stack
    ) {
        /*
         * The Foundry Basin is output-only for item automation.
         * Molten metal still enters exclusively through Faucet insertion.
         */
    }

    @Override
    public boolean canPlaceItem(
            int slot,
            ItemStack stack
    ) {
        return false;
    }

    @Override
    public boolean stillValid(
            Player player
    ) {
        return level != null
                && level.getBlockEntity(
                worldPosition
        ) == this
                && player.distanceToSqr(
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5
        ) <= 64.0;
    }

    @Override
    public void clearContent() {
        clear();
    }

    @Override
    public int[] getSlotsForFace(
            Direction side
    ) {
        return OUTPUT_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(
            int slot,
            ItemStack stack,
            @Nullable Direction direction
    ) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(
            int slot,
            ItemStack stack,
            Direction direction
    ) {
        return slot == OUTPUT_SLOT
                && !getResultCopy().isEmpty();
    }

    // =========================
    // RESULT
    // =========================

    public ItemStack getResultCopy() {
        if (
                !cooled
                        || !isFull()
                        || storedMetal == null
        ) {
            return ItemStack.EMPTY;
        }

        return createResult(storedMetal);
    }

    protected ItemStack createResult(ResourceLocation metal) {
        return ModMoltenMetals.get(metal).map(definition -> definition.createCastResult())
                .orElse(ItemStack.EMPTY);
    }

    public int getRequiredMoltenAmount() {
        return REQUIRED_MOLTEN_AMOUNT;
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
                >= getRequiredMoltenAmount();
    }

    public int getRemainingCapacity() {
        return Math.max(
                0,
                getRequiredMoltenAmount() - moltenAmount
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
        if (
                level == null
                        || level.isClientSide()
        ) {
            return;
        }

        BlockState currentState =
                getBlockState();

        if (
                !currentState.hasProperty(
                        FoundryBasinBlock.CAST_STATE
                )
        ) {
            return;
        }

        FoundryBasinBlock.CastState desiredState;

        if (isEmpty()) {
            desiredState =
                    FoundryBasinBlock.CastState.EMPTY;
        } else if (cooled) {
            desiredState =
                    FoundryBasinBlock.CastState.COOLED;
        } else {
            desiredState =
                    FoundryBasinBlock.CastState.MOLTEN;
        }

        FoundryBasinBlock.CastState currentVisualState =
                currentState.getValue(
                        FoundryBasinBlock.CAST_STATE
                );

        if (currentVisualState == desiredState) {
            return;
        }

        level.setBlock(
                worldPosition,
                currentState.setValue(
                        FoundryBasinBlock.CAST_STATE,
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
                        getRequiredMoltenAmount()
                );

        coolingProgress =
                Mth.clamp(
                        tag.getInt(
                                "CoolingProgress"
                        ),
                        0,
                        MAX_COOLING_PROGRESS
                );

        cooled =
                tag.getBoolean(
                        "Cooled"
                );

        /*
         * Remove invalid or no-longer-supported stored metals.
         */
        if (
                storedMetal == null
                        || !ModMoltenMetals.contains(
                        storedMetal
                )
                        || moltenAmount <= 0
        ) {
            storedMetal = null;
            moltenAmount = 0;
            coolingProgress = 0;
            cooled = false;

            return;
        }

        if (!isFull()) {
            coolingProgress = 0;
            cooled = false;
        }

        if (cooled) {
            coolingProgress =
                    MAX_COOLING_PROGRESS;
        }
    }
}
