package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.chriskatze.katzencraftmetals.block.entity.FoundryControllerBlockEntity;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTankNetwork;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Set;

/**
 * Single renderer for dynamic Foundry visuals.
 *
 * Tank casing and diagonal/concave frame corrections are normal chunk
 * rendering through FoundryTankConnectedFrameModel. This Controller BER owns
 * dynamic liquid only. Liquid is additionally clipped against the client's real
 * physical Tank blocks, so a stale Controller snapshot can never draw liquid in
 * a Tank position that the client has already received as air after a break.
 */
public final class FoundryControllerBlockEntityRenderer
        implements BlockEntityRenderer<FoundryControllerBlockEntity> {

    private static final int VIEW_DISTANCE = 20 * 16;

    private final FoundryControllerTankLiquidRenderer liquidRenderer =
            new FoundryControllerTankLiquidRenderer();

    public FoundryControllerBlockEntityRenderer(
            BlockEntityRendererProvider.Context context
    ) {
    }

    @Override
    public void render(
            FoundryControllerBlockEntity controller,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        FoundryTankNetwork network = controller.getOwnedTankNetwork();

        if (network == null || network.getTankPositions().isEmpty()) {
            return;
        }

        Set<BlockPos> physicalStructure =
                getPhysicalClientStructure(
                        controller,
                        network
                );

        if (physicalStructure.isEmpty()) {
            return;
        }

        liquidRenderer.render(
                controller,
                network,
                physicalStructure,
                partialTick,
                poseStack,
                bufferSource,
                packedOverlay
        );
    }

    private static Set<BlockPos> getPhysicalClientStructure(
            FoundryControllerBlockEntity controller,
            FoundryTankNetwork network
    ) {
        Level level = controller.getLevel();

        if (level == null) {
            return Set.of();
        }

        Set<BlockPos> physical = new HashSet<>();

        for (BlockPos tankPos : network.getTankPositions()) {
            if (
                    level.getBlockState(tankPos)
                            .is(ModBlocks.FOUNDRY_TANK.get())
            ) {
                physical.add(tankPos.immutable());
            }
        }

        return Set.copyOf(physical);
    }

    @Override
    public boolean shouldRenderOffScreen(
            FoundryControllerBlockEntity controller
    ) {
        return false;
    }

    @Override
    public int getViewDistance() {
        return VIEW_DISTANCE;
    }

    @Override
    public AABB getRenderBoundingBox(
            FoundryControllerBlockEntity controller
    ) {
        FoundryTankNetwork network = controller.getOwnedTankNetwork();

        if (network == null || network.getTankPositions().isEmpty()) {
            BlockPos pos = controller.getBlockPos();
            return new AABB(pos);
        }

        Set<BlockPos> structure =
                getPhysicalClientStructure(
                        controller,
                        network
                );

        if (structure.isEmpty()) {
            BlockPos pos = controller.getBlockPos();
            return new AABB(pos);
        }

        int minX = controller.getBlockPos().getX();
        int minY = controller.getBlockPos().getY();
        int minZ = controller.getBlockPos().getZ();
        int maxX = minX;
        int maxY = minY;
        int maxZ = minZ;

        for (BlockPos pos : structure) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        return new AABB(
                minX,
                minY,
                minZ,
                maxX + 1.0D,
                maxY + 1.0D,
                maxZ + 1.0D
        );
    }
}
