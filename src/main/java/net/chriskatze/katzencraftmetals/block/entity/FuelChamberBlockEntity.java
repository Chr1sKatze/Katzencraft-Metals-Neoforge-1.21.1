package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.menu.FuelChamberMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Temporary compatibility BlockEntity for old separate Fuel Chambers.
 *
 * New Foundries store fuel directly in FoundryControllerBlockEntity. Existing
 * chambers remain loadable for one checkpoint so their coal and active burn
 * cycle can migrate safely into the Controller.
 */
public class FuelChamberBlockEntity
        extends BlockEntity
        implements MenuProvider {

    public static final int SLOT_COUNT = 3;
    public static final int COAL_BURN_TIME = 1600;

    @Nullable
    private UUID controllerId;

    private final SimpleContainer fuelInventory =
            new SimpleContainer(SLOT_COUNT) {

                @Override
                public boolean canPlaceItem(
                        int slot,
                        ItemStack stack
                ) {
                    return stack.is(
                            Items.COAL
                    );
                }

                @Override
                public void setChanged() {
                    super.setChanged();
                    FuelChamberBlockEntity.this.setChanged();
                }
            };

    private int burnTimeRemaining;
    private int maxBurnTime;

    public FuelChamberBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                ModBlockEntities.FUEL_CHAMBER.get(),
                pos,
                state
        );
    }

    @Nullable
    public UUID getControllerId() {
        return controllerId;
    }

    public void setControllerId(
            @Nullable UUID controllerId
    ) {
        if (Objects.equals(
                this.controllerId,
                controllerId
        )) {
            return;
        }

        this.controllerId =
                controllerId;

        setChanged();
    }

    public void tryAutoAssign(
            @Nullable BlockPos clickedAgainstPosition
    ) {
        if (
                level == null
                        || level.isClientSide()
        ) {
            return;
        }

        Map<UUID, FoundryControllerBlockEntity> candidates =
                collectCandidateControllers();

        if (
                controllerId != null
                        && candidates.containsKey(
                        controllerId
                )
        ) {
            return;
        }

        FoundryControllerBlockEntity preferred =
                findPreferredController(
                        clickedAgainstPosition,
                        candidates
                );

        if (preferred != null) {
            setControllerId(
                    preferred.getControllerId()
            );

            return;
        }

        if (candidates.size() == 1) {
            setControllerId(
                    candidates.keySet()
                            .iterator()
                            .next()
            );

            return;
        }

        setControllerId(
                null
        );
    }

    private Map<UUID, FoundryControllerBlockEntity>
    collectCandidateControllers() {
        Map<UUID, FoundryControllerBlockEntity> candidates =
                new LinkedHashMap<>();

        if (level == null) {
            return candidates;
        }

        for (Direction direction : Direction.values()) {
            BlockPos neighborPos =
                    worldPosition.relative(
                            direction
                    );

            BlockEntity blockEntity =
                    level.getBlockEntity(
                            neighborPos
                    );

            if (
                    blockEntity
                            instanceof FoundryControllerBlockEntity controller
            ) {
                candidates.putIfAbsent(
                        controller.getControllerId(),
                        controller
                );

                continue;
            }

            FoundryTankNetwork network =
                    FoundryTankNetwork.find(
                            level,
                            neighborPos
                    );

            if (network != null) {
                FoundryControllerBlockEntity controller =
                        network.getAttachedController();

                candidates.putIfAbsent(
                        controller.getControllerId(),
                        controller
                );
            }
        }

        return candidates;
    }

    @Nullable
    private FoundryControllerBlockEntity findPreferredController(
            @Nullable BlockPos clickedAgainstPosition,
            Map<UUID, FoundryControllerBlockEntity> candidates
    ) {
        if (
                level == null
                        || clickedAgainstPosition == null
        ) {
            return null;
        }

        BlockEntity clickedBlockEntity =
                level.getBlockEntity(
                        clickedAgainstPosition
                );

        if (
                clickedBlockEntity
                        instanceof FoundryControllerBlockEntity controller
                        && candidates.containsKey(
                        controller.getControllerId()
                )
        ) {
            return controller;
        }

        FoundryTankNetwork network =
                FoundryTankNetwork.find(
                        level,
                        clickedAgainstPosition
                );

        if (network != null) {
            FoundryControllerBlockEntity controller =
                    network.getAttachedController();

            if (candidates.containsKey(
                    controller.getControllerId()
            )) {
                return controller;
            }
        }

        return null;
    }

    public boolean supplyBurnTick() {
        if (
                level == null
                        || level.isClientSide()
        ) {
            return false;
        }

        if (
                burnTimeRemaining <= 0
                        && !consumeCoal()
        ) {
            return false;
        }

        burnTimeRemaining--;
        setChanged();

        return true;
    }

    private boolean consumeCoal() {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack stack =
                    fuelInventory.getItem(
                            slot
                    );

            if (!stack.is(Items.COAL)) {
                continue;
            }

            fuelInventory.removeItem(
                    slot,
                    1
            );

            burnTimeRemaining =
                    COAL_BURN_TIME;

            maxBurnTime =
                    COAL_BURN_TIME;

            setChanged();
            return true;
        }

        return false;
    }

    /**
     * Removes the currently active burn cycle so it can be moved into the new
     * Controller-owned fuel system without duplicating fuel.
     */
    public int takeBurnTimeForControllerMigration() {
        int transferred =
                burnTimeRemaining;

        if (
                burnTimeRemaining == 0
                        && maxBurnTime == 0
        ) {
            return 0;
        }

        burnTimeRemaining =
                0;

        maxBurnTime =
                0;

        setChanged();

        return transferred;
    }

    public boolean hasAvailableFuel() {
        if (burnTimeRemaining > 0) {
            return true;
        }

        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (
                    fuelInventory.getItem(slot)
                            .is(Items.COAL)
            ) {
                return true;
            }
        }

        return false;
    }

    public boolean isBurning() {
        return burnTimeRemaining > 0;
    }

    public int getBurnTimeRemaining() {
        return burnTimeRemaining;
    }

    public int getMaxBurnTime() {
        return maxBurnTime;
    }

    public int getStoredCoalCount() {
        int coalCount =
                0;

        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack stack =
                    fuelInventory.getItem(
                            slot
                    );

            if (stack.is(Items.COAL)) {
                coalCount +=
                        stack.getCount();
            }
        }

        return coalCount;
    }

    public SimpleContainer getFuelInventory() {
        return fuelInventory;
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.saveAdditional(
                tag,
                registries
        );

        if (controllerId != null) {
            tag.putString(
                    "ControllerId",
                    controllerId.toString()
            );
        }

        tag.put(
                "FuelInventory",
                fuelInventory.createTag(
                        registries
                )
        );

        tag.putInt(
                "BurnTimeRemaining",
                burnTimeRemaining
        );

        tag.putInt(
                "MaxBurnTime",
                maxBurnTime
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

        controllerId =
                null;

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
                        null;
            }
        }

        fuelInventory.removeAllItems();

        if (
                tag.contains(
                        "FuelInventory",
                        Tag.TAG_LIST
                )
        ) {
            fuelInventory.fromTag(
                    tag.getList(
                            "FuelInventory",
                            Tag.TAG_COMPOUND
                    ),
                    registries
            );
        }

        burnTimeRemaining =
                Math.max(
                        0,
                        tag.getInt(
                                "BurnTimeRemaining"
                        )
                );

        maxBurnTime =
                Math.max(
                        burnTimeRemaining,
                        tag.getInt(
                                "MaxBurnTime"
                        )
                );
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(
                "block.katzencraftmetals.fuel_chamber"
        );
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(
            int containerId,
            Inventory playerInventory,
            Player player
    ) {
        return new FuelChamberMenu(
                containerId,
                playerInventory,
                this
        );
    }
}
