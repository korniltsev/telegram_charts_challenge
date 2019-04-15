//todo if optimizing, try batch as mat4 ?
attribute float a_v0 ;
attribute float a_v1 ;
attribute float a_v2 ;
attribute float a_v3 ;
attribute float a_v4 ;
attribute float a_v5 ;
attribute float a_v6 ;
attribute float a_x;
attribute float   a_zeroOrValue;
attribute float   a_xNo;



uniform float u_columnNo;
uniform mat4 u_V;
uniform mat4 u_P;
uniform float u_selected_index;
uniform float u_v0;
uniform float u_v1;
uniform float u_v2;
uniform float u_v3;
uniform float u_v4;
uniform float u_v5;
uniform float u_v6;

uniform vec4 u_color;
varying vec4 v_color;
void main() {
    if (u_selected_index >= 0.0) {
        if (a_xNo == u_selected_index) {
            v_color = u_color;
        } else {
            v_color = vec4(u_color.xyz, 0.5 *u_color.w);
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
            y = a_v0 * u_v0;
        }
    } else if (u_columnNo == 1.0) {
        if (a_zeroOrValue == 0.0) {
            y = a_v0 * u_v0;
        } else {
            y = a_v0 * u_v0 + a_v1 * u_v1;
        }
    } else if (u_columnNo == 2.0) {
        if (a_zeroOrValue == 0.0) {
            y = a_v0 * u_v0 + a_v1 * u_v1;
        } else {
            y = a_v0 * u_v0 + a_v1 * u_v1 + a_v2 * u_v2;
        }
    } else if (u_columnNo == 3.0) {
        if (a_zeroOrValue == 0.0) {
            y = a_v0 * u_v0 + a_v1 * u_v1 + a_v2 * u_v2;
        } else {
            y = a_v0 * u_v0 + a_v1 * u_v1 + a_v2 * u_v2 + a_v3 * u_v3;
        }
    } else if (u_columnNo == 4.0) {
        if (a_zeroOrValue == 0.0) {
            y = a_v0 * u_v0 + a_v1 * u_v1 + a_v2 * u_v2 + a_v3 * u_v3;
        } else {
            y = a_v0 * u_v0 + a_v1 * u_v1 + a_v2 * u_v2 + a_v3 * u_v3 + a_v4 * u_v4;
        }
    } else if (u_columnNo == 5.0) {
        if (a_zeroOrValue == 0.0) {
            y = a_v0 * u_v0 + a_v1 * u_v1 + a_v2 * u_v2 + a_v3 * u_v3 + a_v4 * u_v4;
        } else {
            y = a_v0 * u_v0 + a_v1 * u_v1 + a_v2 * u_v2 + a_v3 * u_v3 + a_v4 * u_v4 + a_v5 * u_v5;
        }
    } else if (u_columnNo == 6.0) {
        if (a_zeroOrValue == 0.0) {
            y = a_v0 * u_v0 + a_v1 * u_v1 + a_v2 * u_v2 + a_v3 * u_v3 + a_v4 * u_v4 + a_v5 * u_v5;
        } else {
            y = a_v0 * u_v0 + a_v1 * u_v1 + a_v2 * u_v2 + a_v3 * u_v3 + a_v4 * u_v4 + a_v5 * u_v5 + a_v6 * u_v6;
        }
    }
    gl_Position = MVP * vec4(a_x, y, 0.0, 1.0);
}
