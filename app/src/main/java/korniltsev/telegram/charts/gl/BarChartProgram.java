package korniltsev.telegram.charts.gl;

import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import korniltsev.telegram.charts.data.ColumnData;
import korniltsev.telegram.charts.ui.Dimen;
import korniltsev.telegram.charts.ui.MyColor;

import static korniltsev.telegram.charts.gl.GLChartProgram.BYTES_PER_FLOAT;

public class BarChartProgram {
    private final ColumnData column;
    private final int w;
    private final int h;
    private final Dimen dimen;
    private final ChartViewGL root;
    private final boolean scrollbar;
    private final float[] vertices;
    private final int[] vbos;
    private final int vbo;

    private final SimpleShader shader;
    public BarChartProgram(ColumnData column, int w, int h, Dimen dimen, ChartViewGL root, boolean scrollbar, SimpleShader simple) {
        this.column = column;
        this.w = w;
        this.h = h;
        this.dimen = dimen;
        this.root = root;
        this.scrollbar = scrollbar;
        this.shader = simple;


        long[] values = column.values;

        int n = values.length;
//        int n = values.length/20;
        vertices = new float[n * 12];
        for (int i = 0, valuesLength = n; i < valuesLength; i++) {
            long value = values[i];
            vertices[12 * i] = i;
            vertices[12 * i + 1] = 0;

            vertices[12 * i + 2] = i;
            vertices[12 * i + 3] = value;

            vertices[12 * i + 4] = i + 1;
            vertices[12 * i + 5] = 0;

            vertices[12 * i + 6] = i + 1;
            vertices[12 * i + 7] = 0;

            vertices[12 * i + 8] = i;
            vertices[12 * i + 9] = value;

            vertices[12 * i + 10] = i + 1;
            vertices[12 * i + 11] = value;
        }
        FloatBuffer buf1 = ByteBuffer.allocateDirect(vertices.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        buf1.put(vertices);
        buf1.position(0);


        vbos = new int[1];
        GLES20.glGenBuffers(1, vbos, 0);
        vbo = vbos[0];
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertices.length * BYTES_PER_FLOAT, buf1, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    float[] colors = new float[4];
    float[] MVP = new float[16];
    float[] V = new float[16];
    public void prepare(float []PROJ) {
        float hpadding = dimen.dpf(16);
        float maxx = vertices[vertices.length - 2];

        Matrix.setIdentityM(V, 0);
        if (scrollbar) {
//            hpadding += dimen.dpf(1);
//            final float dip2 = dimen.dpf(2);

            final float w = this.w - 2 * hpadding;
            final float h = root.dimen_scrollbar_height;
            final float yscale = h / (column.max - 0);
            final float dy = -yscale * 0;

            Matrix.translateM(V, 0, hpadding, root.dimen_v_padding8 + root.checkboxesHeight, 0);
            Matrix.translateM(V, 0, 0, dy, 0);
            Matrix.scaleM(V, 0, w / ((maxx)), yscale, 1.0f);
            Matrix.multiplyMM(MVP, 0, PROJ, 0, V, 0);
        } else {
            throw new AssertionError("unimplemented");
        }

    }
    public boolean draw(long t) {
        shader.use();



        MyColor.set(colors, column.color);
        GLES20.glUniform4fv(shader.colorHandle, 1, colors, 0);
        GLES20.glEnableVertexAttribArray(shader.positionHandle);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
        GLES20.glVertexAttribPointer(shader.positionHandle, 2, GLES20.GL_FLOAT, false, 8, 0);

        GLES20.glUniformMatrix4fv(shader.MVPHandle, 1, false, MVP, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertices.length / 2);
        MyGL.checkGlError2();
        return false;
    }
}
