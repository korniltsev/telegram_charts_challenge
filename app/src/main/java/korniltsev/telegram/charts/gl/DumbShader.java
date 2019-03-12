package korniltsev.telegram.charts.gl;

import android.opengl.GLES20;

public final class DumbShader {
    final String vertexShader =
            "\n"
                    + "attribute vec2 a_Position;     \n"
                    + "void main()                    \n"
                    + "{                              \n"
                    + "   gl_Position = vec4(a_Position.xy, 0.0, 1.0);   \n"
                    + "}                              \n";

    final String fragmentShader =
            "precision mediump float;       \n"
                    + "uniform vec4 u_color;       \n"
                    + "void main()                    \n"
                    + "{                              \n"
                    + "   gl_FragColor = u_color;     \n"
                    + "}                              \n";
    public final int positionHandle;
    public final int program;
    public final int colorHandle;
    private boolean released;

    public DumbShader() {
        program = MyGL.createProgram(vertexShader, fragmentShader);
        positionHandle = GLES20.glGetAttribLocation(program, "a_Position");
        colorHandle = GLES20.glGetUniformLocation(program, "u_color");
    }

    public final void use() {
        GLES20.glUseProgram(program);
    }

    public void release() {
        if (released) {
            return;
        }
        released = true;
        GLES20.glDeleteProgram(program);
    }
}
