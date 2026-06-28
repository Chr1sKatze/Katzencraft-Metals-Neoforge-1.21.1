package net.chriskatze.katzencraftmetals.menu;

import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.chriskatze.katzencraftmetals.block.entity.FoundryControllerBlockEntity;
import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.chriskatze.katzencraftmetals.metal.MoltenMetalDefinition;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class FoundryControllerMenu
        extends AbstractContainerMenu {

    private static final int CONTROLLER_SLOT_COUNT =
            FoundryControllerBlockEntity.SLOT_COUNT;

    private static final int DATA_COUNT = 4;

    /*
     * Menu slot indices:
     *
     * 0      Controller input
     * 1-27   Player inventory
     * 28-36  Player hotbar
     */
    private static final int PLAYER_INVENTORY_START = 1;
    private static final int PLAYER_INVENTORY_END = 28;

    private static final int HOTBAR_START = 28;
    private static final int HOTBAR_END = 37;

    private final FoundryControllerBlockEntity blockEntity;
    private final Container container;
    private final ContainerData data;
    private final ContainerLevelAccess access;

    /*
     * Client constructor.
     */
    public FoundryControllerMenu(
            int containerId,
            Inventory playerInventory,
            FriendlyByteBuf extraData
    ) {
        this(
                containerId,
                playerInventory,
                getBlockEntity(
                        playerInventory,
                        extraData
                ),
                new SimpleContainerData(
                        DATA_COUNT
                )
        );
    }

    /*
     * Server constructor used by the block entity.
     */
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

        this.blockEntity =
                blockEntity;

        this.container =
                blockEntity.getInputInventory();

        this.data =
                data;

        this.access =
                ContainerLevelAccess.create(
                        blockEntity.getLevel(),
                        blockEntity.getBlockPos()
                );

        checkContainerSize(
                container,
                CONTROLLER_SLOT_COUNT
        );

        checkContainerDataCount(
                data,
                DATA_COUNT
        );

        container.startOpen(
                playerInventory.player
        );

        addDataSlots(data);

        /*
         * Any item with a FoundryMeltingRecipe can use the input slot.
         */
        this.addSlot(
                new Slot(
                        container,
                        FoundryControllerBlockEntity.INPUT_SLOT,
                        80,
                        35
                ) {

                    @Override
                    public boolean mayPlace(
                            ItemStack stack
                    ) {
                        return blockEntity.canMelt(
                                stack
                        );
                    }
                }
        );

        addPlayerInventory(
                playerInventory
        );

        addPlayerHotbar(
                playerInventory
        );
    }

    private static FoundryControllerBlockEntity getBlockEntity(
            Inventory playerInventory,
            FriendlyByteBuf extraData
    ) {
        var pos =
                extraData.readBlockPos();

        var blockEntity =
                playerInventory.player
                        .level()
                        .getBlockEntity(pos);

        if (
                blockEntity
                        instanceof FoundryControllerBlockEntity controller
        ) {
            return controller;
        }

        throw new IllegalStateException(
                "FoundryControllerBlockEntity missing at "
                        + pos
        );
    }

    public int getScaledProgress(
            int width
    ) {
        int progress =
                data.get(0);

        int maxProgress =
                data.get(1);

        if (
                progress <= 0
                        || maxProgress <= 0
        ) {
            return 0;
        }

        return progress
                * width
                / maxProgress;
    }

    public int getMoltenAmount() {
        return data.get(2);
    }

    public Optional<MoltenMetalDefinition> getStoredMetalDefinition() {
        return ModMoltenMetals.bySyncId(
                data.get(3)
        );
    }

    @Override
    public boolean stillValid(
            Player player
    ) {
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
        Slot slot =
                this.slots.get(index);

        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack =
                slot.getItem();

        ItemStack originalStack =
                stack.copy();

        if (index == 0) {
            /*
             * Controller -> player inventory
             */
            if (
                    !moveItemStackTo(
                            stack,
                            PLAYER_INVENTORY_START,
                            HOTBAR_END,
                            true
                    )
            ) {
                return ItemStack.EMPTY;
            }
        } else if (
                blockEntity.canMelt(stack)
        ) {
            /*
             * Valid Foundry melting input -> Controller
             */
            if (
                    !moveItemStackTo(
                            stack,
                            0,
                            1,
                            false
                    )
            ) {
                return ItemStack.EMPTY;
            }
        } else if (
                index >= PLAYER_INVENTORY_START
                        && index < PLAYER_INVENTORY_END
        ) {
            /*
             * Player inventory -> hotbar
             */
            if (
                    !moveItemStackTo(
                            stack,
                            HOTBAR_START,
                            HOTBAR_END,
                            false
                    )
            ) {
                return ItemStack.EMPTY;
            }
        } else if (
                index >= HOTBAR_START
                        && index < HOTBAR_END
        ) {
            /*
             * Hotbar -> player inventory
             */
            if (
                    !moveItemStackTo(
                            stack,
                            PLAYER_INVENTORY_START,
                            PLAYER_INVENTORY_END,
                            false
                    )
            ) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(
                    ItemStack.EMPTY
            );
        } else {
            slot.setChanged();
        }

        if (
                stack.getCount()
                        == originalStack.getCount()
        ) {
            return ItemStack.EMPTY;
        }

        slot.onTake(
                player,
                stack
        );

        return originalStack;
    }

    @Override
    public void removed(
            Player player
    ) {
        super.removed(player);
        container.stopOpen(player);
    }

    private void addPlayerInventory(
            Inventory playerInventory
    ) {
        for (int row = 0; row < 3; row++) {
            for (
                    int column = 0;
                    column < 9;
                    column++
            ) {
                this.addSlot(
                        new Slot(
                                playerInventory,
                                column
                                        + row * 9
                                        + 9,
                                8
                                        + column * 18,
                                84
                                        + row * 18
                        )
                );
            }
        }
    }

    private void addPlayerHotbar(
            Inventory playerInventory
    ) {
        for (
                int column = 0;
                column < 9;
                column++
        ) {
            this.addSlot(
                    new Slot(
                            playerInventory,
                            column,
                            8
                                    + column * 18,
                            142
                    )
            );
        }
    }
}
