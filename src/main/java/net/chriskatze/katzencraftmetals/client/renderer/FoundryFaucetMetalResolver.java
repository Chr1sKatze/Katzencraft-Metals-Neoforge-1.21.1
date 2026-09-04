package net.chriskatze.katzencraftmetals.client.renderer;

import net.chriskatze.katzencraftmetals.block.custom.FoundryFaucetBlock;
import net.chriskatze.katzencraftmetals.block.entity.CastingCauldronBlockEntity;
import net.chriskatze.katzencraftmetals.block.entity.FoundryFaucetBlockEntity;
import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.chriskatze.katzencraftmetals.metal.MoltenMetalDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
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
        if (faucet.getLevel() == null) {
            return null;
        }

        Direction facing =
                faucet.getBlockState().getValue(
                        FoundryFaucetBlock.FACING
                );

        BlockPos sourceTankPos =
                faucet.getBlockPos().relative(
                        facing.getOpposite()
                );

        /*
         * Tanks are ordinary blocks now. Resolve the source through the
         * Faucet's Controller-owned network lookup instead of looking for a
         * Tank BlockEntity.
         *
         * resolveOutputMetal() uses the Faucet's cached Controller lookup and
         * respects the Faucet/controller output selection, while remaining
         * entirely position-based.
         */
        return faucet.resolveOutputMetal(
                sourceTankPos
        ).orElse(null);
    }
}
