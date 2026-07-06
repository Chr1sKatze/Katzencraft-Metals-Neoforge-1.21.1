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
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public class FoundryFaucetOutputMenu extends AbstractContainerMenu {

    public static final int CLEAR_SELECTION_BUTTON = 0;
    public static final int SELECT_METAL_BUTTON_BASE = 1;

    private final FoundryFaucetBlockEntity faucet;
    private final ContainerLevelAccess access;

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
                )
        );
    }

    public FoundryFaucetOutputMenu(
            int containerId,
            Inventory playerInventory,
            FoundryFaucetBlockEntity faucet
    ) {
        super(
                ModMenuTypes.FOUNDRY_FAUCET_OUTPUT_MENU.get(),
                containerId
        );

        this.faucet = faucet;

        this.access =
                ContainerLevelAccess.create(
                        faucet.getLevel(),
                        faucet.getBlockPos()
                );
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

    public List<MoltenMetalDefinition> getMetals() {
        return ModMoltenMetals.heaviestFirst();
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
