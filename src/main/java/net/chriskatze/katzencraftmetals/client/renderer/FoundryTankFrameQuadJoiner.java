package net.chriskatze.katzencraftmetals.client.renderer;

import net.minecraft.client.renderer.block.model.BakedQuad;
import java.util.ArrayList;
import java.util.List;

/** Removes only coplanar, same-facing intersections from the chunk-baked frame. */
final class FoundryTankFrameQuadJoiner {
    private FoundryTankFrameQuadJoiner() {}
    private record Rect(float minA, float maxA, float minB, float maxB) {}
    private record Face(BakedQuad quad, int axis, float plane, Rect rect) {}

    static List<BakedQuad> join(List<BakedQuad> input) {
        List<Face> accepted = new ArrayList<>();
        List<BakedQuad> result = new ArrayList<>();
        for (BakedQuad quad : input) {
            Face face = face(quad);
            List<Rect> remaining = new ArrayList<>(); remaining.add(face.rect());
            for (Face previous : accepted) {
                if (previous.axis() != face.axis() || previous.plane() != face.plane()
                        || previous.quad().getDirection() != quad.getDirection()) continue;
                List<Rect> next = new ArrayList<>();
                for (Rect rect : remaining) subtract(rect, previous.rect(), next);
                remaining = next;
            }
            for (Rect rect : remaining) {
                BakedQuad clipped = rect.equals(face.rect()) ? quad : clip(face, rect);
                accepted.add(new Face(clipped, face.axis(), face.plane(), rect)); result.add(clipped);
            }
        }
        return List.copyOf(result);
    }

    private static Face face(BakedQuad quad) {
        int axis = quad.getDirection().getAxis().ordinal(), a = (axis + 1) % 3, b = (axis + 2) % 3;
        int[] data = quad.getVertices();
        float minA = Float.POSITIVE_INFINITY, maxA = Float.NEGATIVE_INFINITY;
        float minB = Float.POSITIVE_INFINITY, maxB = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < 4; i++) {
            float u = Float.intBitsToFloat(data[i * 8 + a]), v = Float.intBitsToFloat(data[i * 8 + b]);
            minA = Math.min(minA, u); maxA = Math.max(maxA, u);
            minB = Math.min(minB, v); maxB = Math.max(maxB, v);
        }
        return new Face(quad, axis, Float.intBitsToFloat(data[axis]), new Rect(minA, maxA, minB, maxB));
    }

    private static void subtract(Rect r, Rect cut, List<Rect> out) {
        float a0 = Math.max(r.minA(), cut.minA()), a1 = Math.min(r.maxA(), cut.maxA());
        float b0 = Math.max(r.minB(), cut.minB()), b1 = Math.min(r.maxB(), cut.maxB());
        if (a0 >= a1 || b0 >= b1) { out.add(r); return; }
        if (r.minA() < a0) out.add(new Rect(r.minA(), a0, r.minB(), r.maxB()));
        if (a1 < r.maxA()) out.add(new Rect(a1, r.maxA(), r.minB(), r.maxB()));
        if (r.minB() < b0) out.add(new Rect(a0, a1, r.minB(), b0));
        if (b1 < r.maxB()) out.add(new Rect(a0, a1, b1, r.maxB()));
    }

    private static BakedQuad clip(Face face, Rect rect) {
        int a = (face.axis() + 1) % 3, b = (face.axis() + 2) % 3;
        int[] source = face.quad().getVertices(), data = source.clone();
        Rect old = face.rect();
        for (int i = 0; i < 4; i++) {
            float oldA = Float.intBitsToFloat(source[i * 8 + a]);
            float oldB = Float.intBitsToFloat(source[i * 8 + b]);
            float newA = oldA == old.minA() ? rect.minA() : rect.maxA();
            float newB = oldB == old.minB() ? rect.minB() : rect.maxB();
            data[i * 8 + a] = Float.floatToRawIntBits(newA);
            data[i * 8 + b] = Float.floatToRawIntBits(newB);
            for (int uv = 4; uv <= 5; uv++) {
                float value = 0;
                for (int j = 0; j < 4; j++) {
                    float x = Float.intBitsToFloat(source[j * 8 + a]);
                    float y = Float.intBitsToFloat(source[j * 8 + b]);
                    float wa = x == old.minA() ? (old.maxA() - newA) / (old.maxA() - old.minA())
                            : (newA - old.minA()) / (old.maxA() - old.minA());
                    float wb = y == old.minB() ? (old.maxB() - newB) / (old.maxB() - old.minB())
                            : (newB - old.minB()) / (old.maxB() - old.minB());
                    value += Float.intBitsToFloat(source[j * 8 + uv]) * wa * wb;
                }
                data[i * 8 + uv] = Float.floatToRawIntBits(value);
            }
        }
        return new BakedQuad(data, face.quad().getTintIndex(), face.quad().getDirection(),
                face.quad().getSprite(), face.quad().isShade(), face.quad().hasAmbientOcclusion());
    }
}
