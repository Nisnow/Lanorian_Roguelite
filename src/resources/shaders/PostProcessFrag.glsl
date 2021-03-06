#version 150 core

uniform sampler2D texture_diffuse;

in vec2 pass_TextureCoord;

out vec4 out_Color;

void main(void) 
{
	//out_Color = texture(texture_diffuse, pass_TextureCoord);
	out_Color = vec4(vec3(1.0 - texture(texture_diffuse, pass_TextureCoord)), 1.0);
}