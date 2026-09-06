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

/**
 * RenderType for the wavy contact correction between two molten metals.
 *
 * Important differences from the first shader experiment:
 *
 * - uses DefaultVertexFormat.NEW_ENTITY, matching normal entity-translucent
 *   molten rendering;
 * - enables the Minecraft lightmap;
 * - keeps side normals so vanilla directional entity lighting is reproduced;
 * - the custom pass REPLACES the normal wavy correction instead of being drawn
 *   as a second coplanar band.
 *
 * Sampler0 = lower molten texture
 * Sampler1 = upper molten texture
 * Sampler2 = Minecraft lightmap
 */
public final class FoundryMetalBlendRenderType extends RenderType {

    private static ShaderInstance shader;

    private static final Map<BlendKey, RenderType> CACHE =
            new HashMap<>();

    private static final ShaderStateShard SHADER_STATE =
            new ShaderStateShard(
                    () -> shader
            );

    private FoundryMetalBlendRenderType(
            String name,
            VertexFormat format,
            VertexFormat.Mode mode,
            int bufferSize,
            boolean affectsCrumbling,
            boolean sortOnUpload,
            Runnable setupState,
            Runnable clearState
    ) {
        super(
                name,
                format,
                mode,
                bufferSize,
                affectsCrumbling,
                sortOnUpload,
                setupState,
                clearState
        );

        throw new IllegalStateException(
                "FoundryMetalBlendRenderType is a static RenderType holder."
        );
    }

    public static void registerShader(
            RegisterShadersEvent event
    ) throws IOException {
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(
                                KatzencraftMetalsMod.MODID,
                                "foundry_metal_blend"
                        ),
                        DefaultVertexFormat.NEW_ENTITY
                ),
                loadedShader -> {
                    shader = loadedShader;
                    CACHE.clear();
                }
        );
    }

    /**
     * Returns null until shader registration has completed. The caller then
     * falls back to the proven clean non-shader wavy correction.
     */
    static boolean isAvailable() {
        return shader != null;
    }

    static RenderType get(
            ResourceLocation lowerTexture,
            ResourceLocation upperTexture
    ) {
        if (shader == null) {
            return null;
        }

        return CACHE.computeIfAbsent(
                new BlendKey(
                        lowerTexture,
                        upperTexture
                ),
                FoundryMetalBlendRenderType::createBlendType
        );
    }

    private static RenderType createBlendType(
            BlendKey key
    ) {
        /*
         * Units 0 and 1 are deliberately our two molten textures.
         * The LIGHTMAP state owns unit 2.
         *
         * Overlay is disabled because NEW_ENTITY's UV1 attribute is repurposed
         * as the blend coordinate for this one optional pass.
         */
        MultiTextureStateShard textures =
                MultiTextureStateShard.builder()
                        .add(
                                key.lowerTexture(),
                                false,
                                false
                        )
                        .add(
                                key.upperTexture(),
                                false,
                                false
                        )
                        .build();

        CompositeState state =
                CompositeState.builder()
                        .setShaderState(SHADER_STATE)
                        .setTextureState(textures)
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setCullState(CULL)
                        .setLightmapState(LIGHTMAP)
                        .setOverlayState(NO_OVERLAY)
                        .setWriteMaskState(COLOR_DEPTH_WRITE)
                        .setOutputState(MAIN_TARGET)
                        .createCompositeState(false);

        return create(
                "katzencraftmetals_foundry_metal_blend",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                256,
                false,
                true,
                state
        );
    }

    private record BlendKey(
            ResourceLocation lowerTexture,
            ResourceLocation upperTexture
    ) {
    }
}
