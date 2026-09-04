package net.chriskatze.katzencraftmetals.menu;

import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.chriskatze.katzencraftmetals.block.entity.FoundryControllerBlockEntity;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTankNetwork;
import net.chriskatze.katzencraftmetals.fuel.FoundryFuels;
import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.chriskatze.katzencraftmetals.metal.MoltenMetalDefinition;
import net.chriskatze.katzencraftmetals.recipe.FoundryAlloyCatalog;
import net.chriskatze.katzencraftmetals.recipe.FoundryAlloyRecipe;
import net.chriskatze.katzencraftmetals.recipe.ModRecipes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;

import java.util.List;
import java.util.Optional;

public class FoundryControllerMenu extends AbstractContainerMenu {

    private static final int INPUT_SLOT_COUNT =
            FoundryControllerBlockEntity.INPUT_SLOT_COUNT;

    private static final int FUEL_SLOT_COUNT =
            FoundryControllerBlockEntity.FUEL_SLOT_COUNT;

    private static final int MACHINE_SLOT_COUNT =
            INPUT_SLOT_COUNT + FUEL_SLOT_COUNT;

    private static final int DATA_COUNT =
            FoundryControllerBlockEntity.DATA_COUNT;

    private static final int INPUT_MENU_START = 0;
    private static final int FUEL_MENU_START = 8;

    private static final int PLAYER_INVENTORY_START = 12;
    private static final int PLAYER_MAIN_INVENTORY_START = PLAYER_INVENTORY_START;
    private static final int PLAYER_MAIN_INVENTORY_END = 39;
    private static final int PLAYER_HOTBAR_START = 39;
    private static final int PLAYER_INVENTORY_END = 48;

    public static final int SELECT_METAL_BUTTON_BASE = 50_000;
    public static final int START_ALLOY_BUTTON_BASE = 100_000;
    public static final int STOP_ALLOY_BUTTON_ID = 200_000;

    private static final int ALLOY_BUTTON_QUANTITY_BASE = 100;

    public static final int MAX_ALLOY_BATCHES = 99;

    /*
     * These are the actual Minecraft Slot positions inside the authored
     * 18 x 18 frames. The frame itself begins one pixel above and left.
     */
    private static final int[] INPUT_SLOT_X = {
            41, 41, 60, 60, 79, 79, 98, 98
    };

    private static final int[] INPUT_SLOT_Y = {
            56, 75, 56, 75, 56, 75, 56, 75
    };

    private static final int FUEL_SLOT_START_X = 41;
    private static final int FUEL_SLOT_Y = 99;
    private static final int SLOT_SPACING = 19;

    private static final int PLAYER_INVENTORY_X = 7;
    private static final int PLAYER_INVENTORY_Y = 155;
    private static final int PLAYER_HOTBAR_Y = 209;

    private final FoundryControllerBlockEntity blockEntity;
    private final Container inputContainer;
    private final Container fuelContainer;
    private final ContainerData data;
    private final ContainerLevelAccess access;

    public FoundryControllerMenu(
            int containerId,
            Inventory playerInventory,
            FriendlyByteBuf extraData
    ) {
        this(
                containerId,
                playerInventory,
                getBlockEntity(playerInventory, extraData),
                new SimpleContainerData(DATA_COUNT)
        );
    }

    public FoundryControllerMenu(
            int containerId,
            Inventory playerInventory,
            FoundryControllerBlockEntity blockEntity
    ) {
        this(
                containerId,
                playerInventory,
                blockEntity,
                blockEntity.getData()
        );
    }

