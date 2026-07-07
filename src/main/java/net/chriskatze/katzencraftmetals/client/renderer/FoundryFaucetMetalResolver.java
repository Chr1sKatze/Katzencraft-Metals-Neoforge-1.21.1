package net.chriskatze.katzencraftmetals.client.renderer;

import net.chriskatze.katzencraftmetals.block.custom.FoundryFaucetBlock;
import net.chriskatze.katzencraftmetals.block.entity.CastingCauldronBlockEntity;
import net.chriskatze.katzencraftmetals.block.entity.FoundryFaucetBlockEntity;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTankBlockEntity;
import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.chriskatze.katzencraftmetals.metal.MoltenMetalDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

final class FoundryFaucetMetalResolver {

    private FoundryFaucetMetalResolver() {
    }

    @Nullable
    static MoltenMetalDefinition resolve(
            FoundryFaucetBlockEntity faucet,
            CastingCauldronBlockEntity cauldron
    ) {
        ResourceLocation renderedMetalId =
                faucet.getPouringMetal();

        if (renderedMetalId == null) {
            renderedMetalId =
                    cauldron.getStoredMetal();
        }

        if (renderedMetalId == null) {
            renderedMetalId =
                    resolveSourceTankMetal(
                            faucet
                    );
        }

        return renderedMetalId != null
                ? ModMoltenMetals.get(
                        renderedMetalId
                ).orElse(null)
                : null;
    }

    @Nullable
    private static ResourceLocation resolveSourceTankMetal(
            FoundryFaucetBlockEntity faucet
    ) {
        Direction facing =
                faucet.getBlockState().getValue(
                        FoundryFaucetBlock.FACING
                );

        BlockPos sourceTankPos =
                faucet.getBlockPos().relative(
                        facing.getOpposite()
                );

        BlockEntity sourceBlockEntity =
                faucet.getLevel().getBlockEntity(
                        sourceTankPos
                );

        if (sourceBlockEntity instanceof FoundryTankBlockEntity sourceTank) {
            return sourceTank.getStoredMetal();
        }

        return null;
    }
}
