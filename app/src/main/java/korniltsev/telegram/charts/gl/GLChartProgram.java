package korniltsev.telegram.charts.gl;

import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import korniltsev.telegram.charts.data.ColumnData;
import korniltsev.telegram.charts.ui.Dimen;
import korniltsev.telegram.charts.ui.MyAnimation;

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
                    + "uniform vec4 u_color;       \n"
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
    public final ColumnData column;
    private final int colorHandle;
    public float zoom = 1f;//1 -- all, 0.2 - partial
    public float left = 0;

    private float[] MVP = new float[16];

    public final int w;
    public final int h;

    private final Dimen dimen;

    public long maxValue;
    public long minValue;
    public float minValueAnim = 1f;
    public float maxValueAnim = 1f;

    final ChartViewGL root;

    final boolean scrollbar;

    public GLChartProgram(ColumnData column, int w, int h, Dimen dimen, ChartViewGL root, boolean scrollbar) {

        this.w = w;
        this.h = h;
        this.column = column;
        this.dimen = dimen;
        this.root = root;
        this.scrollbar = scrollbar;

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

    public final boolean draw(long t) {
        boolean invalidate = false;
        GLES20.glUseProgram(program);
        MyGL.checkGlError2();

        if (alphaAnim != null) {
            alpha = alphaAnim.tick(t);
            if (alphaAnim.ended) {
                alphaAnim = null;
            } else {
                invalidate = true;
            }
        }
        if (minAnim != null) {
            minValueAnim = minAnim.tick(t);
            if (minAnim.ended) {
                minAnim = null;
            }else {
                invalidate = true;
            }
        }

        if (maxAnim != null) {
            maxValueAnim = maxAnim.tick(t);
            if (maxAnim.ended) {
                maxAnim = null;
            }else {
                invalidate = true;
            }
        }


        float[] colors = new float[]{ //todo try to do only once
                Color.red(column.color) / 255f,
                Color.green(column.color) / 255f,
                Color.blue(column.color) / 255f,
                alpha,
        };
        GLES20.glUniform4fv(colorHandle, 1, colors, 0);


        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
        GLES20.glVertexAttribPointer(positionHandle, POSITION_DATA_SIZE, GLES20.GL_FLOAT, false, STRIDE_BYTES, 0);

        float hpadding = dimen.dpf(16);
        float minx = vertices[0];
        float maxx = vertices[vertices.length - 2];
        float scalex = 2.0f / w;
        float scaley = 2.0f / h;


        Matrix.setIdentityM(MVP, 0);

        Matrix.translateM(MVP, 0, -1.0f, -1.0f, 0);
        Matrix.scaleM(MVP, 0, scalex, scaley, 1.0f);
        //todo learn matrixes ¯\_(ツ)_/¯
        if (scrollbar) {
            final float dip2 = dimen.dpf(2);
            Matrix.translateM(MVP, 0, hpadding, root.dimen_v_padding8 + dip2, 0);
            float w = this.w - 2 * hpadding;
            float h = root.dimen_scrollbar_height - 2 * dip2;
            float ydiff = maxValueAnim * maxValue - minValueAnim * minValue;
//            float xx = ydiff / minValue ;
            Matrix.translateM(MVP, 0, 0, 0, 0f);
            float yscale = h / ydiff;
            float dy = -yscale * minValue * minValueAnim;
            Matrix.translateM(MVP, 0, 0, dy, 0);
            Matrix.scaleM(MVP, 0, w / ((maxx - minx) ), yscale, 1.0f);
            GLES20.glLineWidth(dimen.dpf(1f));
        } else {
            int ypx = dimen.dpi(80);
            Matrix.translateM(MVP, 0, hpadding, ypx, 0);
            float w = this.w - 2 * hpadding;
//            int h = root.dimen_chart_height;
            int h = dimen.dpi(280);
            float xdiff = maxx - minx;
            Matrix.scaleM(MVP, 0, w / xdiff /zoom, h  /  (float)(maxValue * maxValueAnim ), 1.0f);
            Matrix.translateM(MVP, 0, -left * xdiff , 0f, 0f);
//            Matrix.scaleM(MVP, 0, w / ((maxx - minx) ), h  /  (float)(maxValue ), 1.0f);
            GLES20.glLineWidth(dimen.dpf(2f));
        }

//        Matrix.scaleM(MVP, 0, 5.0f, 1.0f, 1.0f);
        GLES20.glUniformMatrix4fv(MVPHandle, 1, false, MVP, 0);

        GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, vertices.length / 2);
        return invalidate;
    }

    boolean checked = true;
    MyAnimation.Float alphaAnim;
    MyAnimation.Float minAnim;
    MyAnimation.Float maxAnim;
    float alpha = 1f;

    public void animateAlpha(boolean isChecked) {
        if (this.checked != isChecked) {
            alphaAnim = new MyAnimation.Float(MyAnimation.ANIM_DRATION, alpha, isChecked ? 1.0f : 0.0f);
            this.checked = isChecked;
        }
    }

    public void animateMinMax(long min, long max, boolean animate) {
        long prevMin = this.minValue ;
        long prevMax = this.maxValue;
        if (minValueAnim != 1f) {
            prevMin = (long) (prevMin * minValueAnim);
        }
        if (maxValueAnim != 1f) {
            prevMax = (long) (prevMax * maxValueAnim);
        }
        this.minValue = min;
        this.maxValue = max;
        if (alpha == 0f || !animate) {
            minValueAnim = 1.0f;
            maxValueAnim = 1.0f;
        } else {
            //todo mb double?
            minValueAnim = (float)prevMin / min;
            maxValueAnim = (float)prevMax / max;
//            minValue = min;
//            maxValue = max;
            minAnim = new MyAnimation.Float(MyAnimation.ANIM_DRATION, minValueAnim, 1.0f);
            maxAnim = new MyAnimation.Float(MyAnimation.ANIM_DRATION, maxValueAnim, 1.0f);
        }

    }
}
