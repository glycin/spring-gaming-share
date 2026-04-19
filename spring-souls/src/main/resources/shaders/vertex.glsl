#version 330 core

layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aNormal;
layout (location = 2) in vec2 aTexCoord;
layout (location = 3) in ivec4 aBoneIds;
layout (location = 4) in vec4 aBoneWeights;

const int MAX_BONES = 128;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;
uniform bool animated;
uniform mat4 boneMatrices[MAX_BONES];

out vec3 FragPos;
out vec3 Normal;
out vec2 TexCoord;

void main() {
    vec4 localPos = vec4(aPos, 1.0);
    vec3 localNormal = aNormal;

    if (animated) {
        mat4 boneTransform = boneMatrices[aBoneIds[0]] * aBoneWeights[0]
                           + boneMatrices[aBoneIds[1]] * aBoneWeights[1]
                           + boneMatrices[aBoneIds[2]] * aBoneWeights[2]
                           + boneMatrices[aBoneIds[3]] * aBoneWeights[3];
        localPos = boneTransform * localPos;
        localNormal = mat3(boneTransform) * localNormal;
    }

    FragPos = vec3(model * localPos);
    Normal = mat3(transpose(inverse(model))) * localNormal;
    TexCoord = aTexCoord;
    gl_Position = projection * view * model * localPos;
}
