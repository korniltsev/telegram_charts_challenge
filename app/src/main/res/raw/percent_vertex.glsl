//todo if optimizing, try batch as mat4 ?
attribute float a_v0 ;
attribute float a_v1 ;
attribute float a_v2 ;
attribute float a_v3 ;
attribute float a_v4 ;
attribute float a_v5 ;

attribute float a_x;
attribute float   a_zeroOrValue;
//attribute float   a_xNo;



uniform float u_columnNo;
uniform mat4 u_V;
uniform mat4 u_P;
//uniform float u_selected_index;
uniform float u_v0;
uniform float u_v1;
uniform float u_v2;
uniform float u_v3;
uniform float u_v4;
uniform float u_v5;

uniform float u_chart_height;


uniform vec4 u_color;
varying vec4 v_color;
void main() {
//    if (u_selected_index >= 0.0) {
//        if (a_xNo == u_selected_index) {
//            v_color = u_color;
//        } else {
//            v_color = vec4(u_color.xyz, 0.5);
//        }
//    } else {
        v_color = u_color;
//    }
    mat4 MVP = u_P * u_V;
    float y;
    float x;
    float v0 = a_v0 * u_v0;
    float v1 = a_v1 * u_v1;
    float v2 = a_v2 * u_v2;
    float v3 = a_v3 * u_v3;
    float v4 = a_v4 * u_v4;
    float v5 = a_v5 * u_v5;
    float sum = v0 + v1 + v2 + v3 + v4 + v5;
    if (u_columnNo == 0.0) {
        if (a_zeroOrValue == 0.0) {
            y = 0.0;
        } else {
            y = (v0)/sum;
        }
    } else if (u_columnNo == 1.0) {
        if (a_zeroOrValue == 0.0) {
            y = v0/sum;
        } else {
            y = (v0+v1)/sum;
        }
    } else if (u_columnNo == 2.0) {
        if (a_zeroOrValue == 0.0) {
            y = (v0+v1)/sum;
        } else {
            y = (v0+v1+v2)/sum;
        }
    } else if (u_columnNo == 3.0) {
        if (a_zeroOrValue == 0.0) {
            y = (v0+v1+v2)/sum;
        } else {
            y = (v0+v1+v2+v3)/sum;
        }
    } else if (u_columnNo == 4.0) {
        if (a_zeroOrValue == 0.0) {
            y = (v0+v1+v2+v3)/sum;
        } else {
            y = (v0+v1+v2+v3+v4)/sum;
        }
    } else if (u_columnNo == 5.0) {
        if (a_zeroOrValue == 0.0) {
            y = (v0+v1+v2+v3+v4)/sum;
        } else {
            y = (v0+v1+v2+v3+v4+v5)/sum;
        }
    }
    gl_Position = MVP * vec4(a_x, y, 0.0, 1.0);
}
