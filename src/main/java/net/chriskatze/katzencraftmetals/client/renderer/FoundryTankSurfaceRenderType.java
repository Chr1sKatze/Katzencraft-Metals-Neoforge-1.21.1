package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** One depth-writing liquid top, with its contact rim composited in the fragment shader. */
public final class FoundryTankSurfaceRenderType extends RenderType {
    private static ShaderInstance shader;
    private static final Map<ResourceLocation, RenderType> CACHE = new HashMap<>();
    private static final ShaderStateShard SHADER = new ShaderStateShard(() -> shader);

    private FoundryTankSurfaceRenderType() {
        super("unused", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
                1536, false, false, () -> {}, () -> {});
        throw new IllegalStateException("Static RenderType holder");
    }

    public static void registerShader(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath(KatzencraftMetalsMod.MODID, "foundry_tank_surface"),
                DefaultVertexFormat.NEW_ENTITY), loaded -> {
                    shader = loaded;
                    loaded.safeGetUniform("RimWidths").set(FoundryTankRenderConstants.LIQUID_SIDE_INSET,
                            FoundryTankRenderConstants.CONTACT_RIM_WIDTH_SMALL,
                            FoundryTankRenderConstants.CONTACT_RIM_WIDTH_MEDIUM,
                            FoundryTankRenderConstants.CONTACT_RIM_WIDTH_LARGE);
                    loaded.safeGetUniform("RimCounts").set((float) FoundryTankRenderConstants.CONTACT_RIM_MEDIUM_SURFACE_CELLS,
                            (float) FoundryTankRenderConstants.CONTACT_RIM_LARGE_SURFACE_CELLS);
                    CACHE.clear();
                });
    }

    static RenderType get(ResourceLocation texture) {
        if (shader == null) return null;
        return CACHE.computeIfAbsent(texture, key -> FoundryTankLiquidDepthRenderType.wrap(create(
                "katzencraftmetals_tank_surface", DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS, 1536, true, true,
                CompositeState.builder().setShaderState(SHADER)
                        .setTextureState(new TextureStateShard(key, false, false))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_DEPTH_WRITE)
                        .setCullState(CULL).setLightmapState(LIGHTMAP).setOverlayState(NO_OVERLAY)
                        .createCompositeState(false))));
    }
}
