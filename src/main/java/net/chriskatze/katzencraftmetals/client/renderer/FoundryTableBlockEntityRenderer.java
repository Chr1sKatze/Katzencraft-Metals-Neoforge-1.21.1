package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.chriskatze.katzencraftmetals.block.custom.FoundryTableBlock;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTableBlockEntity;
import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import static net.chriskatze.katzencraftmetals.block.entity.FoundryTableBlockEntity.*;

public class FoundryTableBlockEntityRenderer implements BlockEntityRenderer<FoundryTableBlockEntity> {
    // One hundredth of a model pixel, horizontal only; all vertical dimensions stay exact.
    static final float WALL_CLEARANCE = 0.01f / 16.0f;
    public FoundryTableBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public boolean shouldRender(FoundryTableBlockEntity table, Vec3 camera) { return true; }

    @Override public void render(FoundryTableBlockEntity table, float partialTick, PoseStack poses,
                                 MultiBufferSource buffers, int light, int overlay) {
        if (table.isEmpty() || table.getLevel() == null) return;
        var metal = ModMoltenMetals.get(table.getStoredMetal()).orElse(null);
        if (metal == null) return;
        float fill = table.isCooled() ? 1 : Mth.clamp(FoundryBasinFillSmoother.getDisplayedMoltenAmount(table, partialTick)
                / table.getRequiredMoltenAmount(), 0, 1);
        if (fill <= 0) return;
        float y = Mth.lerp(fill, MOLD_BOTTOM, MOLD_TOP);
        float cooling = table.isCooled() ? 1 : table.isFull()
                ? Mth.clamp((table.getCoolingProgress() + partialTick) / table.getCoolingDuration(), 0, 1) : 0;
        var frame = MoltenIronAnimation.getFrame(table.getLevel().getGameTime());
        RenderType material = FoundryCastingRenderType.get(metal.animatedTexture(), FoundryIngotTextures.get(metal.id()));
        if (material == null) return; // Shader registration completes before normal world rendering.
        VertexConsumer out = buffers.getBuffer(material);
        poses.pushPose();
        poses.translate(.5,0,.5);
        poses.mulPose(Axis.YP.rotationDegrees(-FoundryTableBlock.modelRotation(table.getBlockState().getValue(FoundryTableBlock.FACING))));
        poses.translate(-.5,0,-.5);
        emitCasting(out, poses.last(), y, cooling, frame.minV(), frame.maxV(), light);
        poses.popPose();
    }

    static void emitCasting(VertexConsumer out, PoseStack.Pose pose, float top, float cooling,
                            float minV, float maxV, int light) {
        float x1=MOLD_MIN_X+WALL_CLEARANCE, x2=MOLD_MAX_X-WALL_CLEARANCE;
        float z1=MOLD_MIN_Z+WALL_CLEARANCE, z2=MOLD_MAX_Z-WALL_CLEARANCE;
        float b=MOLD_BOTTOM;
        // Five outward faces. The bottom is permanently buried against the model's mold floor.
        quad(out,pose,new float[]{x1,top,z1, x1,top,z2, x2,top,z2, x2,top,z1},0,1,0,cooling,minV,maxV,light);
        quad(out,pose,new float[]{x1,top,z1, x2,top,z1, x2,b,z1, x1,b,z1},0,0,-1,cooling,minV,maxV,light);
        quad(out,pose,new float[]{x2,top,z2, x1,top,z2, x1,b,z2, x2,b,z2},0,0,1,cooling,minV,maxV,light);
        quad(out,pose,new float[]{x1,top,z2, x1,top,z1, x1,b,z1, x1,b,z2},-1,0,0,cooling,minV,maxV,light);
        quad(out,pose,new float[]{x2,top,z1, x2,top,z2, x2,b,z2, x2,b,z1},1,0,0,cooling,minV,maxV,light);
    }
    private static void quad(VertexConsumer out, PoseStack.Pose pose, float[] xyz,
                             float nx,float ny,float nz,float cooling,float minV,float maxV,int light) {
        for(int i=0;i<4;i++) {
            float u=i<2?0:1, v=(i==0||i==3)?0:1;
            // Painted PNG bounds: X [0,16), Y [4,12). Its long axis follows local mold Z.
            float solidU=ny>0.5f?v:u, solidV=ny>0.5f?0.25f+0.5f*u:v;
            // Native block-local scale for molten textures; cold UVs remain independent.
            float moltenU=nx!=0?xyz[i*3+2]:xyz[i*3];
            float moltenV=ny>0.5f?xyz[i*3+2]:1.0f-xyz[i*3+1];
            out.addVertex(pose.pose(),xyz[i*3],xyz[i*3+1],xyz[i*3+2])
                    .setColor(255,255,255,Math.round(cooling*255)).setUv(moltenU,Mth.lerp(moltenV,minV,maxV))
                    .setUv1(Math.round(solidU*32767),Math.round(solidV*32767)).setLight(light).setNormal(pose,nx,ny,nz);
        }
    }
}