    private FoundryControllerMenu(
            int containerId,
            Inventory playerInventory,
            FoundryControllerBlockEntity blockEntity,
            ContainerData data
    ) {
        super(
                ModMenuTypes.FOUNDRY_CONTROLLER_MENU.get(),
                containerId
        );

        this.blockEntity = blockEntity;
        this.inputContainer = blockEntity.getInputInventory();
        this.fuelContainer = blockEntity.getFuelInventory();
        this.data = data;
        this.access = ContainerLevelAccess.create(
                blockEntity.getLevel(),
                blockEntity.getBlockPos()
        );

        checkContainerSize(inputContainer, INPUT_SLOT_COUNT);
        checkContainerSize(fuelContainer, FUEL_SLOT_COUNT);
        checkContainerDataCount(data, DATA_COUNT);

        inputContainer.startOpen(playerInventory.player);
        fuelContainer.startOpen(playerInventory.player);

        addDataSlots(data);
        addInputSlots();
        addFuelSlots();
        addPlayerInventory(playerInventory);
    }

    private void addInputSlots() {
        for (int slot = 0; slot < INPUT_SLOT_COUNT; slot++) {
            final int inputSlot = slot;

            addSlot(
                    new Slot(
                            inputContainer,
                            inputSlot,
                            INPUT_SLOT_X[inputSlot],
                            INPUT_SLOT_Y[inputSlot]
                    ) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return isInputSlotUnlocked(inputSlot)
                                    && blockEntity.canMelt(stack);
                        }

                        @Override
                        public boolean mayPickup(Player player) {
                            return isInputSlotUnlocked(inputSlot);
                        }

                        @Override
                        public boolean isActive() {
                            return isInputSlotUnlocked(inputSlot);
                        }
                    }
            );
        }
    }

    private void addFuelSlots() {
        for (int slot = 0; slot < FUEL_SLOT_COUNT; slot++) {
            final int fuelSlot = slot;

            addSlot(
                    new Slot(
                            fuelContainer,
                            fuelSlot,
                            FUEL_SLOT_START_X + fuelSlot * SLOT_SPACING,
                            FUEL_SLOT_Y
                    ) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return isFuelSlotUnlocked(fuelSlot)
                                    && FoundryFuels.isFuel(stack);
                        }

                        @Override
                        public boolean mayPickup(Player player) {
                            return isFuelSlotUnlocked(fuelSlot);
                        }

                        @Override
                        public boolean isActive() {
                            return isFuelSlotUnlocked(fuelSlot);
                        }
                    }
            );
        }
    }

    private static FoundryControllerBlockEntity getBlockEntity(
            Inventory playerInventory,
            FriendlyByteBuf extraData
    ) {
        var pos = extraData.readBlockPos();

        var blockEntity = playerInventory.player
                .level()
                .getBlockEntity(pos);

        if (blockEntity instanceof FoundryControllerBlockEntity controller) {
            return controller;
        }

        throw new IllegalStateException(
                "FoundryControllerBlockEntity missing at " + pos
        );
    }

    public int getProgress() {
        return data.get(0);
    }

    public int getMaxProgress() {
        return data.get(1);
    }

    public int getScaledProgress(int size) {
        int progress = getProgress();
        int maxProgress = getMaxProgress();

        if (progress <= 0 || maxProgress <= 0) {
            return 0;
        }

        return Math.max(
                0,
                Math.min(
                        size,
                        progress * size / maxProgress
                )
        );
    }

    public int getProgressPercent() {
        int maxProgress = getMaxProgress();

        if (maxProgress <= 0) {
            return 0;
        }

        return Math.max(
                0,
                Math.min(
                        100,
                        getProgress() * 100 / maxProgress
                )
        );
    }

    public int getBurnTimeRemaining() {
        return data.get(
                FoundryControllerBlockEntity.BURN_TIME_DATA_INDEX
        );
    }

    public int getMaxBurnTime() {
        return data.get(
                FoundryControllerBlockEntity.MAX_BURN_TIME_DATA_INDEX
        );
    }

    public int getScaledBurnTime(int size) {
        int burnTime = getBurnTimeRemaining();
        int maxBurnTime = getMaxBurnTime();

        if (burnTime <= 0 || maxBurnTime <= 0) {
            return 0;
        }

        return Math.max(
                0,
                Math.min(
                        size,
                        burnTime * size / maxBurnTime
                )
        );
    }

    public int getProcessingStatus() {
        return data.get(
                FoundryControllerBlockEntity.STATUS_DATA_INDEX
        );
    }

    public int getFoundryTier() {
        return data.get(
                FoundryControllerBlockEntity.TIER_DATA_INDEX
        );
    }

    public int getFoundryExperience() {
        return data.get(
                FoundryControllerBlockEntity.EXPERIENCE_DATA_INDEX
        );
    }

    public int getTierExperience() {
        return data.get(
                FoundryControllerBlockEntity.TIER_EXPERIENCE_DATA_INDEX
        );
    }

    public int getTierExperienceNeeded() {
        return Math.max(
                1,
                data.get(
                        FoundryControllerBlockEntity
                                .TIER_EXPERIENCE_NEEDED_DATA_INDEX
                )
        );
    }

    public int getScaledExperience(int width) {
        if (getFoundryTier() >= 4) {
            return width;
        }

        return Math.max(
                0,
                Math.min(
                        width,
                        getTierExperience()
                                * width
                                / getTierExperienceNeeded()
                )
        );
    }

    public int getUnlockedInputSlotCount() {
        return Math.min(
                INPUT_SLOT_COUNT,
                Math.max(
                        2,
                        getFoundryTier() * 2
                )
        );
    }

    public int getUnlockedFuelSlotCount() {
        return Math.min(
                FUEL_SLOT_COUNT,
                Math.max(
                        1,
                        getFoundryTier()
                )
        );
    }

    public boolean isInputSlotUnlocked(int slot) {
        return slot >= 0
                && slot < getUnlockedInputSlotCount();
    }

    public boolean isFuelSlotUnlocked(int slot) {
        return slot >= 0
                && slot < getUnlockedFuelSlotCount();
    }

    public int getActiveInputSlot() {
        return data.get(
                FoundryControllerBlockEntity.ACTIVE_INPUT_SLOT_DATA_INDEX
        );
    }

    public boolean hasFuelAvailable() {
        if (getBurnTimeRemaining() > 0) {
            return true;
        }

        for (int slot = 0; slot < getUnlockedFuelSlotCount(); slot++) {
            if (FoundryFuels.isFuel(fuelContainer.getItem(slot))) {
                return true;
            }
        }

        return false;
    }

    public ItemStack getInputStack() {
        int activeSlot = getActiveInputSlot();

        if (activeSlot >= 0 && activeSlot < INPUT_SLOT_COUNT) {
            return inputContainer.getItem(activeSlot);
        }

        for (int slot = 0; slot < getUnlockedInputSlotCount(); slot++) {
            ItemStack stack = inputContainer.getItem(slot);

            if (!stack.isEmpty()) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    public Optional<MoltenMetalDefinition> getInputMoltenMetalDefinition() {
        ItemStack input = getInputStack();

        if (input.isEmpty() || blockEntity.getLevel() == null) {
            return Optional.empty();
        }

        return blockEntity.getLevel()
                .getRecipeManager()
                .getRecipeFor(
                        ModRecipes.FOUNDRY_MELTING_TYPE.get(),
                        new SingleRecipeInput(input),
                        blockEntity.getLevel()
                )
                .map(holder -> holder.value().moltenMetal())
                .flatMap(ModMoltenMetals::get);
    }

    public int getMetalAmount(MoltenMetalDefinition definition) {
        int syncId = ModMoltenMetals.getSyncId(definition.id());

        if (syncId < 0) {
            return 0;
        }

        return data.get(
                FoundryControllerBlockEntity.METAL_DATA_START + syncId
        );
    }

    public Optional<MoltenMetalDefinition> getSelectedMetalDefinition() {
        return ModMoltenMetals.bySyncId(
                data.get(
                        FoundryControllerBlockEntity
                                .SELECTED_METAL_DATA_INDEX
                )
        );
    }

    public int getTotalMoltenAmount() {
        return data.get(
                FoundryControllerBlockEntity.TOTAL_AMOUNT_DATA_INDEX
        );
    }

    public int getTankCapacity() {
        return data.get(
                FoundryControllerBlockEntity.CAPACITY_DATA_INDEX
        );
    }

    public int getTankCount() {
        int capacity = getTankCapacity();

        if (capacity <= 0) {
            return 0;
        }

        return capacity / FoundryTankNetwork.TANK_CAPACITY;
    }

    public int getMaximumTankCount() {
        return FoundryTankNetwork.MAX_TANK_COUNT;
    }

    public List<RecipeHolder<FoundryAlloyRecipe>> getAlloyRecipes() {
        return blockEntity.getLevel() == null
                ? List.of()
                : FoundryAlloyCatalog.getRecipes(blockEntity.getLevel());
    }

    public boolean hasDiscoveredMetal(
            ResourceLocation metal
    ) {
        int syncId =
                ModMoltenMetals.getSyncId(metal);

        return syncId >= 0
                && data.get(
                FoundryControllerBlockEntity
                        .DISCOVERED_METAL_DATA_START
                        + syncId
        ) != 0;
    }

    public boolean isAlloyRecipeUnlocked(
            FoundryAlloyRecipe recipe
    ) {
        if (recipe == null || recipe.ingredients().isEmpty()) {
            return false;
        }

        for (var ingredient : recipe.ingredients()) {
            if (!hasDiscoveredMetal(ingredient.metal())) {
                return false;
            }
        }

        return true;
    }

    public int getMaxCraftableBatches(FoundryAlloyRecipe recipe) {
        if (
                recipe == null
                        || !isAlloyRecipeUnlocked(recipe)
                        || recipe.requiredTier() > getFoundryTier()
                        || blockEntity.getLevel() == null
                        || ModMoltenMetals.get(recipe.outputMetal()).isEmpty()
        ) {
            return 0;
        }

        int maximum = MAX_ALLOY_BATCHES;
        int totalInputPerBatch = 0;

        for (var ingredient : recipe.ingredients()) {
            Optional<MoltenMetalDefinition> definition =
                    ModMoltenMetals.get(ingredient.metal());

            if (definition.isEmpty()) {
                return 0;
            }

            int available = getMetalAmount(definition.get());

            maximum = Math.min(
                    maximum,
                    available / ingredient.amount()
            );

            totalInputPerBatch += ingredient.amount();
        }

        int netGrowthPerBatch =
                recipe.outputAmount() - totalInputPerBatch;

        if (netGrowthPerBatch > 0) {
            int free = Math.max(
                    0,
                    getTankCapacity() - getTotalMoltenAmount()
            );

            maximum = Math.min(
                    maximum,
                    free / netGrowthPerBatch
            );
        }

        return Math.max(0, maximum);
    }

    public boolean isAlloyJobActive() {
        return data.get(
                FoundryControllerBlockEntity.ALLOY_ACTIVE_DATA_INDEX
        ) != 0;
    }

    public int getActiveAlloyBatchCount() {
        return Math.max(
                0,
                data.get(
                        FoundryControllerBlockEntity
                                .ACTIVE_ALLOY_BATCH_COUNT_DATA_INDEX
                )
        );
    }

    public Optional<MoltenMetalDefinition> getActiveAlloyOutput() {
        return ModMoltenMetals.bySyncId(
                data.get(
                        FoundryControllerBlockEntity
                                .ACTIVE_ALLOY_OUTPUT_DATA_INDEX
                )
        );
    }

    public static int createSelectMetalButton(
            MoltenMetalDefinition definition
    ) {
        int syncId = ModMoltenMetals.getSyncId(definition.id());

        return syncId < 0
                ? -1
                : SELECT_METAL_BUTTON_BASE + syncId;
    }

    public static int createStartAlloyButton(
            int recipeIndex,
            int quantity
    ) {
        int normalizedQuantity = Math.max(
                1,
                Math.min(
                        MAX_ALLOY_BATCHES,
                        quantity
                )
        );

        return START_ALLOY_BUTTON_BASE
                + recipeIndex * ALLOY_BUTTON_QUANTITY_BASE
                + normalizedQuantity;
    }

    public static int createStopAlloyButton() {
        return STOP_ALLOY_BUTTON_ID;
    }

    @Override
    public boolean clickMenuButton(
            Player player,
            int buttonId
    ) {
        if (buttonId == STOP_ALLOY_BUTTON_ID) {
            return blockEntity.stopAlloy();
        }

        if (
                buttonId >= SELECT_METAL_BUTTON_BASE
                        && buttonId < START_ALLOY_BUTTON_BASE
        ) {
            int syncId =
                    buttonId - SELECT_METAL_BUTTON_BASE;

            return ModMoltenMetals.bySyncId(syncId)
                    .map(
                            definition ->
                                    blockEntity.setSelectedOutputMetal(
                                            definition.id()
                                    )
                    )
                    .orElse(false);
        }

        if (buttonId < START_ALLOY_BUTTON_BASE) {
            return false;
        }

        int encoded =
                buttonId - START_ALLOY_BUTTON_BASE;

        int recipeIndex =
                encoded / ALLOY_BUTTON_QUANTITY_BASE;

        int quantity =
                encoded % ALLOY_BUTTON_QUANTITY_BASE;

        if (
                quantity < 1
                        || quantity > MAX_ALLOY_BATCHES
        ) {
            return false;
        }

        List<RecipeHolder<FoundryAlloyRecipe>> recipes =
                getAlloyRecipes();

        if (
                recipeIndex < 0
                        || recipeIndex >= recipes.size()
        ) {
            return false;
        }

        FoundryAlloyRecipe recipe =
                recipes.get(recipeIndex).value();

        return isAlloyRecipeUnlocked(recipe)
                && blockEntity.startAlloy(
                recipeIndex,
                quantity
        );
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(
                access,
                player,
                ModBlocks.FOUNDRY_CONTROLLER.get()
        );
    }

    @Override
    public ItemStack quickMoveStack(
            Player player,
            int index
    ) {
        Slot slot = slots.get(index);

        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack originalStack = stack.copy();

        if (index < MACHINE_SLOT_COUNT) {
            if (
                    !moveItemStackTo(
                            stack,
                            PLAYER_INVENTORY_START,
                            PLAYER_INVENTORY_END,
                            true
                    )
            ) {
                return ItemStack.EMPTY;
            }
        } else if (FoundryFuels.isFuel(stack)) {
            if (
                    !moveItemStackTo(
                            stack,
                            FUEL_MENU_START,
                            FUEL_MENU_START
                                    + getUnlockedFuelSlotCount(),
                            false
                    )
            ) {
                return ItemStack.EMPTY;
            }
        } else if (blockEntity.canMelt(stack)) {
            if (
                    !moveItemStackTo(
                            stack,
                            INPUT_MENU_START,
                            INPUT_MENU_START
                                    + getUnlockedInputSlotCount(),
                            false
                    )
            ) {
                return ItemStack.EMPTY;
            }
        } else if (index >= PLAYER_MAIN_INVENTORY_START
                && index < PLAYER_MAIN_INVENTORY_END) {
            if (
                    !moveItemStackTo(
                            stack,
                            PLAYER_HOTBAR_START,
                            PLAYER_INVENTORY_END,
                            false
                    )
            ) {
                return ItemStack.EMPTY;
            }
        } else if (index >= PLAYER_HOTBAR_START
                && index < PLAYER_INVENTORY_END) {
            if (
                    !moveItemStackTo(
                            stack,
                            PLAYER_MAIN_INVENTORY_START,
                            PLAYER_MAIN_INVENTORY_END,
                            false
                    )
            ) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (stack.getCount() == originalStack.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, stack);
        return originalStack;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);

        inputContainer.stopOpen(player);
        fuelContainer.stopOpen(player);
    }

    private void addPlayerInventory(
            Inventory playerInventory
    ) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(
                        new Slot(
                                playerInventory,
                                column + row * 9 + 9,
                                PLAYER_INVENTORY_X + column * 18,
                                PLAYER_INVENTORY_Y + row * 18
                        )
                );
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(
                    new Slot(
                            playerInventory,
                            column,
                            PLAYER_INVENTORY_X + column * 18,
                            PLAYER_HOTBAR_Y
                    )
            );
        }
    }
}
