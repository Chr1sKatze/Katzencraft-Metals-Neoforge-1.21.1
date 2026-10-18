package net.chriskatze.katzencraftmetals.client.renderer;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

import java.util.ArrayList;
import java.util.List;

/** Corrects inset-wall light sampling without changing the basin's full collision box. */
public final class FoundryBasinLightingModel extends BakedModelWrapper<BakedModel> {
    private static final ModelProperty<Integer> OPENING_LIGHT = new ModelProperty<>();

    private FoundryBasinLightingModel(BakedModel original) {
        super(original);
    }

    public static void modifyBakingResult(ModelEvent.ModifyBakingResult event) {
        event.getModels().replaceAll((id, model) ->
                id.id().getNamespace().equals(KatzencraftMetalsMod.MODID)
                        && id.id().getPath().equals("foundry_basin")
                        && !id.variant().equals("inventory")
                        ? new FoundryBasinLightingModel(model) : model);
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos,
                                  BlockState state, ModelData data) {
        return originalModel.getModelData(level, pos, state, data).derive()
                .with(OPENING_LIGHT, LevelRenderer.getLightColor(level, pos.above()))
                .build();
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random,
                                    ModelData data, RenderType renderType) {
        List<BakedQuad> quads = originalModel.getQuads(state, side, random, data, renderType);
        Integer light = data.get(OPENING_LIGHT);
        if (light == null || side != null) {
            return quads;
        }
        List<BakedQuad> lit = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            if (!isInnerWall(quad.getDirection(), quad.getVertices())) {
                lit.add(quad);
                continue;
            }
            int[] vertices = quad.getVertices().clone();
            int stride = vertices.length / 4;
            for (int vertex = 0; vertex < 4; vertex++) {
                // UV2 is the baked lightmap. The renderer merges it with sampled
                // world light, preserving real day/night and block-light levels.
                vertices[vertex * stride + 6] = light;
            }
            lit.add(new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(),
                    quad.getSprite(), quad.isShade(), quad.hasAmbientOcclusion()));
        }
        return lit;
    }

    static boolean isInnerWall(Direction face, int[] vertices) {
        int axis;
        float plane;
        switch (face) {
            case EAST -> { axis = 0; plane = 2.0f / 16.0f; }
            case WEST -> { axis = 0; plane = 14.0f / 16.0f; }
            case SOUTH -> { axis = 2; plane = 2.0f / 16.0f; }
            case NORTH -> { axis = 2; plane = 14.0f / 16.0f; }
            default -> { return false; }
        }
        int stride = vertices.length / 4;
        for (int vertex = 0; vertex < 4; vertex++) {
            if (Math.abs(Float.intBitsToFloat(vertices[vertex * stride + axis]) - plane) > 0.00001f) {
                return false;
            }
        }
        return true;
    }
}
