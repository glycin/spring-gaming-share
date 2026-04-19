#version 330 core

layout (location = 0) in vec2 aPos;

uniform mat4 projection;
uniform vec2 offset;
uniform vec2 size;
uniform vec2 uvOffset;
uniform vec2 uvSize;

out vec2 TexCoord;

void main() {
    vec2 worldPos = offset + aPos * size;
    gl_Position = projection * vec4(worldPos, 0.0, 1.0);
    TexCoord = uvOffset + aPos * uvSize;
}
