package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;

/**
 * Replaces Minecraft's normal full-cube selection outline for Foundry Tanks
 * with a face-only outline.
 *
 * The normal 12-edge block outline looks "x-rayed" through the Tank's tinted
 * glass because rear/internal edges remain visible through the translucent
 * block. A four-edge outline on only the face the player actually hit keeps
 * precise targeting feedback without exposing the back of the selected Tank.
 */
@EventBusSubscriber(
        modid = KatzencraftMetalsMod.MODID,
        value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.GAME
)
public final class FoundryTankHighlightRenderer {

    /*
     * Move the highlight a tiny distance outside the hit face so it does not
     * z-fight with the Tank glass/frame itself.
     */
    private static final float FACE_OFFSET =
            0.0025F;

    /*
     * Dark, slightly translucent line. This keeps the selection readable
     * without the bright/inverted-looking effect of the vanilla outline
     * through tinted glass.
     */
    private static final int LINE_RED = 28;
    private static final int LINE_GREEN = 28;
    private static final int LINE_BLUE = 28;
    private static final int LINE_ALPHA = 190;

    private FoundryTankHighlightRenderer() {
    }

    @SubscribeEvent
    public static void renderTankHighlight(
            RenderHighlightEvent.Block event
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        BlockHitResult hit =
                event.getTarget();

        BlockPos pos =
                hit.getBlockPos();

        BlockState state =
                minecraft.level.getBlockState(
                        pos
                );

        if (!state.is(ModBlocks.FOUNDRY_TANK.get())) {
            return;
        }

        /*
         * Stop vanilla from drawing the complete 12-edge block outline.
         */
        event.setCanceled(true);

        PoseStack poseStack =
                event.getPoseStack();

        Vec3 cameraPos =
                event.getCamera()
                        .getPosition();

        poseStack.pushPose();

        poseStack.translate(
                pos.getX() - cameraPos.x,
                pos.getY() - cameraPos.y,
                pos.getZ() - cameraPos.z
        );

        VertexConsumer lines =
                event.getMultiBufferSource()
                        .getBuffer(
                                RenderType.lines()
                        );

        renderFaceOutline(
                poseStack,
                lines,
                hit.getDirection()
        );

        poseStack.popPose();
    }

    private static void renderFaceOutline(
            PoseStack poseStack,
            VertexConsumer lines,
            Direction face
    ) {
        float min = 0.0F;
        float max = 1.0F;

        switch (face) {
            case NORTH -> {
                float z = -FACE_OFFSET;

                addLine(
                        poseStack,
                        lines,
                        min, min, z,
                        max, min, z
                );

                addLine(
                        poseStack,
                        lines,
                        max, min, z,
                        max, max, z
                );

                addLine(
                        poseStack,
                        lines,
                        max, max, z,
                        min, max, z
                );

                addLine(
                        poseStack,
                        lines,
                        min, max, z,
                        min, min, z
                );
            }

            case SOUTH -> {
                float z = max + FACE_OFFSET;

                addLine(
                        poseStack,
                        lines,
                        max, min, z,
                        min, min, z
                );

                addLine(
                        poseStack,
                        lines,
                        min, min, z,
                        min, max, z
                );

                addLine(
                        poseStack,
                        lines,
                        min, max, z,
                        max, max, z
                );

                addLine(
                        poseStack,
                        lines,
                        max, max, z,
                        max, min, z
                );
            }

            case WEST -> {
                float x = -FACE_OFFSET;

                addLine(
                        poseStack,
                        lines,
                        x, min, max,
                        x, min, min
                );

                addLine(
                        poseStack,
                        lines,
                        x, min, min,
                        x, max, min
                );

                addLine(
                        poseStack,
                        lines,
                        x, max, min,
                        x, max, max
                );

                addLine(
                        poseStack,
                        lines,
                        x, max, max,
                        x, min, max
                );
            }

            case EAST -> {
                float x = max + FACE_OFFSET;

                addLine(
                        poseStack,
                        lines,
                        x, min, min,
                        x, min, max
                );

                addLine(
                        poseStack,
                        lines,
                        x, min, max,
                        x, max, max
                );

                addLine(
                        poseStack,
                        lines,
                        x, max, max,
                        x, max, min
                );

                addLine(
                        poseStack,
                        lines,
                        x, max, min,
                        x, min, min
                );
            }

            case DOWN -> {
                float y = -FACE_OFFSET;

                addLine(
                        poseStack,
                        lines,
                        min, y, max,
                        max, y, max
                );

                addLine(
                        poseStack,
                        lines,
                        max, y, max,
                        max, y, min
                );

                addLine(
                        poseStack,
                        lines,
                        max, y, min,
                        min, y, min
                );

                addLine(
                        poseStack,
                        lines,
                        min, y, min,
                        min, y, max
                );
            }

            case UP -> {
                float y = max + FACE_OFFSET;

                addLine(
                        poseStack,
                        lines,
                        min, y, min,
                        max, y, min
                );

                addLine(
                        poseStack,
                        lines,
                        max, y, min,
                        max, y, max
                );

                addLine(
                        poseStack,
                        lines,
                        max, y, max,
                        min, y, max
                );

                addLine(
                        poseStack,
                        lines,
                        min, y, max,
                        min, y, min
                );
            }
        }
    }

    private static void addLine(
            PoseStack poseStack,
            VertexConsumer lines,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2
    ) {
        float deltaX =
                x2 - x1;

        float deltaY =
                y2 - y1;

        float deltaZ =
                z2 - z1;

        float length =
                (float) Math.sqrt(
                        deltaX * deltaX
                                + deltaY * deltaY
                                + deltaZ * deltaZ
                );

        if (length <= 0.0F) {
            return;
        }

        float normalX =
                deltaX / length;

        float normalY =
                deltaY / length;

        float normalZ =
                deltaZ / length;

        PoseStack.Pose pose =
                poseStack.last();

        lines.addVertex(
                        pose,
                        x1,
                        y1,
                        z1
                )
                .setColor(
                        LINE_RED,
                        LINE_GREEN,
                        LINE_BLUE,
                        LINE_ALPHA
                )
                .setNormal(
                        pose,
                        normalX,
                        normalY,
                        normalZ
                );

        lines.addVertex(
                        pose,
                        x2,
                        y2,
                        z2
                )
                .setColor(
                        LINE_RED,
                        LINE_GREEN,
                        LINE_BLUE,
                        LINE_ALPHA
                )
                .setNormal(
                        pose,
                        normalX,
                        normalY,
                        normalZ
                );
    }
}
