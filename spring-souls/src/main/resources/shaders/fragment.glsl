#version 330 core

in vec3 FragPos;
in vec3 Normal;
in vec2 TexCoord;

uniform vec3 lightDir;
uniform vec3 lightColor;
uniform float ambientStrength;
uniform vec3 objectColor;
uniform bool useTexture;
uniform sampler2D diffuseTexture;
uniform vec3 viewPos;

// Fog
uniform vec3 fogColor;
uniform float fogNear;
uniform float fogFar;

out vec4 FragColor;

void main() {
    vec3 norm = normalize(Normal);
    vec3 dir = normalize(-lightDir);

    // Ambient — slightly brighter on surfaces facing up to fake sky light
    float skyFactor = max(norm.y, 0.0) * 0.08;
    vec3 ambient = (ambientStrength + skyFactor) * lightColor;

    // Diffuse — wrap lighting to soften shadow boundary
    float NdotL = dot(norm, dir);
    float diff = max(NdotL * 0.5 + 0.5, 0.0); // half-Lambert
    vec3 diffuse = diff * lightColor * 0.7;

    // Specular (Blinn-Phong)
    vec3 viewDir = normalize(viewPos - FragPos);
    vec3 halfDir = normalize(dir + viewDir);
    float spec = pow(max(dot(norm, halfDir), 0.0), 32.0);
    vec3 specular = spec * lightColor * 0.3;

    vec3 baseColor;
    if (useTexture) {
        baseColor = texture(diffuseTexture, TexCoord).rgb;
    } else {
        baseColor = objectColor;
    }

    vec3 result = (ambient + diffuse) * baseColor + specular;

    // Distance fog
    float dist = length(viewPos - FragPos);
    float fogFactor = clamp((fogFar - dist) / (fogFar - fogNear), 0.0, 1.0);
    result = mix(fogColor, result, fogFactor);

    FragColor = vec4(result, 1.0);
}
