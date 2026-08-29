package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.menu.FoundryControllerMenu;
import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.chriskatze.katzencraftmetals.recipe.FoundryAlloyRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class FoundryControllerBlockEntity
        extends BlockEntity
        implements MenuProvider {

    public static final ResourceLocation MOLTEN_IRON =
            ModMoltenMetals.IRON.id();

    public static final int INPUT_SLOT = 0;
    public static final int INPUT_SLOT_COUNT = 8;

    public static final int FUEL_SLOT_COUNT =
            FoundryControllerFuelSystem.FUEL_SLOT_COUNT;

    public static final int SLOT_COUNT =
            INPUT_SLOT_COUNT
                    + FUEL_SLOT_COUNT;

    public static final int MAX_PROGRESS = 20;

    public static final int COAL_BURN_TIME =
            FoundryControllerFuelSystem.COAL_BURN_TIME;

    public static final int MOLTEN_IRON_PER_RAW_IRON =
            ModMoltenMetals.IRON.unitsPerOre();

    private UUID controllerId =
            UUID.randomUUID();

    private final FoundryControllerProcessing processing =
            new FoundryControllerProcessing(this);

    private final FoundryControllerFuelSystem fuelSystem =
            new FoundryControllerFuelSystem(this);

    private final FoundryControllerProgression progression =
            new FoundryControllerProgression();

    private final FoundryControllerAlloying alloying =
            new FoundryControllerAlloying(this);

    private final FoundryControllerMetalDiscovery metalDiscovery =
            new FoundryControllerMetalDiscovery(this);

    private final SimpleContainer inputInventory =
            new FoundryControllerInputInventory(this);

    public static final int METAL_DATA_START = 2;

    public static final int METAL_DATA_COUNT =
            ModMoltenMetals.values().size();

    public static final int SELECTED_METAL_DATA_INDEX =
            METAL_DATA_START
                    + METAL_DATA_COUNT;

    public static final int TOTAL_AMOUNT_DATA_INDEX =
            SELECTED_METAL_DATA_INDEX
                    + 1;

    public static final int CAPACITY_DATA_INDEX =
            TOTAL_AMOUNT_DATA_INDEX
                    + 1;

    public static final int BURN_TIME_DATA_INDEX =
            CAPACITY_DATA_INDEX
                    + 1;

    public static final int MAX_BURN_TIME_DATA_INDEX =
            BURN_TIME_DATA_INDEX
                    + 1;

    public static final int TIER_DATA_INDEX =
            MAX_BURN_TIME_DATA_INDEX
                    + 1;

    public static final int EXPERIENCE_DATA_INDEX =
            TIER_DATA_INDEX
                    + 1;

    public static final int TIER_EXPERIENCE_DATA_INDEX =
            EXPERIENCE_DATA_INDEX
                    + 1;

    public static final int TIER_EXPERIENCE_NEEDED_DATA_INDEX =
            TIER_EXPERIENCE_DATA_INDEX
                    + 1;

    public static final int ACTIVE_INPUT_SLOT_DATA_INDEX =
            TIER_EXPERIENCE_NEEDED_DATA_INDEX
                    + 1;

    public static final int STATUS_DATA_INDEX =
            ACTIVE_INPUT_SLOT_DATA_INDEX
                    + 1;

    public static final int ACTIVE_ALLOY_OUTPUT_DATA_INDEX =
            STATUS_DATA_INDEX
                    + 1;

    public static final int ALLOY_ACTIVE_DATA_INDEX =
            ACTIVE_ALLOY_OUTPUT_DATA_INDEX
                    + 1;

    public static final int ACTIVE_ALLOY_BATCH_COUNT_DATA_INDEX =
            ALLOY_ACTIVE_DATA_INDEX
                    + 1;

    public static final int DISCOVERED_METAL_DATA_START =
            ACTIVE_ALLOY_BATCH_COUNT_DATA_INDEX
                    + 1;

    public static final int DATA_COUNT =
            DISCOVERED_METAL_DATA_START
                    + METAL_DATA_COUNT;

    private final ContainerData data =
            new FoundryControllerMenuData(this);

    public FoundryControllerBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                ModBlockEntities.FOUNDRY_CONTROLLER.get(),
                pos,
                state
        );
    }

    // =========================
    // CONTROLLER / TANK NETWORK
    // =========================

    public UUID getControllerId() {
        return controllerId;
    }

    void setControllerIdForPersistence(
            UUID controllerId
    ) {
        this.controllerId =
                controllerId != null
                        ? controllerId
                        : UUID.randomUUID();
    }

    FoundryControllerMetalDiscovery getMetalDiscoveryForPersistence() {
        return metalDiscovery;
    }

    FoundryControllerProcessing getProcessingForPersistence() {
        return processing;
    }

    FoundryControllerProgression getProgressionForPersistence() {
        return progression;
    }

    FoundryControllerAlloying getAlloyingForPersistence() {
        return alloying;
    }

    public Direction getFacing() {
        return FoundryControllerNetwork.getFacing(this);
    }

    public boolean isValidTankAttachmentPosition(
            BlockPos tankPos
    ) {
        return FoundryControllerNetwork
                .isValidTankAttachmentPosition(
                        this,
                        tankPos
                );
    }

    public boolean canOwnTankLayout(
            Set<BlockPos> tankPositions
    ) {
        return FoundryControllerNetwork
                .canOwnTankLayout(
                        this,
                        tankPositions
                );
    }

    public List<BlockPos> getValidTankAttachmentPositions() {
        return FoundryControllerNetwork
                .getValidTankAttachmentPositions(this);
    }

    @Nullable
    public FoundryTankNetwork getOwnedTankNetwork() {
        return FoundryControllerNetwork
                .getOwnedTankNetwork(this);
    }

    public boolean ensureTankNetwork() {
        return FoundryControllerNetwork
                .ensureTankNetwork(this);
    }

    public boolean hasDiscoveredMoltenMetal(
            ResourceLocation metal
    ) {
        return metalDiscovery.hasDiscovered(
                metal
        );
    }

    public boolean isAlloyRecipeUnlocked(
            FoundryAlloyRecipe recipe
    ) {
        return metalDiscovery.isAlloyRecipeUnlocked(
                recipe
        );
    }

    private void discoverCurrentTankMetals() {
        metalDiscovery.discoverCurrentTankMetals();
    }

    public void releaseFoundry() {
        alloying.cancelAndRefund();
        FoundryControllerNetwork.releaseFoundry(this);
    }

    // =========================
    // PROCESSING / OUTPUT
    // =========================

    public boolean canMelt(
            ItemStack stack
    ) {
        return processing.canMelt(stack);
    }

    @Nullable
    public ResourceLocation getSelectedOutputMetal() {
        return processing.getSelectedOutputMetal();
    }

    public boolean setSelectedOutputMetal(
            ResourceLocation metal
    ) {
        return processing.setSelectedOutputMetal(metal);
    }

    @Nullable
    public ResourceLocation getSelectedOutputMetalOrDefault(
            FoundryTankNetwork network
    ) {
        return processing
                .getSelectedOutputMetalOrDefault(network);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            FoundryControllerBlockEntity controller
    ) {
        if (!level.isClientSide()) {
            controller.discoverCurrentTankMetals();
        }

        if (controller.alloying.hasActiveJob()) {
            controller.alloying.tick(
                    level,
                    pos
            );
        } else {
            controller.processing.tick(
                    level,
                    pos
            );
        }

        /*
         * Melting, alloying, or an intake hatch may have inserted a new metal or
         * new process item during this tick.
         */
        if (!level.isClientSide()) {
            FoundryTankIntakeHatch.processOpenHatches(controller);
            controller.discoverCurrentTankMetals();
        }
    }

    // =========================
    // MENU / INVENTORIES
    // =========================

    @Override
    public Component getDisplayName() {
        return Component.translatable(
                "block.katzencraftmetals.foundry_controller"
        );
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(
            int containerId,
            Inventory playerInventory,
            Player player
    ) {
        return new FoundryControllerMenu(
                containerId,
                playerInventory,
                this
        );
    }

    public SimpleContainer getInputInventory() {
        return inputInventory;
    }

    public SimpleContainer getFuelInventory() {
        return fuelSystem.getInventory();
    }

    FoundryControllerFuelSystem getFuelSystem() {
        return fuelSystem;
    }

    public boolean startAlloy(
            int recipeIndex,
            int batchCount
    ) {
        return alloying.start(
                recipeIndex,
                batchCount
        );
    }

    public boolean stopAlloy() {
        return alloying.stop();
    }

    public boolean isAlloying() {
        return alloying.hasActiveJob();
    }

    public ContainerData getData() {
        return data;
    }

    int getMenuProgress() {
        return alloying.hasActiveJob()
                ? alloying.getProgress()
                : processing.getProgress();
    }

    int getMenuMaxProgress() {
        return alloying.hasActiveJob()
                ? alloying.getMaxProgress()
                : processing.getMaxProgress();
    }

    int getMenuActiveInputSlot() {
        return alloying.hasActiveJob()
                ? -1
                : processing.getActiveInputSlot();
    }

    int getMenuStatusCode() {
        return alloying.hasActiveJob()
                ? alloying.getStatusCode()
                : processing.getStatusCode();
    }

    @Nullable
    ResourceLocation getActiveAlloyOutputMetal() {
        return alloying.getOutputMetal();
    }

    int getActiveAlloyBatchCount() {
        return alloying.hasActiveJob()
                ? alloying.getBatchCount()
                : 0;
    }

    int getFoundryExperienceIntoTier() {
        return progression.getExperienceIntoTier();
    }

    int getFoundryExperienceNeededForTier() {
        return progression.getExperienceNeededForTier();
    }

    void setMenuProgressFromData(
            int value
    ) {
        processing.setProgressFromMenuData(
                value
        );
    }

    void setMenuMaxProgressFromData(
            int value
    ) {
        processing.setMaxProgressFromMenuData(
                value
        );
    }

    public int getProgress() {
        return processing.getProgress();
    }

    public int getMaxProgress() {
        return processing.getMaxProgress();
    }

    public int getBurnTimeRemaining() {
        return fuelSystem.getBurnTimeRemaining();
    }

    public int getMaxBurnTime() {
        return fuelSystem.getMaxBurnTime();
    }

    public boolean isBurning() {
        return fuelSystem.isBurning();
    }

    public boolean hasAvailableFuel() {
        return fuelSystem.hasAvailableFuel();
    }

    public int getStoredCoalCount() {
        return fuelSystem.getStoredCoalCount();
    }

    public int getFoundryTier() {
        return progression.getTier();
    }

    public int getFoundryExperience() {
        return progression.getExperience();
    }

    public int getUnlockedInputSlotCount() {
        return Math.min(
                INPUT_SLOT_COUNT,
                progression.getUnlockedInputSlots()
        );
    }

    public int getUnlockedFuelSlotCount() {
        return Math.min(
                FUEL_SLOT_COUNT,
                progression.getUnlockedFuelSlots()
        );
    }

    public boolean isInputSlotUnlocked(
            int slot
    ) {
        return slot >= 0
                && slot < getUnlockedInputSlotCount();
    }

    public boolean isFuelSlotUnlocked(
            int slot
    ) {
        return slot >= 0
                && slot < getUnlockedFuelSlotCount();
    }

    public boolean addFoundryExperience(
            int amount
    ) {
        boolean tierChanged =
                progression.addExperience(amount);

        if (amount > 0) {
            setChanged();
        }

        return tierChanged;
    }

    @Nullable
    public ResourceLocation getActiveMoltenMetal() {
        return processing.getActiveMoltenMetal();
    }

    public int getActiveMoltenAmount() {
        return processing.getActiveMoltenAmount();
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

        FoundryControllerPersistence.save(
                this,
                tag,
                registries
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

        FoundryControllerPersistence.load(
                this,
                tag,
                registries
        );
    }
}
