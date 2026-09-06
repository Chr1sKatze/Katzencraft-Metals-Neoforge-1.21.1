#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec4 vertexColor;
in vec4 lightMapColor;
in vec2 texCoord0;
in float blendDistance;

out vec4 fragColor;

void main() {
    vec4 lowerColor =
            texture(
                    Sampler0,
                    texCoord0
            );

    vec4 upperColor =
            texture(
                    Sampler1,
                    texCoord0
            );

    /*
     * A real narrow crossfade centered on the moving wavy contact line.
     *
     * Outside +/-1 the shader is already fully one source metal or the other,
     * so the carved strip visually rejoins the normal side faces cleanly.
     */
    float t =
            smoothstep(
                    -1.0,
                    1.0,
                    blendDistance
            );

    vec4 color =
            mix(
                    lowerColor,
                    upperColor,
                    t
            );

    if (color.a < 0.1) {
        discard;
    }

    /*
     * Mirror the relevant Minecraft entity-translucent fragment treatment.
     * The first experiment skipped this entire lighting/modulation/fog path,
     * which is why the blend appeared as a bright rim.
     */
    color *=
            vertexColor
                    * ColorModulator;

    color *= lightMapColor;

    fragColor =
            linear_fog(
                    color,
                    vertexDistance,
                    FogStart,
                    FogEnd,
                    FogColor
            );
}
