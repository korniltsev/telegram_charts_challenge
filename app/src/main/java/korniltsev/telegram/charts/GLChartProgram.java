package korniltsev.telegram.charts;

import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import korniltsev.telegram.charts.gl.MyGL;

public final class GLChartProgram {

    private static final int BYTES_PER_FLOAT = 4;
    private static final int STRIDE_BYTES = 2 * BYTES_PER_FLOAT;
    private static final int POSITION_DATA_SIZE = 2;

    final String vertexShader =
            "uniform mat4 u_MVPMatrix;      \n"
                    + "attribute vec2 a_Position;     \n"
                    + "void main()                    \n"
                    + "{                              \n"
                    + "   gl_Position = u_MVPMatrix * vec4(a_Position.xy, 0.0, 1.0);   \n"
                    + "}                              \n";

    final String fragmentShader =
            "precision mediump float;       \n"
                    + "void main()                    \n"
                    + "{                              \n"
                    + "   gl_FragColor = vec4(0.0, 1.0, 0.0, 1.0);     \n"
                    + "}                              \n";
    private final int MVPHandle;
    private final int positionHandle;
    private final int program;
    private final int vbo;
    private final float[] vertices;
    private final FloatBuffer buf1;

    private float[] MVP = new float[16];

    public final int w;
        public final int h;

    public GLChartProgram(ColumnData column, int w, int h) {
        this.w = w;
        this.h = h;
//        long[] values = column.values;
//
//            vertices = new float[values.length * 2];
//            for (int i = 0, valuesLength = values.length; i < valuesLength; i++) {
//                long value = values[i];
//                vertices[i * 2] = i;
//                vertices[i * 2 + 1] = value;
//            }
        vertices = new float[]{
                0.0f, 0.0f,
                0.5f,  1.0f,
                1.0f, 0f,
                2.0f, 0.25f,
                3.0f, 1.0f,
                4.0f, 0.25f,
                5.0f, 0.0f,
        };


        buf1 = ByteBuffer.allocateDirect(vertices.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        buf1.put(vertices);
        buf1.position(0);


        program = MyGL.createProgram(vertexShader, fragmentShader);
        MVPHandle = GLES20.glGetUniformLocation(program, "u_MVPMatrix");
        positionHandle = GLES20.glGetAttribLocation(program, "a_Position");


        int[] vbos = new int[1];
        GLES20.glGenBuffers(1, vbos, 0);
        vbo = vbos[0];
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertices.length * BYTES_PER_FLOAT, buf1, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }




    public final void draw() {
        GLES20.glUseProgram(program);


        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
        GLES20.glVertexAttribPointer(positionHandle, POSITION_DATA_SIZE, GLES20.GL_FLOAT, false, STRIDE_BYTES, 0);

//            Matrix.setIdentityM(model, 0);
//            Matrix.scaleM(model, 0, 1.0f, 1.0f, 1.0f);

//            Matrix.multiplyMM(MVP, 0, view, 0, model, 0);
//            Matrix.multiplyMM(MVP, 0, projection, 0, MVP, 0);

        float scalex = 2.0f;
        float scaley = 2.0f;
        Matrix.setIdentityM(MVP, 0);

        Matrix.translateM(MVP, 0, -1.0f, -1.0f, 0);
        Matrix.scaleM(MVP, 0, scalex, scaley, 1.0f);

        GLES20.glUniformMatrix4fv(MVPHandle, 1, false, MVP, 0);
        GLES20.glLineWidth(8f);
        GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, 3);
    }
}
