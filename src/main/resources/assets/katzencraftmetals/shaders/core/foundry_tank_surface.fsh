#version 150
#moj_import <fog.glsl>
uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
in float vertexDistance;
in vec4 liquidLight;
in vec4 rimColor;
in vec2 texCoord0;
in vec2 localXZ;
flat in int edges;
flat in int sideSurface;
flat in float rimWidth;
flat in float inset;
out vec4 fragColor;
const float RIM_INTENSITY = 1.18;
float edgeWeight(float distance) { return clamp(1.0 - distance / rimWidth, 0.0, 1.0); }
float cornerWeight(vec2 distance) {
    return edgeWeight(max(distance.x, distance.y) - inset);
}
void main() {
    vec4 liquid = texture(Sampler0, texCoord0) * liquidLight * ColorModulator;
    if (liquid.a < 0.1) discard;
    float glow = 0.0;
    if (sideSurface != 0) {
        // UV1 carries depth and fade distance, not edge coordinates on sides.
        glow = 1.0 * (1.0 - smoothstep(0.0, max(localXZ.y, 1.0 / 32767.0), localXZ.x));
    } else {
        if ((edges & 1) != 0) glow = max(glow, edgeWeight(localXZ.y - inset));
        if ((edges & 2) != 0) glow = max(glow, edgeWeight(1.0 - inset - localXZ.y));
        if ((edges & 4) != 0) glow = max(glow, edgeWeight(localXZ.x - inset));
        if ((edges & 8) != 0) glow = max(glow, edgeWeight(1.0 - inset - localXZ.x));
        if ((edges & 16) != 0) glow = max(glow, cornerWeight(localXZ));
        if ((edges & 32) != 0) glow = max(glow, cornerWeight(vec2(1.0-localXZ.x, localXZ.y)));
        if ((edges & 64) != 0) glow = max(glow, cornerWeight(vec2(localXZ.x, 1.0-localXZ.y)));
        if ((edges & 128) != 0) glow = max(glow, cornerWeight(1.0-localXZ));
    }
    // Source-over color composition, once, on the liquid fragment itself.
    // MAX coverage keeps intersecting edge/corner influences from accumulating brightness.
    vec4 glowColor = rimColor * ColorModulator;
    liquid.rgb = mix(liquid.rgb, glowColor.rgb, glow * glowColor.a * RIM_INTENSITY);
    fragColor = linear_fog(liquid, vertexDistance, FogStart, FogEnd, FogColor);
}
