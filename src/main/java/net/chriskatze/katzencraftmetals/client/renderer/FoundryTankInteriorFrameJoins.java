package net.chriskatze.katzencraftmetals.client.renderer;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Closes the recessed backs of concave frame edges, at the existing rail thickness. */
final class FoundryTankInteriorFrameJoins {
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final float[] GRID = {0, 0.1f, 15.9f, 16};

    private FoundryTankInteriorFrameJoins() {}

    static int mask(Predicate<BlockPos> tank, Predicate<Direction> connected, BlockPos pos) {
        int mask = 0, bit = 0;
        for (int i = 0; i < DIRECTIONS.length; i++) {
            for (int j = i + 1; j < DIRECTIONS.length; j++) {
                Direction first = DIRECTIONS[i], second = DIRECTIONS[j];
                if (first.getAxis() == second.getAxis()) continue;
                if (connected.test(first) && connected.test(second)
                        && tank.test(pos.relative(first)) && tank.test(pos.relative(second))
                        && !tank.test(pos.relative(first).relative(second))) mask |= 1 << bit;
                bit++;
            }
        }
        return mask;
    }

    // A union of thin edge volumes avoids duplicate faces at three-way joins.
    static List<Face> faces(int mask) {
        boolean[][][] solid = new boolean[3][3][3];
        int bit = 0;
        for (int i = 0; i < DIRECTIONS.length; i++) {
            for (int j = i + 1; j < DIRECTIONS.length; j++) {
                Direction first = DIRECTIONS[i], second = DIRECTIONS[j];
                if (first.getAxis() == second.getAxis()) continue;
                if ((mask & (1 << bit)) != 0) {
                    for (int x = 0; x < 3; x++) for (int y = 0; y < 3; y++) for (int z = 0; z < 3; z++) {
                        int[] cell = {x, y, z};
                        if (cell[first.getAxis().ordinal()] == edge(first)
                                && cell[second.getAxis().ordinal()] == edge(second)) solid[x][y][z] = true;
                    }
                }
                bit++;
            }
        }
        List<Face> result = new ArrayList<>();
        for (int axis = 0; axis < 3; axis++) for (int plane = 1; plane <= 2; plane++) {
            int a = (axis + 1) % 3, b = (axis + 2) % 3;
            int[][] signs = new int[3][3];
            for (int u = 0; u < 3; u++) for (int v = 0; v < 3; v++) {
                int[] cell = new int[3]; cell[a] = u; cell[b] = v; cell[axis] = plane - 1;
                boolean low = solid[cell[0]][cell[1]][cell[2]];
                cell[axis] = plane;
                boolean high = solid[cell[0]][cell[1]][cell[2]];
                signs[u][v] = low == high ? 0 : low ? 1 : -1;
            }
            // Merge coplanar tiles; no unnecessary subdivisions along a straight edge.
            for (int u = 0; u < 3; u++) for (int v = 0; v < 3; v++) {
                int sign = signs[u][v]; if (sign == 0) continue;
                int endU = u + 1;
                while (endU < 3 && signs[endU][v] == sign) endU++;
                int endV = v + 1;
                outer: while (endV < 3) {
                    for (int k = u; k < endU; k++) if (signs[k][endV] != sign) break outer;
                    endV++;
                }
                for (int k = u; k < endU; k++) for (int l = v; l < endV; l++) signs[k][l] = 0;
                Direction normal = Direction.fromAxisAndDirection(Direction.Axis.values()[axis],
                        sign > 0 ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
                result.add(new Face(normal, GRID[plane], GRID[u], GRID[endU], GRID[v], GRID[endV]));
            }
        }
        // No faces at 0 or 16: neighboring frame planes already own those surfaces.
        return List.copyOf(result);
    }

    private static int edge(Direction direction) {
        return direction.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? 0 : 2;
    }

    record Face(Direction normal, float plane, float minA, float maxA, float minB, float maxB) {
        float[][] vertices() {
            int axis = normal.getAxis().ordinal(), a = (axis + 1) % 3, b = (axis + 2) % 3;
            float[][] vertices = new float[4][3];
            float[] us = {minA, maxA, maxA, minA}, vs = {minB, minB, maxB, maxB};
            for (int i = 0; i < 4; i++) {
                int k = normal.getAxisDirection() == Direction.AxisDirection.POSITIVE ? i : 3 - i;
                vertices[i][axis] = plane; vertices[i][a] = us[k]; vertices[i][b] = vs[k];
            }
            return vertices;
        }
    }

    static List<BakedQuad> bake(int mask, TextureAtlasSprite sprite) {
        List<BakedQuad> quads = new ArrayList<>();
        for (Face face : faces(mask)) {
            int[] data = new int[32];
            float[][] positions = face.vertices();
            for (int i = 0; i < 4; i++) {
                int k = i * 8;
                for (int axis = 0; axis < 3; axis++) data[k + axis] = Float.floatToRawIntBits(positions[i][axis] / 16);
                data[k + 3] = -1;
                // Sample only the established opaque bevel pixel, inset from its UV edges.
                data[k + 4] = Float.floatToRawIntBits(sprite.getU((i < 2 ? 0.25f : 0.75f) / 16));
                data[k + 5] = Float.floatToRawIntBits(sprite.getV((i == 0 || i == 3 ? 0.25f : 0.75f) / 16));
                data[k + 7] = (face.normal().getStepX() * 127 & 255)
                        | ((face.normal().getStepY() * 127 & 255) << 8)
                        | ((face.normal().getStepZ() * 127 & 255) << 16);
            }
            quads.add(new BakedQuad(data, -1, face.normal(), sprite, true, false));
        }
        return List.copyOf(quads);
    }
}
