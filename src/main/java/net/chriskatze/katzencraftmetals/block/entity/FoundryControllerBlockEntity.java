package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.menu.FoundryControllerMenu;
import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.chriskatze.katzencraftmetals.recipe.FoundryAlloyRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
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

import java.util.HashSet;
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

    /**
     * Every registered molten metal that has existed in this controller's
     * attached tank network at least once.
     */
    private final Set<ResourceLocation> discoveredMoltenMetals =
            new HashSet<>();

    private final SimpleContainer inputInventory =
            new SimpleContainer(INPUT_SLOT_COUNT) {

                @Override
                public boolean canPlaceItem(
                        int slot,
                        ItemStack stack
                ) {
                    return isInputSlotUnlocked(slot)
                            && processing.canMelt(stack);
                }

                @Override
                public void setChanged() {
                    super.setChanged();
                    FoundryControllerBlockEntity.this.setChanged();
                }
            };

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
            new ContainerData() {

                @Override
                public int get(
                        int index
                ) {
                    if (index == 0) {
                        return alloying.hasActiveJob()
                                ? alloying.getProgress()
                                : processing.getProgress();
                    }

                    if (index == 1) {
                        return alloying.hasActiveJob()
                                ? alloying.getMaxProgress()
                                : processing.getMaxProgress();
                    }

                    FoundryTankNetwork network =
                            getOwnedTankNetwork();

                    if (
                            index >= METAL_DATA_START
                                    && index < SELECTED_METAL_DATA_INDEX
                    ) {
                        int syncId =
                                index
                                        - METAL_DATA_START;

                        ResourceLocation metal =
                                ModMoltenMetals.bySyncId(syncId)
                                        .map(definition -> definition.id())
                                        .orElse(null);

                        return network != null
                                && metal != null
                                ? network.getMoltenAmount(metal)
                                : 0;
                    }

                    if (index == SELECTED_METAL_DATA_INDEX) {
                        ResourceLocation selected =
                                network != null
                                        ? processing
                                        .getSelectedOutputMetalOrDefault(network)
                                        : null;

                        return selected != null
                                ? ModMoltenMetals.getSyncId(selected)
                                : -1;
                    }

                    if (index == TOTAL_AMOUNT_DATA_INDEX) {
                        return network != null
                                ? network.getTotalMoltenAmount()
                                : 0;
                    }

                    if (index == CAPACITY_DATA_INDEX) {
                        return network != null
                                ? network.getCapacity()
                                : 0;
                    }

                    if (index == BURN_TIME_DATA_INDEX) {
                        return fuelSystem.getBurnTimeRemaining();
                    }

                    if (index == MAX_BURN_TIME_DATA_INDEX) {
                        return fuelSystem.getMaxBurnTime();
                    }

                    if (index == TIER_DATA_INDEX) {
                        return progression.getTier();
                    }

                    if (index == EXPERIENCE_DATA_INDEX) {
                        return progression.getExperience();
                    }

                    if (index == TIER_EXPERIENCE_DATA_INDEX) {
                        return progression.getExperienceIntoTier();
                    }

                    if (index == TIER_EXPERIENCE_NEEDED_DATA_INDEX) {
                        return progression.getExperienceNeededForTier();
                    }

                    if (index == ACTIVE_INPUT_SLOT_DATA_INDEX) {
                        return alloying.hasActiveJob()
                                ? -1
                                : processing.getActiveInputSlot();
                    }

                    if (index == STATUS_DATA_INDEX) {
                        return alloying.hasActiveJob()
                                ? alloying.getStatusCode()
                                : processing.getStatusCode();
                    }

                    if (index == ACTIVE_ALLOY_OUTPUT_DATA_INDEX) {
                        ResourceLocation output =
                                alloying.getOutputMetal();

                        return output == null
                                ? -1
                                : ModMoltenMetals.getSyncId(output);
                    }

                    if (index == ALLOY_ACTIVE_DATA_INDEX) {
                        return alloying.hasActiveJob()
                                ? 1
                                : 0;
                    }

                    if (index == ACTIVE_ALLOY_BATCH_COUNT_DATA_INDEX) {
                        return alloying.hasActiveJob()
                                ? alloying.getBatchCount()
                                : 0;
                    }

                    if (
                            index >= DISCOVERED_METAL_DATA_START
                                    && index < DATA_COUNT
                    ) {
                        int syncId =
                                index
                                        - DISCOVERED_METAL_DATA_START;

                        ResourceLocation metal =
                                ModMoltenMetals.bySyncId(syncId)
                                        .map(definition -> definition.id())
                                        .orElse(null);

                        return metal != null
                                && discoveredMoltenMetals.contains(metal)
                                ? 1
                                : 0;
                    }

                    return 0;
                }

                @Override
                public void set(
                        int index,
                        int value
                ) {
                    if (index == 0) {
                        processing.setProgressFromMenuData(value);
                        return;
                    }

                    if (index == 1) {
                        processing.setMaxProgressFromMenuData(value);
                    }
                }

                @Override
                public int getCount() {
                    return DATA_COUNT;
                }
            };

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
        return metal != null
                && discoveredMoltenMetals.contains(metal);
    }

    public boolean isAlloyRecipeUnlocked(
            FoundryAlloyRecipe recipe
    ) {
        if (recipe == null || recipe.ingredients().isEmpty()) {
            return false;
        }

        for (var ingredient : recipe.ingredients()) {
            if (
                    !hasDiscoveredMoltenMetal(
                            ingredient.metal()
                    )
            ) {
                return false;
            }
        }

        return true;
    }

    private void discoverCurrentTankMetals() {
        FoundryTankNetwork network =
                getOwnedTankNetwork();

        if (network == null) {
            return;
        }

        network.ensureMoltenContentsMigrated();

        boolean changed = false;

        for (
                var entry :
                network.getMoltenContents().entrySet()
        ) {
            if (
                    entry.getValue() > 0
                            && ModMoltenMetals.contains(
                            entry.getKey()
                    )
            ) {
                changed |=
                        discoveredMoltenMetals.add(
                                entry.getKey()
                        );
            }
        }

        if (changed) {
            setChanged();
        }
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
         * Melting or alloying may have inserted a new metal during this tick.
         */
        if (!level.isClientSide()) {
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

        tag.putString(
                "ControllerId",
                controllerId.toString()
        );

        CompoundTag discoveredTag =
                new CompoundTag();

        for (
                ResourceLocation metal :
                discoveredMoltenMetals
        ) {
            discoveredTag.putBoolean(
                    metal.toString(),
                    true
            );
        }

        tag.put(
                "DiscoveredMoltenMetals",
                discoveredTag
        );

        tag.put(
                "InputInventory",
                inputInventory.createTag(registries)
        );

        fuelSystem.save(
                tag,
                registries
        );

        processing.save(
                tag,
                registries
        );

        progression.save(tag);
        alloying.save(tag);
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

        if (tag.contains("ControllerId")) {
            try {
                controllerId =
                        UUID.fromString(
                                tag.getString("ControllerId")
                        );
            } catch (IllegalArgumentException ignored) {
                controllerId =
                        UUID.randomUUID();
            }
        } else {
            controllerId =
                    UUID.randomUUID();
        }

        discoveredMoltenMetals.clear();

        if (
                tag.contains(
                        "DiscoveredMoltenMetals",
                        Tag.TAG_COMPOUND
                )
        ) {
            CompoundTag discoveredTag =
                    tag.getCompound(
                            "DiscoveredMoltenMetals"
                    );

            for (String key : discoveredTag.getAllKeys()) {
                ResourceLocation metal =
                        ResourceLocation.tryParse(key);

                if (
                        metal != null
                                && discoveredTag.getBoolean(key)
                                && ModMoltenMetals.contains(metal)
                ) {
                    discoveredMoltenMetals.add(metal);
                }
            }
        }

        inputInventory.removeAllItems();

        if (
                tag.contains(
                        "InputInventory",
                        Tag.TAG_LIST
                )
        ) {
            inputInventory.fromTag(
                    tag.getList(
                            "InputInventory",
                            Tag.TAG_COMPOUND
                    ),
                    registries
            );
        }

        fuelSystem.load(
                tag,
                registries
        );

        progression.load(tag);

        if (!tag.contains("FoundryProgressionVersion")) {
            progression.migrateLegacyFuelAccess(
                    fuelSystem.getHighestOccupiedSlot()
            );
        }

        processing.load(
                tag,
                registries
        );

        alloying.load(tag);
    }
}
