package korniltsev.telegram.charts.gl;

import android.opengl.GLES20;

public final class SimpleShader {
    final String vertexShader =
            "uniform mat4 u_MVPMatrix;      \n"
                    + "attribute vec2 a_Position;     \n"
                    + "void main()                    \n"
                    + "{                              \n"
                    + "   gl_Position = u_MVPMatrix * vec4(a_Position.xy, 0.0, 1.0);   \n"
                    + "}                              \n";

    final String fragmentShader =
            "precision mediump float;       \n"
                    + "uniform vec4 u_color;       \n"
                    + "void main()                    \n"
                    + "{                              \n"
                    + "   gl_FragColor = u_color;     \n"
                    + "}                              \n";
    public final int MVPHandle;
    public final int positionHandle;
    public final int program;
    public final int colorHandle;
    private boolean released;

    public SimpleShader() {

        program = MyGL.createProgram(vertexShader, fragmentShader);
        MVPHandle = GLES20.glGetUniformLocation(program, "u_MVPMatrix");
        positionHandle = GLES20.glGetAttribLocation(program, "a_Position");
        colorHandle = GLES20.glGetUniformLocation(program, "u_color");
    }

    public void release() {
        if (released) {
            return;
        }
        released = true;
        GLES20.glDeleteProgram(program);
    }
}
