package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.block.custom.FoundryControllerBlock;
import net.chriskatze.katzencraftmetals.menu.FoundryControllerMenu;
import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.chriskatze.katzencraftmetals.recipe.FoundryMeltingRecipe;
import net.chriskatze.katzencraftmetals.recipe.ModRecipes;
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
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class FoundryControllerBlockEntity
        extends BlockEntity
        implements MenuProvider {

    /*
     * Compatibility constants retained for existing callers.
     *
     * The Controller no longer uses these values to decide what can melt.
     * That information now comes from FoundryMeltingRecipe.
     */
    public static final ResourceLocation MOLTEN_IRON =
            ModMoltenMetals.IRON.id();

    public static final int INPUT_SLOT = 0;
    public static final int SLOT_COUNT = 1;

    public static final int MAX_PROGRESS = 20;
    public static final int MOLTEN_IRON_PER_RAW_IRON =
            ModMoltenMetals.IRON.unitsPerOre();

    /*
     * Every Controller owns one persistent UUID.
     *
     * Tanks and Fuel Chambers assigned to this Foundry store the same UUID.
     */
    private UUID controllerId =
            UUID.randomUUID();

    private final SimpleContainer inputInventory =
            new SimpleContainer(SLOT_COUNT) {

                @Override
                public boolean canPlaceItem(
                        int slot,
                        ItemStack stack
                ) {
                    return slot == INPUT_SLOT
                            && canMelt(stack);
                }

                @Override
                public void setChanged() {
                    super.setChanged();
                    FoundryControllerBlockEntity.this.setChanged();
                }
            };

    private int progress;
    private int maxProgress =
            MAX_PROGRESS;

    /*
     * The active recipe signature prevents progress from carrying across
     * two different Foundry recipes when the input is changed midway.
     */
    @Nullable
    private ResourceLocation activeMoltenMetal;

    private int activeMoltenAmount;

    private final ContainerData data =
            new ContainerData() {

                @Override
                public int get(
                        int index
                ) {
                    return switch (index) {
                        case 0 -> progress;
                        case 1 -> maxProgress;
                        case 2 -> {
                            FoundryTankNetwork network =
                                    getOwnedTankNetwork();

                            yield network != null
                                    ? network.getMoltenAmount()
                                    : 0;
                        }
                        default -> 0;
                    };
                }

                @Override
                public void set(
                        int index,
                        int value
                ) {
                    switch (index) {
                        case 0 ->
                                progress =
                                        value;

                        case 1 ->
                                maxProgress =
                                        Math.max(
                                                1,
                                                value
                                        );

                        default -> {
                        }
                    }
                }

                @Override
                public int getCount() {
                    return 3;
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
    // CONTROLLER ID / FACING
    // =========================

    public UUID getControllerId() {
        return controllerId;
    }

    public Direction getFacing() {
        return getBlockState().getValue(
                FoundryControllerBlock.FACING
        );
    }

    /**
     * A Controller connects to Tanks directly behind it, to its left,
     * or to its right. A Tank directly in front is never a valid attachment.
     */
    public boolean isValidTankAttachmentPosition(
            BlockPos tankPos
    ) {
        if (tankPos.getY() != worldPosition.getY()) {
            return false;
        }

        int deltaX =
                tankPos.getX()
                        - worldPosition.getX();

        int deltaZ =
                tankPos.getZ()
                        - worldPosition.getZ();

        if (Math.abs(deltaX) + Math.abs(deltaZ) != 1) {
            return false;
        }

        int forwardDistance =
                deltaX * getFacing().getStepX()
                        + deltaZ * getFacing().getStepZ();

        return forwardDistance <= 0;
    }

    /**
     * The complete Tank layout may extend behind or beside the Controller,
     * but no Tank in the owned network may lie in front of it.
     */
    public boolean canOwnTankLayout(
            Set<BlockPos> tankPositions
    ) {
        if (tankPositions.isEmpty()) {
            return false;
        }

        boolean touchesController =
                false;

        Direction facing =
                getFacing();

        for (BlockPos tankPos : tankPositions) {
            int deltaX =
                    tankPos.getX()
                            - worldPosition.getX();

            int deltaZ =
                    tankPos.getZ()
                            - worldPosition.getZ();

            int forwardDistance =
                    deltaX * facing.getStepX()
                            + deltaZ * facing.getStepZ();

            if (forwardDistance > 0) {
                return false;
            }

            if (isValidTankAttachmentPosition(tankPos)) {
                touchesController =
                        true;
            }
        }

        return touchesController;
    }

    public List<BlockPos> getValidTankAttachmentPositions() {
        Direction facing =
                getFacing();

        return List.of(
                worldPosition.relative(
                        facing.getOpposite()
                ),
                worldPosition.relative(
                        facing.getClockWise()
                ),
                worldPosition.relative(
                        facing.getCounterClockWise()
                )
        );
    }

    // =========================
    // TANK NETWORK
    // =========================

    @Nullable
    public FoundryTankNetwork getOwnedTankNetwork() {
        if (level == null) {
            return null;
        }

        FoundryTankNetwork best =
                null;

        Set<BlockPos> alreadyChecked =
                new HashSet<>();

        for (
                BlockPos attachmentPos :
                getValidTankAttachmentPositions()
        ) {
            BlockEntity blockEntity =
                    level.getBlockEntity(
                            attachmentPos
                    );

            if (
                    !(blockEntity
                            instanceof FoundryTankBlockEntity tank)
            ) {
                continue;
            }

            FoundryTankNetwork network =
                    tank.getNetwork();

            if (
                    network == null
                            || !controllerId.equals(
                            network.getOwnerId()
                    )
                            || !canOwnTankLayout(
                            network.getTankPositions()
                    )
            ) {
                continue;
            }

            BlockPos networkKey =
                    network.getTankPositions()
                            .stream()
                            .min(
                                    Comparator
                                            .comparingInt(
                                                    (BlockPos pos) ->
                                                            pos.getY()
                                            )
                                            .thenComparingInt(
                                                    pos ->
                                                            pos.getX()
                                            )
                                            .thenComparingInt(
                                                    pos ->
                                                            pos.getZ()
                                            )
                            )
                            .orElse(
                                    attachmentPos
                            );

            if (!alreadyChecked.add(networkKey)) {
                continue;
            }

            if (
                    best == null
                            || network.getTankCount()
                            > best.getTankCount()
            ) {
                best =
                        network;
            }
        }

        return best;
    }

    /**
     * Restores an existing owned network or claims the largest valid
     * unassigned layout touching the Controller on its back/left/right side.
     */
    public boolean ensureTankNetwork() {
        if (
                level == null
                        || level.isClientSide()
        ) {
            return false;
        }

        if (getOwnedTankNetwork() != null) {
            return true;
        }

        return FoundryTankNetwork
                .claimLargestUnassignedLayoutForController(
                        level,
                        this
                )
                != null;
    }

    /**
     * Releases every local machine owned by this Controller.
     */
    public void releaseFoundry() {
        FoundryTankNetwork network =
                getOwnedTankNetwork();

        for (
                FuelChamberBlockEntity fuelChamber :
                collectNearbyFuelChambers(network)
        ) {
            if (
                    controllerId.equals(
                            fuelChamber.getControllerId()
                    )
            ) {
                fuelChamber.setControllerId(null);
            }
        }

        if (network != null) {
            network.releaseOwnership();
        }
    }

    // =========================
    // FOUNDRY RECIPES
    // =========================

    public boolean canMelt(
            ItemStack stack
    ) {
        return findMeltingRecipe(stack)
                .isPresent();
    }

    private Optional<FoundryMeltingRecipe> findMeltingRecipe(
            ItemStack stack
    ) {
        if (
                level == null
                        || stack.isEmpty()
        ) {
            return Optional.empty();
        }

        return level.getRecipeManager()
                .getRecipeFor(
                        ModRecipes.FOUNDRY_MELTING_TYPE.get(),
                        new SingleRecipeInput(stack),
                        level
                )
                .map(
                        holder ->
                                holder.value()
                )
                .filter(
                        recipe ->
                                ModMoltenMetals.contains(
                                        recipe.moltenMetal()
                                )
                );
    }

    private boolean matchesActiveRecipe(
            FoundryMeltingRecipe recipe
    ) {
        return Objects.equals(
                activeMoltenMetal,
                recipe.moltenMetal()
        )
                && activeMoltenAmount
                == recipe.moltenAmount()
                && maxProgress
                == recipe.processingTime();
    }

    private void selectActiveRecipe(
            FoundryMeltingRecipe recipe
    ) {
        if (matchesActiveRecipe(recipe)) {
            return;
        }

        progress = 0;

        activeMoltenMetal =
                recipe.moltenMetal();

        activeMoltenAmount =
                recipe.moltenAmount();

        maxProgress =
                recipe.processingTime();

        setChanged();
    }

    private void clearActiveRecipe() {
        boolean changed =
                progress != 0
                        || maxProgress != MAX_PROGRESS
                        || activeMoltenMetal != null
                        || activeMoltenAmount != 0;

        progress = 0;
        maxProgress = MAX_PROGRESS;
        activeMoltenMetal = null;
        activeMoltenAmount = 0;

        if (changed) {
            setChanged();
        }
    }

    // =========================
    // FUEL CHAMBER
    // =========================

    @Nullable
    private FuelChamberBlockEntity getConnectedFuelChamber(
            FoundryTankNetwork network
    ) {
        List<FuelChamberBlockEntity> fuelChambers =
                collectNearbyFuelChambers(network);

        fuelChambers.sort(
                Comparator
                        .comparingLong(
                                (FuelChamberBlockEntity fuelChamber) ->
                                        distanceSquared(
                                                worldPosition,
                                                fuelChamber.getBlockPos()
                                        )
                        )
                        .thenComparingInt(
                                fuelChamber ->
                                        fuelChamber.getBlockPos()
                                                .getY()
                        )
                        .thenComparingInt(
                                fuelChamber ->
                                        fuelChamber.getBlockPos()
                                                .getX()
                        )
                        .thenComparingInt(
                                fuelChamber ->
                                        fuelChamber.getBlockPos()
                                                .getZ()
                        )
        );

        /*
         * Preserve an existing valid assignment first.
         */
        for (
                FuelChamberBlockEntity fuelChamber :
                fuelChambers
        ) {
            if (
                    controllerId.equals(
                            fuelChamber.getControllerId()
                    )
            ) {
                return fuelChamber;
            }
        }

        /*
         * Unassigned or stale Fuel Chambers resolve their surrounding
         * Controllers automatically. Ambiguous chambers remain unused.
         */
        for (
                FuelChamberBlockEntity fuelChamber :
                fuelChambers
        ) {
            fuelChamber.tryAutoAssign(null);

            if (
                    controllerId.equals(
                            fuelChamber.getControllerId()
                    )
            ) {
                return fuelChamber;
            }
        }

        return null;
    }

    private List<FuelChamberBlockEntity> collectNearbyFuelChambers(
            @Nullable FoundryTankNetwork network
    ) {
        if (level == null) {
            return List.of();
        }

        Set<BlockPos> candidatePositions =
                new HashSet<>();

        /*
         * A Fuel Chamber may sit on any of the six faces of the Controller:
         * above, below, north, south, east, or west.
         */
        for (Direction direction : Direction.values()) {
            candidatePositions.add(
                    worldPosition.relative(direction)
            );
        }

        /*
         * It may alternatively touch any Tank belonging to the network,
         * again on any of the six faces.
         */
        if (network != null) {
            for (
                    BlockPos tankPos :
                    network.getTankPositions()
            ) {
                for (
                        Direction direction :
                        Direction.values()
                ) {
                    candidatePositions.add(
                            tankPos.relative(direction)
                    );
                }
            }
        }

        List<FuelChamberBlockEntity> result =
                new ArrayList<>();

        for (BlockPos candidatePos : candidatePositions) {
            BlockEntity blockEntity =
                    level.getBlockEntity(
                            candidatePos
                    );

            if (
                    blockEntity
                            instanceof FuelChamberBlockEntity fuelChamber
            ) {
                result.add(fuelChamber);
            }
        }

        return result;
    }

    // =========================
    // SERVER TICK
    // =========================

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            FoundryControllerBlockEntity controller
    ) {
        if (level.isClientSide()) {
            return;
        }

        if (!controller.ensureTankNetwork()) {
            controller.resetProgress();
            return;
        }

        FoundryTankNetwork network =
                controller.getOwnedTankNetwork();

        if (network == null) {
            controller.resetProgress();
            return;
        }

        ItemStack inputStack =
                controller.inputInventory.getItem(
                        INPUT_SLOT
                );

        Optional<FoundryMeltingRecipe> recipeOptional =
                controller.findMeltingRecipe(
                        inputStack
                );

        if (recipeOptional.isEmpty()) {
            controller.clearActiveRecipe();
            return;
        }

        FoundryMeltingRecipe recipe =
                recipeOptional.get();

        controller.selectActiveRecipe(recipe);

        FuelChamberBlockEntity fuelChamber =
                controller.getConnectedFuelChamber(
                        network
                );

        if (fuelChamber == null) {
            controller.resetProgress();
            return;
        }

        FoundryTankBlockEntity tank =
                controller.getAnyTank(
                        network
                );

        if (tank == null) {
            controller.resetProgress();
            return;
        }

        /*
         * A full Tank or incompatible stored metal pauses the Controller
         * without wasting fuel or losing its current progress.
         */
        if (
                !tank.canAccept(
                        recipe.moltenMetal(),
                        recipe.moltenAmount()
                )
        ) {
            return;
        }

        if (!fuelChamber.supplyBurnTick()) {
            return;
        }

        controller.progress++;

        if (
                controller.progress
                        >= controller.maxProgress
        ) {
            controller.finishMelting(
                    tank,
                    recipe
            );
        }

        controller.setChanged();
    }

    @Nullable
    private FoundryTankBlockEntity getAnyTank(
            FoundryTankNetwork network
    ) {
        if (level == null) {
            return null;
        }

        List<BlockPos> positions =
                new ArrayList<>(
                        network.getTankPositions()
                );

        positions.sort(
                Comparator
                        .comparingInt(
                                (BlockPos tankPos) ->
                                        tankPos.getY()
                        )
                        .thenComparingInt(
                                tankPos ->
                                        tankPos.getX()
                        )
                        .thenComparingInt(
                                tankPos ->
                                        tankPos.getZ()
                        )
        );

        for (BlockPos tankPos : positions) {
            BlockEntity blockEntity =
                    level.getBlockEntity(
                            tankPos
                    );

            if (
                    blockEntity
                            instanceof FoundryTankBlockEntity tank
            ) {
                return tank;
            }
        }

        return null;
    }

    private void finishMelting(
            FoundryTankBlockEntity tank,
            FoundryMeltingRecipe expectedRecipe
    ) {
        ItemStack inputStack =
                inputInventory.getItem(
                        INPUT_SLOT
                );

        Optional<FoundryMeltingRecipe> currentRecipeOptional =
                findMeltingRecipe(
                        inputStack
                );

        if (currentRecipeOptional.isEmpty()) {
            clearActiveRecipe();
            return;
        }

        FoundryMeltingRecipe currentRecipe =
                currentRecipeOptional.get();

        if (
                !matchesActiveRecipe(currentRecipe)
                        || !sameRecipeResult(
                        expectedRecipe,
                        currentRecipe
                )
        ) {
            selectActiveRecipe(currentRecipe);
            return;
        }

        if (
                !tank.canAccept(
                        currentRecipe.moltenMetal(),
                        currentRecipe.moltenAmount()
                )
        ) {
            return;
        }

        int inserted =
                tank.insert(
                        currentRecipe.moltenMetal(),
                        currentRecipe.moltenAmount()
                );

        if (
                inserted
                        != currentRecipe.moltenAmount()
        ) {
            return;
        }

        inputStack.shrink(1);
        progress = 0;

        inputInventory.setChanged();
        setChanged();
    }

    private static boolean sameRecipeResult(
            FoundryMeltingRecipe first,
            FoundryMeltingRecipe second
    ) {
        return first.moltenAmount()
                == second.moltenAmount()
                && first.processingTime()
                == second.processingTime()
                && first.moltenMetal()
                .equals(
                        second.moltenMetal()
                );
    }

    private void resetProgress() {
        if (progress == 0) {
            return;
        }

        progress = 0;
        setChanged();
    }

    private static long distanceSquared(
            BlockPos first,
            BlockPos second
    ) {
        long deltaX =
                first.getX()
                        - second.getX();

        long deltaY =
                first.getY()
                        - second.getY();

        long deltaZ =
                first.getZ()
                        - second.getZ();

        return deltaX * deltaX
                + deltaY * deltaY
                + deltaZ * deltaZ;
    }

    // =========================
    // MENU
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

    // =========================
    // GETTERS
    // =========================

    public SimpleContainer getInputInventory() {
        return inputInventory;
    }

    public ContainerData getData() {
        return data;
    }

    public int getProgress() {
        return progress;
    }

    public int getMaxProgress() {
        return maxProgress;
    }

    @Nullable
    public ResourceLocation getActiveMoltenMetal() {
        return activeMoltenMetal;
    }

    public int getActiveMoltenAmount() {
        return activeMoltenAmount;
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

        tag.put(
                "InputInventory",
                inputInventory.createTag(
                        registries
                )
        );

        tag.putInt(
                "Progress",
                progress
        );

        tag.putInt(
                "MaxProgress",
                maxProgress
        );

        if (activeMoltenMetal != null) {
            tag.putString(
                    "ActiveMoltenMetal",
                    activeMoltenMetal.toString()
            );
        }

        tag.putInt(
                "ActiveMoltenAmount",
                activeMoltenAmount
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

        if (tag.contains("ControllerId")) {
            try {
                controllerId =
                        UUID.fromString(
                                tag.getString(
                                        "ControllerId"
                                )
                        );
            } catch (IllegalArgumentException ignored) {
                controllerId =
                        UUID.randomUUID();
            }
        } else {
            controllerId =
                    UUID.randomUUID();
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

        maxProgress =
                tag.contains("MaxProgress")
                        ? Math.max(
                        1,
                        tag.getInt(
                                "MaxProgress"
                        )
                )
                        : MAX_PROGRESS;

        progress =
                Math.max(
                        0,
                        Math.min(
                                tag.getInt("Progress"),
                                maxProgress
                        )
                );

        activeMoltenMetal =
                null;

        if (tag.contains("ActiveMoltenMetal")) {
            activeMoltenMetal =
                    ResourceLocation.tryParse(
                            tag.getString(
                                    "ActiveMoltenMetal"
                            )
                    );
        }

        activeMoltenAmount =
                Math.max(
                        0,
                        tag.getInt(
                                "ActiveMoltenAmount"
                        )
                );

        if (
                activeMoltenMetal == null
                        || activeMoltenAmount <= 0
                        || !ModMoltenMetals.contains(
                        activeMoltenMetal
                )
        ) {
            progress = 0;
            maxProgress = MAX_PROGRESS;
            activeMoltenMetal = null;
            activeMoltenAmount = 0;
        }
    }
}
