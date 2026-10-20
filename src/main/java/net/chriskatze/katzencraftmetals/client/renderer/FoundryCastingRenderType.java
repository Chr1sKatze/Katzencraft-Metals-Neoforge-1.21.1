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

/** Two materials on one casting mesh; no overlapping cooling overlay or polygon offsets. */
public final class FoundryCastingRenderType extends RenderType {
    private static ShaderInstance shader;
    private static final Map<Materials, RenderType> CACHE = new HashMap<>();
    private static final ShaderStateShard SHADER = new ShaderStateShard(() -> shader);
    private FoundryCastingRenderType() {
        super("unused", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, false, () -> {}, () -> {});
        throw new IllegalStateException("Static holder");
    }
    public static void registerShader(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath(KatzencraftMetalsMod.MODID,"foundry_casting"),
                DefaultVertexFormat.NEW_ENTITY), loaded -> { shader = loaded; CACHE.clear(); });
    }
    private record Materials(ResourceLocation molten, ResourceLocation solid) {}
    static RenderType get(ResourceLocation molten, ResourceLocation solid) {
        if (shader == null) return null;
        return CACHE.computeIfAbsent(new Materials(molten, solid), key -> create("katzencraftmetals_casting",
                DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, true,
                CompositeState.builder().setShaderState(SHADER)
                        .setTextureState(MultiTextureStateShard.builder().add(key.molten(), false, false)
                                .add(key.solid(), false, false).build())
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(COLOR_DEPTH_WRITE).setCullState(CULL)
                        .setLightmapState(LIGHTMAP).setOverlayState(NO_OVERLAY).createCompositeState(false)));
    }
}
