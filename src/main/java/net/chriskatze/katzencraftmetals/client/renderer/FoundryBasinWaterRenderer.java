package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.chriskatze.katzencraftmetals.block.custom.FoundryBasinBlock;
import net.chriskatze.katzencraftmetals.block.entity.FoundryBasinBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

/** Only the exposed contained-fluid surface and four recessed window faces are visible inside the authored Basin. */
public final class FoundryBasinWaterRenderer {
    private FoundryBasinWaterRenderer() {}

    // Model pixels. Glass occupies 0..1 / 15..16, slit 7..9 and Y=4..13.
    // Keep 0.05 pixel clearance from each slit edge and from the glass's inner plane.
    static final float[][] FACES = {
            {2.05f,15,2.05f, 2.05f,15,13.95f, 13.95f,15,13.95f, 13.95f,15,2.05f, 0,1,0},
            {7.05f,4.05f,1.05f, 7.05f,12.95f,1.05f, 8.95f,12.95f,1.05f, 8.95f,4.05f,1.05f, 0,0,-1},
            {8.95f,4.05f,14.95f, 8.95f,12.95f,14.95f, 7.05f,12.95f,14.95f, 7.05f,4.05f,14.95f, 0,0,1},
            {1.05f,4.05f,8.95f, 1.05f,12.95f,8.95f, 1.05f,12.95f,7.05f, 1.05f,4.05f,7.05f, -1,0,0},
            {14.95f,4.05f,7.05f, 14.95f,12.95f,7.05f, 14.95f,12.95f,8.95f, 14.95f,4.05f,8.95f, 1,0,0}
    };

    // A partial water surface also crosses each recessed slit. Full fluid sits above the slits.
    static final float[][] WINDOW_TOPS = {
            {7.05f,0,1.05f, 7.05f,0,2.05f, 8.95f,0,2.05f, 8.95f,0,1.05f, 0,1,0},
            {7.05f,0,13.95f, 7.05f,0,14.95f, 8.95f,0,14.95f, 8.95f,0,13.95f, 0,1,0},
            {1.05f,0,7.05f, 1.05f,0,8.95f, 2.05f,0,8.95f, 2.05f,0,7.05f, 0,1,0},
            {13.95f,0,7.05f, 13.95f,0,8.95f, 14.95f,0,8.95f, 14.95f,0,7.05f, 0,1,0}
    };

    static float surfacePixels(net.minecraft.world.level.block.state.BlockState state) {
        return 3 + 4 * state.getValue(FoundryBasinBlock.FLUID_LEVEL);
    }

    static float vertexYPixels(float[] face, int vertex, float surface) {
        return face[13] != 0 ? surface : Math.min(face[vertex*3+1], surface);
    }

    static boolean render(FoundryBasinBlockEntity basin, PoseStack poses, MultiBufferSource buffers, int light, int overlay) {
        var type = FoundryBasinBlock.containedFluid(basin.getBlockState());
        if (type == Fluids.EMPTY) return false;
        var fluid = type.defaultFluidState();
        float surface = surfacePixels(basin.getBlockState());
        if (type == Fluids.LAVA) light = net.minecraft.client.renderer.LightTexture.FULL_BRIGHT;
        var appearance = IClientFluidTypeExtensions.of(fluid);
        var sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(appearance.getStillTexture(fluid, basin.getLevel(), basin.getBlockPos()));
        int tint = appearance.getTintColor(fluid, basin.getLevel(), basin.getBlockPos());
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
        var pose = poses.last();
        draw(FACES,consumer,pose,sprite,tint,light,overlay,surface);
        if (surface > 4.05f && surface < 12.95f) {
            draw(WINDOW_TOPS,consumer,pose,sprite,tint,light,overlay,surface);
        }
        return true;
    }

    private static void draw(float[][] faces, VertexConsumer consumer, PoseStack.Pose pose,
                             net.minecraft.client.renderer.texture.TextureAtlasSprite sprite,
                             int tint, int light, int overlay, float surface) {
        for (float[] face : faces) {
            if (face[13] == 0 && surface <= face[1]) continue;
            for (int i=0; i<4; i++) {
                float x=face[i*3]/16f, y=vertexYPixels(face, i, surface)/16f, z=face[i*3+2]/16f;
                float u=face[12]!=0 ? z : x;
                float v=face[13]!=0 ? z : 1-y;
                consumer.addVertex(pose.pose(),x,y,z).setColor(tint)
                        .setUv(sprite.getU(u),sprite.getV(v)).setOverlay(overlay).setLight(light)
                        .setNormal(pose,face[12],face[13],face[14]);
            }
        }
    }
}
