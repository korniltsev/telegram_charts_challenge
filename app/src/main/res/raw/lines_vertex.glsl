uniform mat4 u_V;
uniform mat4 u_P;
uniform float u_animate_in_y;
uniform float u_animate_in_value;
attribute vec2 a_Position;
void main()
{
    vec4 screenPos = u_V * vec4(a_Position.xy, 0.0, 1.0);
    if (u_animate_in_y < 0.0 || u_animate_in_value == 1.0) {
        gl_Position = u_P * screenPos;
    } else {
        float diff = u_animate_in_value * (screenPos.y - u_animate_in_y);
        gl_Position = u_P * vec4(screenPos.x, u_animate_in_y + diff, 0.0, 1.0);
    }
}
