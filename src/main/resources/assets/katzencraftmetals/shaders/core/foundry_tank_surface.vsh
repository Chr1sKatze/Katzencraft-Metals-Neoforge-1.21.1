#version 150
#moj_import <light.glsl>
#moj_import <fog.glsl>
in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;
uniform sampler2D Sampler2;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;
uniform vec3 Light0_Direction;
uniform vec3 Light1_Direction;
uniform vec4 RimWidths;
uniform vec2 RimCounts;
out float vertexDistance;
out vec4 liquidLight;
out vec4 rimColor;
out vec2 texCoord0;
out vec2 localXZ;
flat out int edges;
flat out float rimWidth;
flat out float inset;
void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexDistance = fog_distance(Position, FogShape);
    // Low bytes retain the original full-bright liquid lightmap coordinates.
    vec4 lightMap = texelFetch(Sampler2, (UV2 & ivec2(255)) / 16, 0);
    liquidLight = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, vec4(1.0)) * lightMap;
    rimColor = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, Color) * lightMap;
    texCoord0 = UV0;
    localXZ = vec2(UV1) / 32767.0;
    edges = (UV2.x >> 8) & 255;
    float cells = float((UV2.y >> 8) & 255);
    rimWidth = cells <= RimCounts.x
        ? mix(RimWidths.y, RimWidths.z, clamp((cells - 1.0) / (RimCounts.x - 1.0), 0.0, 1.0))
        : mix(RimWidths.z, RimWidths.w, clamp((cells - RimCounts.x) / (RimCounts.y - RimCounts.x), 0.0, 1.0));
    inset = RimWidths.x;
}
