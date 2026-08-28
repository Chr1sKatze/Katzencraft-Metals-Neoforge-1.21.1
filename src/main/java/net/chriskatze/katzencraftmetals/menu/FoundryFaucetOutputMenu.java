package net.chriskatze.katzencraftmetals.menu;

import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.chriskatze.katzencraftmetals.block.entity.FoundryFaucetBlockEntity;
import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.chriskatze.katzencraftmetals.metal.MoltenMetalDefinition;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FoundryFaucetOutputMenu extends AbstractContainerMenu {

    public static final int CLEAR_SELECTION_BUTTON = 0;
    public static final int SELECT_METAL_BUTTON_BASE = 1;

    private static final int DATA_COUNT =
            ModMoltenMetals.values().size();

    private final FoundryFaucetBlockEntity faucet;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    public FoundryFaucetOutputMenu(
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
                new SimpleContainerData(DATA_COUNT)
        );
    }

    public FoundryFaucetOutputMenu(
            int containerId,
            Inventory playerInventory,
            FoundryFaucetBlockEntity faucet
    ) {
        this(
                containerId,
                playerInventory,
                faucet,
                createData(faucet)
        );
    }

    private FoundryFaucetOutputMenu(
            int containerId,
            Inventory playerInventory,
            FoundryFaucetBlockEntity faucet,
            ContainerData data
    ) {
        super(
                ModMenuTypes.FOUNDRY_FAUCET_OUTPUT_MENU.get(),
                containerId
        );

        this.faucet = faucet;
        this.data = data;

        this.access =
                ContainerLevelAccess.create(
                        faucet.getLevel(),
                        faucet.getBlockPos()
                );

        checkContainerDataCount(
                data,
                DATA_COUNT
        );

        addDataSlots(data);
    }

    private static FoundryFaucetBlockEntity getBlockEntity(
            Inventory playerInventory,
            FriendlyByteBuf extraData
    ) {
        var pos =
                extraData.readBlockPos();

        var blockEntity =
                playerInventory.player
                        .level()
                        .getBlockEntity(pos);

        if (blockEntity instanceof FoundryFaucetBlockEntity faucet) {
            return faucet;
        }

        throw new IllegalStateException(
                "FoundryFaucetBlockEntity missing at " + pos
        );
    }

    private static ContainerData createData(
            FoundryFaucetBlockEntity faucet
    ) {
        return new ContainerData() {
            @Override
            public int get(
                    int index
            ) {
                return ModMoltenMetals.bySyncId(index)
                        .map(
                                definition ->
                                        faucet.hasDiscoveredLockableMetal(
                                                definition.id()
                                        )
                                                ? 1
                                                : 0
                        )
                        .orElse(0);
            }

            @Override
            public void set(
                    int index,
                    int value
            ) {
                /* Server-owned data. */
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    public List<MoltenMetalDefinition> getMetals() {
        List<MoltenMetalDefinition> metals =
                new ArrayList<>();

        for (MoltenMetalDefinition definition : ModMoltenMetals.heaviestFirst()) {
            int syncId =
                    ModMoltenMetals.getSyncId(
                            definition.id()
                    );

            if (
                    syncId >= 0
                            && data.get(syncId) != 0
            ) {
                metals.add(definition);
            }
        }

        return metals;
    }

    public Optional<ResourceLocation> getSelectedMetalId() {
        return faucet.getSelectedOutputMetalId();
    }

    public boolean isAutomaticSelected() {
        return getSelectedMetalId().isEmpty();
    }

    public boolean isSelected(
            MoltenMetalDefinition definition
    ) {
        return getSelectedMetalId()
                .map(id -> id.equals(definition.id()))
                .orElse(false);
    }

    public int getButtonForMetal(
            MoltenMetalDefinition definition
    ) {
        int index =
                getMetals().indexOf(definition);

        return index < 0
                ? -1
                : SELECT_METAL_BUTTON_BASE + index;
    }

    @Override
    public boolean clickMenuButton(
            Player player,
            int buttonId
    ) {
        if (buttonId == CLEAR_SELECTION_BUTTON) {
            faucet.clearSelectedOutputMetal();
            return true;
        }

        int index =
                buttonId - SELECT_METAL_BUTTON_BASE;

        List<MoltenMetalDefinition> metals =
                getMetals();

        if (
                index < 0
                        || index >= metals.size()
        ) {
            return false;
        }

        faucet.setSelectedOutputMetal(
                metals.get(index).id()
        );

        return true;
    }

    @Override
    public boolean stillValid(
            Player player
    ) {
        return stillValid(
                access,
                player,
                ModBlocks.FOUNDRY_FAUCET.get()
        );
    }

    @Override
    public ItemStack quickMoveStack(
            Player player,
            int index
    ) {
        return ItemStack.EMPTY;
    }
}
