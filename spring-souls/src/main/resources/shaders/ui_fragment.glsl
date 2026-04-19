#version 330 core

uniform vec4 color;
uniform bool useTexture;
uniform sampler2D fontTexture;

in vec2 TexCoord;
out vec4 FragColor;

void main() {
    if (useTexture) {
        float a = texture(fontTexture, TexCoord).r;
        FragColor = vec4(color.rgb, color.a * a);
    } else {
        FragColor = color;
    }
}
