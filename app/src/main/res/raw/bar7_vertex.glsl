//float v0;
//float v1;
//float v2;
//float v3;
//float v4;
//float v5;
//float v6;
//float x;
//int zeroOrValue;// zero or value
//int columnNo;// 0 - 7
//int xNo;

attribute float a_v0;
attribute float a_v1;
attribute float a_v2;
attribute float a_v3;
attribute float a_v4;
attribute float a_v5;
attribute float a_v6;
attribute float a_x;
attribute float   a_zeroOrValue;
attribute float   a_xNo;



uniform float u_columnNo;
uniform mat4 u_V;
uniform mat4 u_P;
uniform float u_selected_index;

uniform vec4 u_color;
varying vec4 v_color;
void main() {
    if (u_selected_index >= 0.0) {
        if (a_xNo == u_selected_index) {
            v_color = u_color;
        } else {
            v_color = vec4(u_color.xyz, 0.5);
        }
    } else {
        v_color = u_color;
    }
    mat4 MVP = u_P * u_V;
    float y;
    float x;
    if (u_columnNo == 0.0) {
        if (a_zeroOrValue == 0.0) {
            y = 0.0;
        } else {
            y = a_v0;
        }
    } else if (u_columnNo == 1.0) {
        if (a_zeroOrValue == 0.0) {
            y = a_v0;
        } else {
            y = a_v0 + a_v1;
        }
    } else if (u_columnNo == 2.0) {
        if (a_zeroOrValue == 0.0) {
            y = a_v0 + a_v1;
        } else {
            y = a_v0 + a_v1 + a_v2;
        }
    }
    gl_Position = MVP * vec4(a_x, y, 0.0, 1.0);
}
