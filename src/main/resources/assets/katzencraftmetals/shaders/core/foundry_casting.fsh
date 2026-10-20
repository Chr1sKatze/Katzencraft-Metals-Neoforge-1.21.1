#version 150
#moj_import <fog.glsl>
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
in float vertexDistance;
in vec4 directionLight;
in vec4 worldLight;
in vec2 moltenUV;
in vec2 solidUV;
in float cooling;
flat in int topFace;
out vec4 fragColor;
void main() {
    float t = smoothstep(0.0, 1.0, cooling);
    vec4 hot = texture(Sampler0, moltenUV);
    // The top keeps its established painted-region UVs. Side faces use a painted edge row.
    vec2 coldUV = topFace != 0 ? solidUV : vec2(solidUV.x, 4.5 / 16.0);
    vec4 cold = texture(Sampler1, coldUV) * worldLight;
    vec4 color = mix(hot, cold, t) * directionLight * ColorModulator;
    if (color.a < 0.1) discard;
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
