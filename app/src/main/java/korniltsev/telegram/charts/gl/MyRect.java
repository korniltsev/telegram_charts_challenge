package korniltsev.telegram.charts.gl;

import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import korniltsev.telegram.charts.ui.MyColor;

public class MyRect {
    private final float[] MVP = new float[16];
    final float w;
    final float h;
    final float x;
    final float y;
    private final float[] vertices;
    private int color;

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
//                    + "   gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);     \n"
                    + "}                              \n";
    private final int program;
    private final int MVPHandle;
    private final int positionHandle;
    private final int colorHandle;
    private int vbo;

    private final float canvasw;
    private final float canvash;

    public MyRect (float w,float h, float x, float y, int color, float canvasw, float canvash) {
        this.w = w;
        this.h = h;
        this.x = x;
        this.y = y;
        this.color = color;
        this.canvasw = canvasw;
        this.canvash = canvash;


        program = MyGL.createProgram(vertexShader, fragmentShader);
        MVPHandle = GLES20.glGetUniformLocation(program, "u_MVPMatrix");
        positionHandle = GLES20.glGetAttribLocation(program, "a_Position");
        colorHandle = GLES20.glGetUniformLocation(program, "u_color");

        vertices = new float[]{
                0f, 0f,
                0f, 1f,
                1f, 0f,
                1f, 1f,
        };
        FloatBuffer buf1 = ByteBuffer.allocateDirect(vertices.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        buf1.put(vertices);
        buf1.position(0);

        int[] vbos = new int[1];
        GLES20.glGenBuffers(1, vbos, 0);
        vbo = vbos[0];
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertices.length * 4, buf1, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    public final void draw(){

        GLES20.glUseProgram(program);
        float[] colors = new float[]{
                MyColor.red(color)/255f,
                MyColor.green(color)/255f,
                MyColor.blue(color)/255f,
                MyColor.alpha(color)/255f,
        };
        GLES20.glUniform4fv(colorHandle, 1, colors, 0);//todo try to bind only once

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 8, 0);


        final float scalex = 2.0f / canvasw;
        final float scaley = 2.0f / canvash;
        Matrix.setIdentityM(MVP, 0);
        Matrix.translateM(MVP, 0, -1.0f, -1.0f, 0);
        Matrix.scaleM(MVP, 0, scalex, scaley, 1.0f);

        Matrix.translateM(MVP, 0, x, y, 0);
        Matrix.scaleM(MVP, 0, w, h, 1.0f);

//        GLES20.glLineWidth(20);
        GLES20.glUniformMatrix4fv(MVPHandle, 1, false, MVP, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, vertices.length / 2);
//        GLES20.glDrawArrays(GLES20.GL_LINES, 0, vertices.length / 2);



    }
}
