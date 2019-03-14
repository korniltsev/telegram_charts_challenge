package korniltsev.telegram.charts;

import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

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
            +"uniform vec4 u_color;       \n"
                    + "void main()                    \n"
                    + "{                              \n"
                    + "   gl_FragColor = u_color;     \n"
//                    + "   gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);     \n"
                    + "}                              \n";
    private final int MVPHandle;
    private final int positionHandle;
    private final int program;
    private final int vbo;
    private final float[] vertices;
    private final FloatBuffer buf1;
    private final ColumnData column;
    private final int colorHandle;

    private float[] MVP = new float[16];

    public final int w;
    public final int h;

    private final Dimen dimen;

    float maxValue;

    public GLChartProgram(ColumnData column, int w, int h, Dimen dimen) {
        this.w = w;
        this.h = h;
        this.column = column;
        this.dimen = dimen;

        long[] values = column.values;
        vertices = new float[values.length * 2];
        for (int i = 0, valuesLength = values.length; i < valuesLength; i++) {
            long value = values[i];
            vertices[i * 2] = i;
            vertices[i * 2 + 1] = value;
        }
        buf1 = ByteBuffer.allocateDirect(vertices.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        buf1.put(vertices);
        buf1.position(0);


        program = MyGL.createProgram(vertexShader, fragmentShader);
        MVPHandle = GLES20.glGetUniformLocation(program, "u_MVPMatrix");
        positionHandle = GLES20.glGetAttribLocation(program, "a_Position");
        colorHandle = GLES20.glGetUniformLocation(program, "u_color");


        int[] vbos = new int[1];
        GLES20.glGenBuffers(1, vbos, 0);
        vbo = vbos[0];
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertices.length * BYTES_PER_FLOAT, buf1, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

    }

    float prevt = -1.0f;
    public final void draw(float t) {

//        float t = SystemClock.elapsedRealtime() / 1000f;
        float d = t - prevt;
        Log.d("tttt", "" + d);
        prevt = t;
        t = 1.5f - Math.abs(t % 1.0f - 0.5f);


        GLES20.glUseProgram(program);

        float[] colors = new float[]{
                Color.red(column.color) / 255f,
                Color.green(column.color) / 255f,
                Color.blue(column.color) / 255f,
                1.0f,
        };
        GLES20.glUniform4fv(colorHandle, 1, colors, 0);


        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
        GLES20.glVertexAttribPointer(positionHandle, POSITION_DATA_SIZE, GLES20.GL_FLOAT, false, STRIDE_BYTES, 0);

        float hpadding = dimen.dpf(16);
        float minx = vertices[0];
        float maxx = vertices[vertices.length - 2];
        float scalex = 2.0f / (maxx - minx + hpadding * 2);
        float scaley = 2.0f / (maxValue);

        Matrix.setIdentityM(MVP, 0);

        Matrix.translateM(MVP, 0, -1.0f, -1.0f, 0);
        Matrix.scaleM(MVP, 0, scalex, scaley, 1.0f);
        Matrix.translateM(MVP, 0, hpadding, 0, 0);
        Matrix.scaleM(MVP, 0, 1.0f, t, 1.0f);

        GLES20.glUniformMatrix4fv(MVPHandle, 1, false, MVP, 0);
        GLES20.glLineWidth(dimen.dpf(1f));
        GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, vertices.length / 2);
    }
}
